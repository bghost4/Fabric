package com.derpderphurr.network.fabric;

import com.derpderphurr.network.fabric.ui.DeviceTreeData;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Endpoint implements DeviceTreeData {
    private final byte[] macAddress;
    private final StringProperty name = new SimpleStringProperty();
    private final Interface iface;

    public Endpoint(byte[] macAddress,Interface iface) {
        this.macAddress = macAddress;
        name.set(formatMacAddress(macAddress));
        this.iface = iface;
    }

    public static String formatMacAddress(byte[] bytes) {
        if(bytes.length != 6) { return "INVALID"; }
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x",bytes[0],bytes[1],bytes[2],bytes[3],bytes[4],bytes[5]);
    }

    @Override
    public String getDisplayName() {
        return name.get();
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }

    public Interface getInterface() {
        return iface;
    }
}
