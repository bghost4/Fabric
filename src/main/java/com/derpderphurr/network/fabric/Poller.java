package com.derpderphurr.network.fabric;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Poller {
    protected Snmp snmp;
    protected final Map<Device,Map<Integer,Interface>> datamap = new HashMap<>();

    protected long pollInterval = 10000;
    protected long lastPoll = 0;

    protected Consumer<InterfaceData> dataConsumer = (d) -> {};

    public void start() throws IOException {
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        snmp.listen();

        Thread pollingThread = new Thread(() -> {
            while(true) {
                long current = System.currentTimeMillis();
                if( current - lastPoll  > pollInterval ) {
                    onPollInterval();
                    lastPoll = current;
                    //System.err.printf("Poll took %d Millis%n",System.currentTimeMillis()-current);
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    public void stop() throws IOException {
        snmp.close();
    }

    public void getInterfaces(Device d,Runnable r) {
        try {
            d.scanInterfaces(snmp);
            r.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getDeviceNeighbors(Device d) {
        try {
            d.fetchLLDPNeighbors(snmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getPortVlan(Device d) {
        System.out.printf("Fetch Port Vlan for %s%n",d);
        try {
            d.fetchPortVlan(snmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void onPollInterval() {
        for(Device d : datamap.keySet()) {
            List<OID> deviceOids = datamap.get(d).values().stream().flatMap(im -> im.buildOids().stream()).collect(Collectors.toList());
            fetchDeviceData(snmp,d,deviceOids).forEach(dataConsumer);
        }
    }

    protected Collection<InterfaceData> fetchDeviceData(Snmp snmp, Device d, List<OID> oids) {
        Map<Integer,InterfaceData> interfaceDataMap = new HashMap<>();
        PDU pdu = new PDU();
        pdu.addAll(oids.stream().map(VariableBinding::new).collect(Collectors.toList()));
        pdu.setType(PDU.GET);
        try {
            ResponseEvent<?> re = snmp.get(pdu,d.getTarget());
            if(re.getResponse() == null || re.getResponse().getVariableBindings() == null) {
                System.err.println("Respose was Null");
                return interfaceDataMap.values();
            }
            for(VariableBinding vb : re.getResponse().getVariableBindings() ) {
                int index = vb.getOid().last();
                interfaceDataMap.computeIfAbsent(index, index1 -> new InterfaceData(d, index1)).set(vb);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return interfaceDataMap.values();
    }

    public void addInterface(Interface i) {
        datamap.computeIfAbsent(i.getDevice(),k -> new HashMap<>()).put(i.getIndex(),i);
    }

    public void removeInterface(Interface myInterface) {
        datamap.get(myInterface.getDevice()).remove(myInterface.getIndex());
    }
}
