package com.derpderphurr.network.fabric.config;

import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;

import java.nio.charset.StandardCharsets;

/** Jackson-friendly stand-in for {@link CommunityTarget}, which isn't itself serializable via databind. */
public class CommunityTargetTransferObject {
    private String address;
    private String community;
    private int version;
    private int retries;
    private long timeout;

    public CommunityTargetTransferObject() {
    }

    public static CommunityTargetTransferObject fromTarget(CommunityTarget<? extends Address> target) {
        CommunityTargetTransferObject dto = new CommunityTargetTransferObject();
        dto.address = target.getAddress().toString();
        dto.community = new String(target.getCommunity().getValue(), StandardCharsets.UTF_8);
        dto.version = target.getVersion();
        dto.retries = target.getRetries();
        dto.timeout = target.getTimeout();
        return dto;
    }

    public CommunityTarget<Address> toTarget() {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setAddress(GenericAddress.parse(address));
        target.setCommunity(new OctetString(community));
        target.setVersion(version);
        target.setRetries(retries);
        target.setTimeout(timeout);
        return target;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }

    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }
}
