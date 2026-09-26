package com.derpderphurr.network.fabric;

import com.derpderphurr.network.fabric.ui.DeviceTreeData;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.DataFormat;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.util.List;

public class Interface implements DeviceTreeData {
    public static final DataFormat DATAFORMAT = new DataFormat("fabric.dnd.interface");
    private final int index;
    private long speed;
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty activeStatus = new SimpleBooleanProperty();
    private final ObservableList<Endpoint> myEndpoints = FXCollections.observableArrayList();
    private final ObservableList<LLDPNeighbor> myNeighbors = FXCollections.observableArrayList();

    private final SimpleObjectProperty<PortVlan> myPortVlan = new SimpleObjectProperty<>();

    public List<LLDPNeighbor> getLLDPNeighbors() { return myNeighbors; }

    public static enum Mode {
        /** Untagged member of a single VLAN, no tagged VLANs. */
        ACCESS,
        /** Tagged member of one or more VLANs (may also carry an untagged native VLAN). */
        TRUNK,
        /** Bridge port found, but the device exposed no usable PVID or membership data. */
        UNKNOWN
    }

    private final StringExpression displayName = Bindings.format("%s %s (%d)",name,myPortVlan,Bindings.size(myEndpoints));

    private final Device parent;

    private String ifaceMac;

    public Interface(int index, Device parent) {
        this.index = index;
        this.parent = parent;
    }

    public String getIFaceMac() {
        return ifaceMac;
    }
    public void setIFaceMac(String mac) {
        this.ifaceMac = mac;
    }

    public void setPortVlan(PortVlan pv) {
        this.myPortVlan.set(pv);
    }

    public PortVlan getPortVlan() { return myPortVlan.get(); }

    public ObservableList<Endpoint> getEndpoints() { return myEndpoints; }

    public int getIndex() {
        return index;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    @Override
    public StringProperty nameProperty() { return name; }

    @Override
    public String getDisplayName() {
        return displayName.get();
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Device getDevice() { return parent; }

    public void set(VariableBinding vb) {
        if( vb.getOid().startsWith(InterfaceData.ifDescr)) {
            setName(vb.getVariable().toString());
        } else if( vb.getOid().startsWith(InterfaceData.ifSpeed)) {
            setSpeed(vb.getVariable().toLong());
        } else if(vb.getOid().startsWith(InterfaceData.ifOperStatus)) {
            activeStatus.set(InterfaceData.PortStatus.UP == InterfaceData.PortStatus.get(vb.getVariable().toInt()));
        } else if(vb.getOid().startsWith(InterfaceData.ifPhysAddress)) {
            setIFaceMac(vb.getVariable().toString());
        }
        else { }
    }

    public void clearEndpoints() { myEndpoints.clear(); }

    public void setActiveStatus(boolean isup) { activeStatus.set(isup); }
    public boolean getActiveStatus() { return activeStatus.get(); }
    public BooleanProperty activeStatusProperty() { return activeStatus; }

    public List<OID> buildOids() {
        return InterfaceData.buildOIDList(getIndex());
    }

}
