package com.derpderphurr.network.fabric;

import com.derpderphurr.network.fabric.ui.DeviceTreeData;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Device implements DeviceTreeData {
    private final CommunityTarget<Address> t;
    private final String friendlyName;
    private final SimpleStringProperty sysName = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final List<Interface> allInterfaces = new ArrayList<>();
    private final List<Interface> monitoredInterfaces = new ArrayList<>();
    private final UUID deviceUUID = UUID.randomUUID();

    public Device(String address,String community,String name) {
        this(name, defaultTarget(address,community));
    }

    /** Restores a device from a previously configured target, e.g. when loading from a saved config. */
    public Device(String friendlyName, CommunityTarget<Address> target) {
        this.t = target;
        this.friendlyName = friendlyName;
        this.name.bind(Bindings.format("%s (%s)",sysName,friendlyName));
    }

    private static CommunityTarget<Address> defaultTarget(String address, String community) {
        CommunityTarget<Address> t = new CommunityTarget<>();
        t.setCommunity(new OctetString(community));
        t.setAddress(GenericAddress.parse(address));
        t.setRetries(2);
        t.setTimeout(1500);
        t.setVersion(SnmpConstants.version2c);
        return t;
    }

    public List<Interface> getInterfaces() { return allInterfaces; }

    @Override
    public String getDisplayName() {
        return name.get();
    }

    @Override
    public StringProperty nameProperty() { return name; }

    public List<OID> getMonitorOID() {
        return monitoredInterfaces.stream().flatMap(iface -> InterfaceData.buildOIDList(iface.getIndex()).stream()).collect(Collectors.toList());
    }

    public Optional<Interface> getInterface(int index) {
        return allInterfaces.stream().filter(iface -> iface.getIndex() == index).findFirst();
    }

    public void fetchLLDPNeighbors(Snmp snmp) throws IOException {
        LldpNeighborDiscovery discovery = new LldpNeighborDiscovery();
        List<LLDPNeighbor> neighbors =  discovery.discover(snmp,t);

        for(LLDPNeighbor neighbor: neighbors) {
            this.getInterface(neighbor.localIfIndex()).ifPresent(iface -> iface.getLLDPNeighbors().add(neighbor));
        }

    }

    /** interfaces must be filled in first **/
    public void fetchPortVlan(Snmp snmp) throws IOException {
        PortVlanDiscovery pvd = new PortVlanDiscovery();
        Map<Integer,PortVlan> vlan_ports = pvd.discover(snmp,t);
        for(Integer ifaceIndex : vlan_ports.keySet()) {
            System.out.println(vlan_ports.get(ifaceIndex));
            getInterface(ifaceIndex).ifPresent(i -> Platform.runLater(() -> i.setPortVlan(vlan_ports.get(ifaceIndex)) ));
        }
    }

    public void fetchEndpoints(Snmp snmp) {

        OID dot1qTpFdbTable =   new OID(".1.3.6.1.2.1.17.7.1.2.2");
        OID dot1qTpFdbAddress = new OID(".1.3.6.1.2.1.17.7.1.2.2.1.1");
        OID dot1qTpFdbPort =    new OID(".1.3.6.1.2.1.17.7.1.2.2.1.2");
        OID dot1qTpFdbStatus =  new OID(".1.3.6.1.2.1.17.7.1.2.2.1.3");

        //Expected Response
        //.1.3.6.1.2.1.17.7.1.2.2.1.2.3000.232.177.252.172.157.226 = INTEGER: 49
        //|       OID                |VLAN|     MAC               |  |  INTERFACE |
        TreeUtils tree = new TreeUtils(snmp,new DefaultPDUFactory());
        List<TreeEvent> events = tree.getSubtree(t, dot1qTpFdbTable);

        //HashMap<Interface,List<Endpoint>> data = new HashMap<>();

        if( events == null || events.isEmpty()) {
            System.err.println("Empty Events table");
           // return Collections.emptyList();
        } else {
            for(TreeEvent event : events) {
                if( event == null) {
                    System.err.printf("Event Was Null during getEndpoints (%s)%n",t.toString());
                    continue; }
                if( event.isError() ) {
                    System.err.printf("Event Was ERROR during getEndpoints (%s)%n",t.toString());
                    continue; }
                if( event.getVariableBindings() == null ) {
                    System.err.printf("Event Variable Bindings were Null during getEndpoints (%s)%n",t.toString());
                    continue;
                }
                if( event.getVariableBindings().length == 0) {
                    System.err.println("VariableBinding Length == 0 for "+event);
                    continue;
                }

                for(VariableBinding vb : event.getVariableBindings()) {
                    if(vb == null) { continue; }
                    if( vb.getOid().startsWith(dot1qTpFdbPort)) {
                        byte[] mac = extractMacFromOID(vb.getOid());

                        //14-20 == MAC Address
                        // value is port index
                        int interfaceIndex = vb.getVariable().toInt();
                        //getInterface(interfaceIndex).ifPresent(iface -> data.computeIfAbsent(iface,i -> new ArrayList<>()).add(new Endpoint(mac)));


                        getInterface(interfaceIndex).ifPresentOrElse(
                                iface -> {
                                    Endpoint ep = new Endpoint(mac,iface);
                                    System.out.println("Added "+ep.getName()+" to "+iface.getName());
                                    iface.getEndpoints().add(ep);
                                    },() -> System.err.printf("No Interface at Index (%s) for Device: \"%s\"",interfaceIndex,t.toString()));

                    }
                }
            }
        }
    }

    public static byte[] extractMacFromOID(OID oid) {
        int[] oidar = oid.toIntArray();
        byte[] mac = new byte[6];
        for(int i=oidar.length-1,j=5,a=0; a < 6; a++,i--,j-- ) {
            mac[j] = (byte)oidar[i];
        }
        return mac;
    }

    private static final OID SYS_NAME = new OID("1.3.6.1.2.1.1.5.0");

    public static <A extends Address> String getSysName(Snmp snmp, Target<A> target)
            throws IOException {
        // SNMPv3 targets require a ScopedPDU
        PDU pdu = (target.getVersion() == SnmpConstants.version3) ? new ScopedPDU() : new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(SYS_NAME));

        ResponseEvent<A> event = snmp.send(pdu, target);
        PDU response = event.getResponse();

        if (response == null) {
            throw new IOException("SNMP request to " + target.getAddress() + " timed out");
        }
        if (response.getErrorStatus() != PDU.noError) {
            throw new IOException("SNMP error from " + target.getAddress() + ": "
                    + response.getErrorStatusText());
        }

        VariableBinding vb = response.get(0);
        if (vb == null || vb.isException()) {
            return null; // noSuchObject / noSuchInstance / endOfMibView
        }

        Variable value = vb.getVariable();
        if (value instanceof OctetString) {
            // Avoid SNMP4J's hex formatting for non-printable bytes
            return new String(((OctetString) value).getValue(), StandardCharsets.UTF_8).trim();
        }
        return value.toString();
    }

    public void scanInterfaces(Snmp snmp) throws IOException {
        allInterfaces.clear();
        HashMap<Integer,Interface> interfaces = new HashMap<>();

        TreeUtils tree = new TreeUtils(snmp,new DefaultPDUFactory());
        List<TreeEvent> events = tree.getSubtree(t, InterfaceData.ifTable);

        if( events == null || events.size() == 0) {
            System.err.println("Empty Events table");
        } else {
            for(TreeEvent event : events) {
                if( event == null) {
                    System.err.println("Event Was Null");
                    continue; }
                if( event.isError() ) {
                    System.err.printf("Event Was ERROR during scanInterfaces: %s%n",event.getErrorMessage());
                    continue; }
                if( event.getVariableBindings() == null ) {
                    System.err.println("Event Variable Bindings were Null");
                    continue;
                }
                if( event.getVariableBindings().length == 0) {
                    System.err.println("VariableBinding Length == 0");
                    continue;
                }

                for(VariableBinding vb : event.getVariableBindings()) {
                    if(vb == null) { continue; }
                    interfaces.computeIfAbsent(vb.getOid().last(), index -> new Interface(index,this)).set(vb);
                }
            }
        }

        allInterfaces.addAll(interfaces.values());

        fetchEndpoints(snmp);
        fetchPortVlan(snmp);
        fetchLLDPNeighbors(snmp);

        String sysname = getSysName(snmp,t);
        Platform.runLater(() -> this.sysName.set(sysname));

        //return new ArrayList<>(interfaces.values());
    }

    public Map<Integer,InterfaceData> fetchData(Snmp snmp) {
        long methodBegin = System.currentTimeMillis();
        Map<Integer,InterfaceData> interfaceDataMap = new HashMap<>();
        PDU pdu = new PDU();
        pdu.addAll(getMonitorOID().stream().map(VariableBinding::new).collect(Collectors.toList()));
        pdu.setType(PDU.GET);
        try {
            ResponseEvent<Address> re = snmp.get(pdu,t);
            if(re.getResponse() == null || re.getResponse().getVariableBindings() == null) {
                System.err.println("Respose was Null");
            }
            for(VariableBinding vb : re.getResponse().getVariableBindings() ) {
                int index = vb.getOid().last();
                interfaceDataMap.computeIfAbsent(index, index1 -> new InterfaceData(this, index1)).set(vb);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("Fetch took %d millis%n",System.currentTimeMillis()-methodBegin);

        return interfaceDataMap;
    }

    public CommunityTarget<?> getTarget() { return t; }

    public String getFriendlyName() { return friendlyName; }

    public String getInterfaceName(Integer ifIndex) {
        return allInterfaces.stream().filter(iface -> iface.getIndex() == ifIndex).findFirst().map(Interface::getName).orElse("NONE");
    }
}
