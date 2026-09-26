package com.derpderphurr.network.fabric;

import com.derpderphurr.network.fabric.ui.DeviceTreeData;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One LLDP neighbor. Immutable, so results can be cached or handed across threads.
 *
 * @param localPortNum        lldpRemLocalPortNum (NOT guaranteed to be an ifIndex)
 * @param localIfIndex        resolved ifIndex, or null if it could not be determined
 * @param localIfIndexGuessed true if resolution fell back to assuming portNum == ifIndex
 */
public record LLDPNeighbor(
        int localPortNum,
        Integer localIfIndex,
        boolean localIfIndexGuessed,
        String localPortName,
        String chassisIdSubtype,
        String chassisId,
        String portIdSubtype,
        String portId,
        String portDescription,
        String systemName,
        String systemDescription,
        Set<String> capabilitiesSupported,
        Set<String> capabilitiesEnabled,
        List<String> managementAddresses) implements DeviceTreeData {

    public LLDPNeighbor {
        // Keep bit order (Set.copyOf would not), but make them unmodifiable
        capabilitiesSupported = Collections.unmodifiableSet(new LinkedHashSet<>(capabilitiesSupported));
        capabilitiesEnabled = Collections.unmodifiableSet(new LinkedHashSet<>(capabilitiesEnabled));
        managementAddresses = List.copyOf(managementAddresses);
    }

    @Override
    public String getDisplayName() {
        return String.format("%s (%s,%s)",systemName,portId,portDescription);
    }

    @Override
    public StringProperty nameProperty() {
        return new SimpleStringProperty(getDisplayName());
    }


}