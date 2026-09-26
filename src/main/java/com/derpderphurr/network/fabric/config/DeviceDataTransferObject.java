package com.derpderphurr.network.fabric.config;

import com.derpderphurr.network.fabric.Device;

public class DeviceDataTransferObject {
    private String friendlyName;
    private CommunityTargetTransferObject target;

    public DeviceDataTransferObject() {
    }

    public static DeviceDataTransferObject fromDevice(Device d) {
        DeviceDataTransferObject dto = new DeviceDataTransferObject();
        dto.friendlyName = d.getFriendlyName();
        dto.target = CommunityTargetTransferObject.fromTarget(d.getTarget());
        return dto;
    }

    public Device toDevice() {
        return new Device(friendlyName, target.toTarget());
    }

    public String getFriendlyName() { return friendlyName; }
    public void setFriendlyName(String friendlyName) { this.friendlyName = friendlyName; }

    public CommunityTargetTransferObject getTarget() { return target; }
    public void setTarget(CommunityTargetTransferObject target) { this.target = target; }
}
