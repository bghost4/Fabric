package com.derpderphurr.network.fabric;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InterfaceData {
    private final Device device;
    private final int index;
    private String name;
    private long inOctets,outOctets;
    private long inErrors,outErrors;
    private long lastChange;
    private final long captureTimestamp;
    private PortStatus operStatus,adminStatus;

    public enum PortStatus {
        UP(1),DOWN(2),TESTING(3),UNKNOWN(4),DORMANT(5),NOT_PRESENT(6),LOWER_LAYER_DOWN(7);

        private final int innerStatus;
        private static final Map<Integer,PortStatus> reverse;

        static {
            reverse = new HashMap<>();
            Arrays.stream(PortStatus.values()).forEach(ps -> reverse.put(ps.getValue(),ps));
        }

        private int getValue() { return innerStatus; }

        PortStatus(int i) {
            innerStatus = i;
        }

        public static PortStatus get(int i) {
            return reverse.get(i);
        }

    }

    public Device getDevice() { return device; }

    public long getInErrors() {
        return inErrors;
    }

    public void setInErrors(long inErrors) {
        this.inErrors = inErrors;
    }

    public long getOutErrors() {
        return outErrors;
    }

    public void setOutErrors(long outErrors) {
        this.outErrors = outErrors;
    }

    public long getLastChange() {
        return lastChange;
    }

    public void setLastChange(long lastChange) {
        this.lastChange = lastChange;
    }

    public final static OID ifTable = new OID(".1.3.6.1.2.1.2.2");
    public final static OID ifDescr = new OID(".1.3.6.1.2.1.2.2.1.2");
    public final static OID ifType = new OID(".1.3.6.1.2.1.2.2.1.3");
    public final static OID ifMTU = new OID(".1.3.6.1.2.1.2.2.1.4");
    public final static OID ifSpeed = new OID(".1.3.6.1.2.1.2.2.1.5");
    public final static OID ifPhysAddress = new OID(".1.3.6.1.2.1.2.2.1.6");
    public final static OID ifAdminStatus = new OID(".1.3.6.1.2.1.2.2.1.7");
    public final static OID ifOperStatus = new OID(".1.3.6.1.2.1.2.2.1.8");
    public final static OID ifLastChange = new OID(".1.3.6.1.2.1.2.2.1.9");
    public final static OID ifInOctets = new OID(".1.3.6.1.2.1.2.2.1.10");
    public final static OID ifOutOctets = new OID(".1.3.6.1.2.1.2.2.1.16");
    public final static OID ifInErrors = new OID(".1.3.6.1.2.1.2.2.1.14");
    public final static OID ifOutErrors = new OID(".1.3.6.1.2.1.2.2.1.20");

    public static List<OID> buildOIDList(int interfaceIndex) {
        return Stream.of(ifAdminStatus,ifOperStatus,ifLastChange,ifInOctets,ifOutOctets,ifInErrors,ifOutErrors).map(oid -> new OID(oid).append(interfaceIndex)).collect(Collectors.toList());
    }

    public PortStatus getOperStatus() {
        return operStatus;
    }

    public void setOperStatus(PortStatus operStatus) {
        this.operStatus = operStatus;
    }

    public PortStatus getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(PortStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    public void set(VariableBinding vb) {
        if(vb.getOid().last() != index) {
            System.err.println("This data is not for me: "+vb.getOid().toString()+" "+index);
            return; }
        if(vb.getOid().startsWith(ifInOctets)) {
            inOctets = vb.getVariable().toLong();
        } else if( vb.getOid().startsWith(ifOutOctets)) {
            outOctets = vb.getVariable().toLong();
        } else if( vb.getOid().startsWith(ifAdminStatus)) {
            setAdminStatus(PortStatus.get(vb.getVariable().toInt()));
        } else if( vb.getOid().startsWith(ifOperStatus)) {
            setOperStatus(PortStatus.get(vb.getVariable().toInt()));
            device.getInterface(index).ifPresent(iface -> iface.setActiveStatus(getOperStatus() == PortStatus.UP));
        } else if (vb.getOid().startsWith(ifLastChange)) {
            setLastChange(vb.getVariable().toLong());
        } else if( vb.getOid().startsWith(ifInErrors)) {
            setInErrors(vb.getVariable().toLong());
        } else if( vb.getOid().startsWith(ifOutErrors)) {
            setOutErrors(vb.getVariable().toLong());
        }
        else {
            System.err.println("Data Not Handled By Set: "+vb.getOid().toString());
        }
    }

    public InterfaceData(Device device, int index) {
        this.device = device;
        this.index = index;
        captureTimestamp = System.currentTimeMillis();
    }

    public long getCaptureTimestamp() { return captureTimestamp; }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getInOctets() {
        return inOctets;
    }

    public void setInOctets(long inOctets) {
        this.inOctets = inOctets;
    }

    public long getOutOctets() {
        return outOctets;
    }

    public void setOutOctets(long outOctets) {
        this.outOctets = outOctets;
    }



}
