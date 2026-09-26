package com.derpderphurr.network.fabric;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.io.IOException;
import java.util.*;

/**
 * Stateless, thread-safe detection of trunk vs access ports and their VLANs using only
 * standard MIBs: BRIDGE-MIB (RFC 4188) and Q-BRIDGE-MIB (RFC 4363).
 *
 * A port is treated as a trunk if it is a TAGGED member of at least one VLAN
 * (egress member but not untagged). Otherwise it is an access port and its
 * access VLAN is its PVID (the VLAN untagged ingress frames are assigned to).
 *
 * Results are keyed by ifIndex so they can be joined with existing interface data.
 * Interfaces that are not bridge ports (routed ports, loopbacks, SVIs, LAG members
 * whose LAG is the bridge port) are simply absent from the result.
 *
 * Written against SNMP4J 3.x (generic Target). For 2.x, drop the generics.
 */
public final class PortVlanDiscovery {

    // ---- OID definitions ------------------------------------------------------------

    private static final int[] DOT1D_BRIDGE = {1, 3, 6, 1, 2, 1, 17};
    private static final int[] BASE_PORT_ENTRY      = concat(DOT1D_BRIDGE, 1, 4, 1);    // dot1dBasePortEntry
    private static final int[] VLAN_CURRENT_ENTRY   = concat(DOT1D_BRIDGE, 7, 1, 4, 2, 1); // dot1qVlanCurrentEntry
    private static final int[] VLAN_STATIC_ENTRY    = concat(DOT1D_BRIDGE, 7, 1, 4, 3, 1); // dot1qVlanStaticEntry
    private static final int[] PORT_VLAN_ENTRY      = concat(DOT1D_BRIDGE, 7, 1, 4, 5, 1); // dot1qPortVlanEntry

    // dot1dBasePortTable (index: dot1dBasePort)
    private static final OID BASE_PORT_IF_INDEX = new OID(BASE_PORT_ENTRY, 2);

    // dot1qPortVlanTable (index: dot1dBasePort)
    private static final OID PVID = new OID(PORT_VLAN_ENTRY, 1);

    // dot1qVlanCurrentTable (index: dot1qVlanTimeMark.dot1qVlanIndex) - operational membership
    private static final OID CURRENT_EGRESS   = new OID(VLAN_CURRENT_ENTRY, 4);
    private static final OID CURRENT_UNTAGGED = new OID(VLAN_CURRENT_ENTRY, 5);

    // dot1qVlanStaticTable (index: dot1qVlanIndex) - configured membership, used as fallback
    private static final OID STATIC_EGRESS    = new OID(VLAN_STATIC_ENTRY, 2);
    private static final OID STATIC_UNTAGGED  = new OID(VLAN_STATIC_ENTRY, 4);

    private static final OID[] CURRENT_COLUMNS = {CURRENT_EGRESS, CURRENT_UNTAGGED};
    private static final OID[] STATIC_COLUMNS  = {STATIC_EGRESS, STATIC_UNTAGGED};

    // ---- Result types ---------------------------------------------------------------



    public enum Source {
        /** Membership came from dot1qVlanCurrentTable (operational state). */
        CURRENT,
        /** Membership came from dot1qVlanStaticTable (configuration). */
        STATIC,
        /** No membership tables available; mode inferred from dot1qPvid alone. */
        PVID_ONLY,
        /** Nothing usable. */
        NONE
    }



    // ---- Configuration --------------------------------------------------------------

    private final int maxRowsPerPdu;

    public PortVlanDiscovery() {
        this(10);
    }

    /** @param maxRowsPerPdu rows requested per GETBULK; lower it for devices that choke on large responses */
    public PortVlanDiscovery(int maxRowsPerPdu) {
        if (maxRowsPerPdu < 1) throw new IllegalArgumentException("maxRowsPerPdu must be >= 1");
        this.maxRowsPerPdu = maxRowsPerPdu;
    }

    // ---- Public API -----------------------------------------------------------------

