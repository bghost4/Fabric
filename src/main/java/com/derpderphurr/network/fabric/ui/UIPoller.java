package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.Interface;
import com.derpderphurr.network.fabric.Poller;

import java.util.HashMap;
import java.util.Map;

public class UIPoller extends Poller {

    private final Map<Interface,InterfaceMonitor> monitorMap = new HashMap<>();

    public UIPoller() {
        this.dataConsumer = (interfaceData -> {
            interfaceData.getDevice().getInterface(interfaceData.getIndex()).ifPresent(iface -> monitorMap.get(iface).addData(interfaceData));
        });
    }

    @Override
    public void addInterface(Interface i) {
        super.addInterface(i);
        InterfaceMonitor im = new InterfaceMonitor(i,this);
        monitorMap.put(i,im);
    }

    public InterfaceMonitor getMonitor(Interface i) {
        return monitorMap.get(i);
    }

}
