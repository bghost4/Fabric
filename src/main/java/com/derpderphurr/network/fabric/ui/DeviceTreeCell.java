package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.Interface;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class DeviceTreeCell extends TreeCell<DeviceTreeData> {

    private final Node portUp,portDown;

    public DeviceTreeCell() {
        Path pPortUp = new Path();
        Path pPortDown = new Path();

        pPortDown.getElements().addAll(
                new MoveTo(3,3),
                new LineTo(13,3),
                new LineTo(8,13),
                new LineTo(3,3)
        );

        pPortUp.setFill(Color.LIGHTGREEN);

        pPortUp.getElements().addAll(
                new MoveTo(3,13),
                new LineTo(13,13),
                new LineTo(8,3),
                new LineTo(3,13)
        );

        pPortDown.setFill(Color.RED);

        portUp = pPortUp;
        portDown = pPortDown;
    }

    @Override
    protected void updateItem(DeviceTreeData item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty && item != null) {
            //textProperty().bind(item.nameProperty());
            setText(item.getDisplayName());
            if(item instanceof Interface iface) {
                setTooltip(new Tooltip(iface.getIFaceMac()));
                if( iface.getActiveStatus()) {
                    //setText(iface.getDisplayName());
                    setGraphic(portUp);
                } else {
                    setGraphic(portDown);
                }
                iface.activeStatusProperty().addListener((ob,ov,nv) -> {
                    if( nv ) { setGraphic(portUp); }
                    else { setGraphic(portDown); }
                });
                //enableDnd(iface);
            } else { setGraphic(null);
                //disableDnd();
            }
        } else {
            //textProperty().unbind();
            setText(null);
            setGraphic(null);
            //disableDnd();
        }
    }

    private void disableDnd() {
        this.setOnDragDetected(mouseEvent -> {} );
    }

    private void enableDnd(Interface i) {
//        this.setOnDragDetected(mouseEvent -> {
//            Dragboard db = this.startDragAndDrop(TransferMode.LINK);
//            ClipboardContent content = new ClipboardContent();
//            content.put(Interface.DATAFORMAT,i);
//            db.setContent(content);
//            mouseEvent.consume();
//        });
    }

}
