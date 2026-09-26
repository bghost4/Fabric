package com.derpderphurr.network.fabric.ui;
import javafx.beans.property.StringProperty;

public interface DeviceTreeData {
    String getDisplayName();
    default String getName() { return nameProperty().get(); }
    StringProperty nameProperty();
}
