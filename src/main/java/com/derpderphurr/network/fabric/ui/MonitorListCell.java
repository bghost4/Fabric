package com.derpderphurr.network.fabric.ui;

import javafx.scene.control.ListCell;

public class MonitorListCell extends ListCell<InterfaceMonitor> {

    @Override
    protected void updateItem(InterfaceMonitor item, boolean empty) {
        super.updateItem(item, empty);

        if(item != null && !empty) {
            setGraphic(item);
            setText(null);
        } else { setText(null); setGraphic(null); }
    }
}