    /**
     * Determine trunk/access mode and VLANs for every bridge port on the target.
     *
     * @param snmp   an open (listening) Snmp session; not closed by this method
     * @param target the device to query
     * @return map of ifIndex -> PortVlan; empty if the device doesn't implement BRIDGE-MIB/Q-BRIDGE-MIB
     * @throws IOException on SNMP timeout or transport failure
     */
    public Map<Integer, PortVlan> discover(Snmp snmp, Target<?> target) throws IOException {
        Objects.requireNonNull(snmp, "snmp");
        Objects.requireNonNull(target, "target");
        Walker w = new Walker(snmp, target, maxRowsPerPdu);

        // 1. bridge port -> ifIndex
        Map<Integer, Integer> basePortToIfIndex = new TreeMap<>();
        for (TableEvent e : w.table(BASE_PORT_IF_INDEX)) {
            Integer ifIndex = intOf(e.getColumns()[0]);
            if (ifIndex != null && ifIndex > 0) basePortToIfIndex.put(e.getIndex().get(0), ifIndex);
        }

        // 2. PVID per bridge port
        Map<Integer, Integer> pvids = new HashMap<>();
        for (TableEvent e : w.table(PVID)) {
            Integer pvid = intOf(e.getColumns()[0]);
            if (pvid != null && pvid > 0) pvids.put(e.getIndex().get(0), pvid);
        }

        // 3. VLAN membership: prefer operational (current) table, fall back to static config
        Membership membership = new Membership();
        Source source = Source.CURRENT;
        loadMembership(w.table(CURRENT_COLUMNS), membership);
        if (membership.isEmpty()) {
            source = Source.STATIC;
            loadMembership(w.table(STATIC_COLUMNS), membership);
        }
        if (membership.isEmpty()) {
            source = pvids.isEmpty() ? Source.NONE : Source.PVID_ONLY;
        }

        // Some agents omit dot1dBasePortIfIndex; assume basePort == ifIndex in that case.
        if (basePortToIfIndex.isEmpty()) {
            Set<Integer> ports = new TreeSet<>(pvids.keySet());
            ports.addAll(membership.allPorts());
            for (int p : ports) basePortToIfIndex.put(p, p);
        }

        // 4. Classify each bridge port
        Map<Integer, PortVlan> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> bp : basePortToIfIndex.entrySet()) {
            int basePort = bp.getKey();
            int ifIndex = bp.getValue();
            Integer pvid = pvids.get(basePort);

            SortedSet<Integer> untagged = membership.untagged.getOrDefault(basePort, new TreeSet<>());
            SortedSet<Integer> egress = membership.egress.getOrDefault(basePort, new TreeSet<>());
            SortedSet<Integer> tagged = new TreeSet<>(egress);
            tagged.removeAll(untagged);

            result.put(ifIndex, classify(ifIndex, basePort, pvid, tagged, untagged, source));
        }
        return result;
    }

    // ---- Classification -------------------------------------------------------------

    private static PortVlan classify(int ifIndex, int basePort, Integer pvid,
                                     SortedSet<Integer> tagged, SortedSet<Integer> untagged, Source source) {
        if (!tagged.isEmpty()) {
            // Trunk. Native VLAN is the PVID if the port actually sends it untagged,
            // otherwise the single untagged VLAN if there is exactly one.
            Integer nativeVlan = null;
            if (pvid != null && untagged.contains(pvid)) nativeVlan = pvid;
            else if (untagged.size() == 1) nativeVlan = untagged.first();
            return new PortVlan(ifIndex, basePort, Interface.Mode.TRUNK, null, nativeVlan, pvid, tagged, untagged, source);
        }

        if (!untagged.isEmpty()) {
            Integer access;
            if (pvid != null && untagged.contains(pvid)) access = pvid;
            else if (untagged.size() == 1) access = untagged.first();
            else access = pvid; // several untagged VLANs (unusual); PVID decides ingress
            return new PortVlan(ifIndex, basePort, Interface.Mode.ACCESS, access, null, pvid, tagged, untagged, source);
        }

        if (pvid != null) {
            // No membership data for this port (or no membership tables at all):
            // a port with a PVID and no tagged VLANs is effectively an access port.
            Source s = source == Source.CURRENT || source == Source.STATIC ? source : Source.PVID_ONLY;
            return new PortVlan(ifIndex, basePort, Interface.Mode.ACCESS, pvid, null, pvid, tagged, untagged, s);
        }

        return new PortVlan(ifIndex, basePort, Interface.Mode.UNKNOWN, null, null, null, tagged, untagged, Source.NONE);
    }

    // ---- Membership parsing ---------------------------------------------------------

    /** Per-bridge-port VLAN sets, built by inverting the per-VLAN PortList bitmaps. */
    private static final class Membership {
        final Map<Integer, SortedSet<Integer>> egress = new HashMap<>();
        final Map<Integer, SortedSet<Integer>> untagged = new HashMap<>();

        boolean isEmpty() { return egress.isEmpty() && untagged.isEmpty(); }

        Set<Integer> allPorts() {
            Set<Integer> s = new HashSet<>(egress.keySet());
            s.addAll(untagged.keySet());
            return s;
        }
    }

    private static void loadMembership(List<TableEvent> rows, Membership m) {
        for (TableEvent e : rows) {
            OID idx = e.getIndex();
            if (idx.size() < 1) continue;
            // current table index = timeMark.vlanIndex, static table index = vlanIndex
            int vlan = idx.get(idx.size() - 1);
            VariableBinding[] c = e.getColumns();
            addPorts(m.egress, vlan, octets(c[0]));
            addPorts(m.untagged, vlan, octets(c[1]));
        }
    }

    /**
     * PortList (RFC 4363): each octet covers 8 ports, most significant bit first.
     * Bit 0x80 of the first octet is bridge port 1.
     */
    private static void addPorts(Map<Integer, SortedSet<Integer>> target, int vlan, OctetString portList) {
        if (portList == null) return;
        byte[] b = portList.getValue();
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v == 0) continue;
            for (int bit = 0; bit < 8; bit++) {
                if ((v & (0x80 >> bit)) != 0) {
                    int port = i * 8 + bit + 1;
                    target.computeIfAbsent(port, k -> new TreeSet<>()).add(vlan);
                }
            }
        }
    }

    // ---- SNMP helpers ---------------------------------------------------------------

    /** Wraps the SNMP walking for one session/target pair. Created per call; never shared. */
    private static final class Walker {
        private final Target<?> target;
        private final TableUtils tableUtils;

        Walker(Snmp snmp, Target<?> target, int maxRowsPerPdu) {
            this.target = target;
            int pduType = target.getVersion() == SnmpConstants.version1 ? PDU.GETNEXT : PDU.GETBULK;
            this.tableUtils = new TableUtils(snmp, new DefaultPDUFactory(pduType));
            this.tableUtils.setMaxNumRowsPerPDU(maxRowsPerPdu);
        }

        List<TableEvent> table(OID... columns) throws IOException {
            List<TableEvent> out = new ArrayList<>();
            for (TableEvent e : tableUtils.getTable(target, columns, null, null)) {
                if (e.isError()) {
                    if (e.getStatus() == TableEvent.STATUS_TIMEOUT) {
                        throw new IOException("SNMP timeout walking " + columns[0] + " on " + target.getAddress());
                    }
                    continue; // noSuchObject / not supported -> treat as empty
                }
                out.add(e);
            }
            return out;
        }
    }

    private static int[] concat(int[] prefix, int... suffix) {
        int[] r = Arrays.copyOf(prefix, prefix.length + suffix.length);
        System.arraycopy(suffix, 0, r, prefix.length, suffix.length);
        return r;
    }

    private static OctetString octets(VariableBinding vb) {
        return (vb != null && vb.getVariable() instanceof OctetString os) ? os : null;
    }

    private static Integer intOf(VariableBinding vb) {
        if (vb == null || vb.getVariable() instanceof Null) return null;
        try { return vb.getVariable().toInt(); } catch (UnsupportedOperationException ex) { return null; }
    }
}
