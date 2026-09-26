package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.Device;
import com.derpderphurr.network.fabric.Interface;
import com.derpderphurr.network.fabric.InterfaceData;
import com.derpderphurr.network.fabric.Poller;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.Duration;
import java.time.ZonedDateTime;

public class InterfaceMonitor extends TitledPane {

    private final Interface myInterface;
    private final LineChart<String,Number> chart;
    private final XYChart.Series<String,Number> inBytes = new XYChart.Series<>();
    private final XYChart.Series<String,Number> outBytes = new XYChart.Series<>();
    private boolean catchFirst = true;

    private final StringProperty cBytesIn = new SimpleStringProperty(),cBytesOut = new SimpleStringProperty();

    ZonedDateTime lastDataPoint;

    long lastIn = 0,lastOut = 0;

    private final SimpleIntegerProperty windowSize = new SimpleIntegerProperty(30);

    public IntegerProperty windowSizeProperty() { return windowSize; }
    public int getWindowSize() { return windowSize.get(); }
    public void setWindowSize(int i) { windowSize.set(i); }

    private static final BinaryStringConverter cvt = new BinaryStringConverter();

    public Interface getInterface() {
        return myInterface;
    }

    public Device getDevice() { return myInterface.getDevice(); }

    private final Button btnClose = new Button("\uD83D\uDFAE");
    private final Button btnMoveUp = new Button("∆"),btnMoveDown = new Button("∇");

    public InterfaceMonitor(Interface i, Poller p) {
        super();

        myInterface = i;

        BorderPane bp = new BorderPane();

        HBox bbox = new HBox();
        bbox.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label();

        HBox.setHgrow(btnClose,Priority.NEVER);

        lblTitle.setMaxWidth(Double.MAX_VALUE);
        lblTitle.textProperty().bind(Bindings.format("%s: %s (%s in,%s out)",getDevice().nameProperty(),i.nameProperty(),cBytesIn,cBytesOut));

        bbox.getChildren().addAll(btnMoveUp,btnMoveDown,btnClose);

        bp.setLeft(lblTitle);
        bp.setRight(bbox);
        bp.setMaxWidth(Double.MAX_VALUE);
        bp.prefWidthProperty().bind(Bindings.subtract(widthProperty(),40));

        setGraphic(bp);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Bits/Second");
        yAxis.setTickLabelFormatter(cvt);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(false);

        inBytes.setName("IN");
        outBytes.setName("OUT");

        chart = new LineChart<>(xAxis,yAxis);
        chart.maxWidthProperty().bind(widthProperty().subtract(32));
        chart.getData().add(inBytes);
        chart.getData().add(outBytes);
        chart.setPrefHeight(250);
        chart.setAnimated(false);

        this.setContent(chart);

    }


    public void addData(InterfaceData d) {
        Platform.runLater(() -> {
            if(inBytes.getData().size() > windowSize.get()) {
                inBytes.getData().remove(0);
                outBytes.getData().remove(0);
            }

            long maxint = 4294967295L;

            ZonedDateTime now = ZonedDateTime.now();
            if(!catchFirst) {

                double diffIn = 0,diffOut = 0;
                if(d.getInOctets() < lastIn) {
                    diffIn = ((maxint - lastIn) + d.getInOctets())*8.0;
                } else {
                    diffIn = (d.getInOctets() - lastIn)*8.0;
                }

                if(d.getOutOctets() < lastOut) {
                    diffOut = ((maxint - lastOut) + d.getOutOctets()) * 8.0;
                } else {
                    diffOut = (d.getOutOctets() - lastOut )*8.0;
                }

                String category = String.format("%d:%02d",now.getMinute(),now.getSecond());
                double elapsedSeconds = ((double)(Duration.between(lastDataPoint,now)).toMillis()/1000.0);
                double bytesIn = Math.abs(diffIn / elapsedSeconds);
                double bytesOut = Math.abs((diffOut / elapsedSeconds));
                inBytes.getData().add(new XYChart.Data<>(category,bytesIn));
                outBytes.getData().add(new XYChart.Data<>(category, bytesOut));
                cBytesOut.set(cvt.toString(bytesOut));
                cBytesIn.set(cvt.toString(bytesIn));
            } else {
                catchFirst = false;
            }

            lastIn = d.getInOctets();
            lastOut = d.getOutOctets();
            lastDataPoint = now;
        });
    }

    public void setOnClose(EventHandler<ActionEvent> eh) {
        btnClose.setOnAction(eh);
    }
    public void setOnMoveUp(EventHandler<ActionEvent> eh) {
        btnMoveUp.setOnAction(eh);
    }
    public void setOnMoveDown(EventHandler<ActionEvent> eh) {
        btnMoveDown.setOnAction(eh);
    }

}
