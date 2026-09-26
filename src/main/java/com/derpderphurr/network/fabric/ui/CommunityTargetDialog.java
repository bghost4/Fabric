package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.Device;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;

public class CommunityTargetDialog extends Dialog<Device> {

    private final TextField txtFriendlyName = new TextField();
    private final TextField txtAddress = new TextField();
    private final TextField txtCommnuity = new TextField();
    private final Spinner<Integer> spnPort = new Spinner<>();
    private final Spinner<Integer> spnTimeout = new Spinner<>();
    private final Spinner<Integer> spnRetries = new Spinner<>();

    public CommunityTargetDialog() {

        SpinnerValueFactory.IntegerSpinnerValueFactory portValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,65535,161);
        SpinnerValueFactory.IntegerSpinnerValueFactory timeoutValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,65535,1500);
        SpinnerValueFactory.IntegerSpinnerValueFactory retryValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,65535,2);

        spnPort.setValueFactory(portValueFactory);
        spnRetries.setValueFactory(retryValueFactory);
        spnTimeout.setValueFactory(timeoutValueFactory);

        spnPort.setEditable(true);
        spnTimeout.setEditable(true);
        spnRetries.setEditable(true);

        GridPane gp = new GridPane();
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);

        gp.add(new Label("Friendly Name:"),0,1);
        gp.add(txtFriendlyName,1,1);

        gp.add(new Label("Hostname/IP:"),0,2);
        gp.add(txtAddress,1,2);

        gp.add(new Label("Port:"),0,3);
        gp.add(spnPort,1,3);

        gp.add(new Label("Community:"),0,4);
        gp.add(txtCommnuity,1,4);

        gp.add(new Label("Version: (2c)"),0,5);

        gp.add(new Label("Timeout:"),0,6);
        gp.add(spnTimeout,1,6);

        gp.add(new Label("Retries:"),0,7);
        gp.add(spnRetries,1,7);

        getDialogPane().setContent(gp);

        this.setResultConverter(btn -> {
            if(btn == ButtonType.OK) {
                CommunityTarget<Address> t = new CommunityTarget<>();
                t.setCommunity(new OctetString(txtCommnuity.getText()));
                String addressString = String.format("udp:%s/%d",txtAddress.getText(),spnPort.getValue());
                t.setAddress(GenericAddress.parse(addressString));
                t.setRetries(spnRetries.getValue());
                t.setTimeout(spnTimeout.getValue());
                t.setVersion(SnmpConstants.version2c);

                System.out.println("Device Address String: "+addressString);
                if(t.getAddress() == null) {
                    throw new RuntimeException("Target Address was null");
                }

                return new Device(txtFriendlyName.getText(),t);
            } else { return null; }
        });

    }

}
