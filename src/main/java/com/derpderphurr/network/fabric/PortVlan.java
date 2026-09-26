package com.derpderphurr.network.fabric;

import java.util.Collections;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * VLAN information for one switched interface. Immutable.
 *
 * @param ifIndex       the interface's ifIndex
 * @param bridgePort    dot1dBasePort number (differs from ifIndex on many devices)
 * @param mode          ACCESS, TRUNK or UNKNOWN
 * @param accessVlan    the access VLAN when mode == ACCESS, otherwise null
 * @param nativeVlan    the untagged/native VLAN when mode == TRUNK (null if none), otherwise null
 * @param pvid          raw dot1qPvid value, or null if not reported
 * @param taggedVlans   VLANs the port sends tagged
 * @param untaggedVlans VLANs the port sends untagged
 * @param source        where the membership data came from
 */
public record PortVlan(
        int ifIndex,
        int bridgePort,
        Interface.Mode mode,
        Integer accessVlan,
        Integer nativeVlan,
        Integer pvid,
        SortedSet<Integer> taggedVlans,
        SortedSet<Integer> untaggedVlans,
        PortVlanDiscovery.Source source) {

    public PortVlan {
        taggedVlans = Collections.unmodifiableSortedSet(new TreeSet<>(taggedVlans));
        untaggedVlans = Collections.unmodifiableSortedSet(new TreeSet<>(untaggedVlans));
    }

    public boolean isTrunk()  { return mode == Interface.Mode.TRUNK; }
    public boolean isAccess() { return mode == Interface.Mode.ACCESS; }

    @Override
    public String toString() {
//        StringBuilder sb = new StringBuilder("PortVlan[ifIndex=").append(ifIndex)
//                .append(", bridgePort=").append(bridgePort)
//                .append(", mode=").append(mode);
        StringBuilder sb = new StringBuilder("Mode: ").append(mode);

        switch (mode) {
            case ACCESS -> sb.append(", accessVlan=").append(accessVlan);
            case TRUNK -> sb.append(", nativeVlan=").append(nativeVlan == null ? "none" : nativeVlan)
                    .append(", tagged=").append(ranges(taggedVlans));
            case UNKNOWN -> { }
        }

        // Only show extra untagged VLANs or a PVID that disagrees with the reported VLAN
        if (untaggedVlans.size() > 1) {
            sb.append(", untagged=").append(ranges(untaggedVlans));
        }
        if (pvid != null && !pvid.equals(accessVlan) && !pvid.equals(nativeVlan)) {
            sb.append(", pvid=").append(pvid);
        }

        //return sb.append(", source=").append(source).append(']').toString();
        return sb.toString();
    }

    /** Collapses a sorted VLAN set into ranges: [1-10,20,100-200]. */
    private static String ranges(SortedSet<Integer> vlans) {
        StringJoiner out = new StringJoiner(",", "[", "]");
        int start = -1, prev = -1;
        for (int v : vlans) {
            if (start < 0) {
                start = prev = v;
            } else if (v == prev + 1) {
                prev = v;
            } else {
                out.add(start == prev ? String.valueOf(start) : start + "-" + prev);
                start = prev = v;
            }
        }
        if (start >= 0) out.add(start == prev ? String.valueOf(start) : start + "-" + prev);
        return out.toString();
    }

}