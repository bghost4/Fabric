package com.derpderphurr.network.fabric.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DummyTreeData implements DeviceTreeData {
    SimpleStringProperty name = new SimpleStringProperty();

    public DummyTreeData(String name) {
        this.name.set(name);
    }

    @Override
    public String getDisplayName() {
        return name.get();
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }
}
