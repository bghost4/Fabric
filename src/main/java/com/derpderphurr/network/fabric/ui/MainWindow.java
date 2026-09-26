package com.derpderphurr.network.fabric.ui;

import com.derpderphurr.network.fabric.Device;
import com.derpderphurr.network.fabric.Endpoint;
import com.derpderphurr.network.fabric.Interface;
import com.derpderphurr.network.fabric.LLDPNeighbor;
import com.derpderphurr.network.fabric.config.Config;
import com.derpderphurr.network.fabric.datastore.InMemoryDataStore;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class MainWindow extends VBox {

        @FXML
        private TreeView<DeviceTreeData> tvDevices;

        @FXML
        private ListView<InterfaceMonitor> lstMonitor;

        @FXML
        private SplitPane splitPane;

        @FXML
        private TextField txtFilterMac;

        private final SimpleBooleanProperty hideDownInterfaces = new SimpleBooleanProperty(false);
        private final SimpleBooleanProperty hidNonTrunkInerface = new SimpleBooleanProperty(false);

        private final InMemoryDataStore dataStore = new InMemoryDataStore();
        private UIPoller poller;
        private final TreeItem<DeviceTreeData> root = new TreeItem<>(null);

        private final SimpleObjectProperty<Config> config = new SimpleObjectProperty<>(new Config());
        private Path configPath;

        @FXML
        private void onLoadConfig(ActionEvent e) {
            FileChooser fc = new FileChooser();
            File fconfg = fc.showOpenDialog(null);
            if(fconfg != null) {
                try {
                    Config nconfig = Config.load(fconfg.toPath(),"");
                    root.getChildren().clear();
                    this.config.set(nconfig);
                    configPath = fconfg.toPath();
                    this.config.get().getDevices().forEach(this::addDeviceToTree);
                } catch (IOException ex) {
                    //Err Dialog, with info about why ot broke
                }
            }
        }

        @FXML private void onSaveConfig(ActionEvent e) {
            if(configPath == null) {
                FileChooser fc = new FileChooser();
                File fconfig = fc.showSaveDialog(null);
                if(fconfig != null) {
                    configPath = fconfig.toPath();
                }
            }

            try {
                this.config.get().save(configPath,"");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }

        private TreeItem<DeviceTreeData> filterTree(String name) {
            if(name == null || name.isBlank()) { return root; }
            TreeItem<DeviceTreeData> newRoot = new TreeItem<>(new DummyTreeData("ROOT"));

            List<DeviceTreeData> matches = flatten(root).filter(dtd -> dtd.getName().toLowerCase().replace(":","").contains(name.toLowerCase().replace(":","").replace("-",""))).toList();

            for(DeviceTreeData match : matches) {
                if(match instanceof Endpoint end) {

                    TreeItem<DeviceTreeData> ep = new TreeItem<>(end);

                    TreeItem<DeviceTreeData> device = newRoot.getChildren().stream().filter(dtd -> end.getInterface().getDevice() == dtd.getValue()).findFirst().orElse(new TreeItem<>(end.getInterface().getDevice()));
                    if(!newRoot.getChildren().contains(device)) {
                        newRoot.getChildren().add(device);
                    }
                    TreeItem<DeviceTreeData> iface = device.getChildren().stream().filter(dtd -> end.getInterface() == dtd.getValue()).findFirst().orElse(new TreeItem<>(end.getInterface()));
                    if(!device.getChildren().contains(iface)) {
                        device.getChildren().add(iface);
                    }
                    if(!iface.getChildren().contains(ep)) {
                        iface.getChildren().add(ep);
                    }
                }
            }
            return newRoot;
        }

        public Stream<DeviceTreeData> flatten(TreeItem<DeviceTreeData> item) {
            Stream<DeviceTreeData> thisItem = Stream.of(item.getValue());
            Stream<DeviceTreeData> other = item.getChildren().stream().flatMap(this::flatten);
            if(item.getValue() != null) {
                return Stream.concat(thisItem, other);
            } else {
                return other;
            }
        }

        public MainWindow() {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainwindow.fxml"));
            loader.setRoot(this);
            loader.setController(this);

            try {
                loader.load();
            } catch (IOException exception) {
                throw new RuntimeException("Failed to Load FXML",exception);
            }
        }

        @FXML
        private void onSearchMac(ActionEvent e) {

        }

        @FXML
        private void onQuit(ActionEvent e) {}

        @FXML
        void initialize() {

            assert tvDevices != null : "fx:id=\"tvDevices\" was not injected: check your FXML file 'mainwindow.fxml'.";
            assert lstMonitor != null : "fx:id=\"lstMonitors\" was not injected: check your FXML file 'mainwindow.fxml'.";

            poller = new UIPoller();
            try {
                poller.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            tvDevices.setRoot(root);

            tvDevices.setCellFactory((t) -> new DeviceTreeCell());

            tvDevices.setOnMouseClicked(mouseEvent -> {
                if( (mouseEvent.getClickCount() == 2) && (mouseEvent.getButton() == MouseButton.PRIMARY) ) {
                    DeviceTreeData dtd = tvDevices.getSelectionModel().getSelectedItem().getValue();

                    if(dtd instanceof Interface iface) {
                        addMonitorInterface(iface);
                    }
                }
            });

            tvDevices.setShowRoot(false);

            lstMonitor.setCellFactory((f) -> new MonitorListCell());

            lstMonitor.setOnDragOver(dragEvent -> {
                if(dragEvent.getDragboard().getContentTypes().contains(Interface.DATAFORMAT)) {
                    dragEvent.acceptTransferModes(TransferMode.LINK);
                }
                dragEvent.consume();
            });

            lstMonitor.widthProperty().addListener((ob,ov,nv) -> System.out.println("List width: "+nv));

            lstMonitor.setOnDragDropped(dragEvent -> {
                Object objInterface = dragEvent.getDragboard().getContent(Interface.DATAFORMAT);
                if(objInterface instanceof Interface iface) {
                    addMonitorInterface(iface);
                }
            });

            ContextMenu ctx = new ContextMenu();
            MenuItem miRename = new MenuItem("Rename");
            miRename.setOnAction(eh -> {
                TreeItem<DeviceTreeData> item = tvDevices.getSelectionModel().getSelectedItem();
                if(item != null) {
                    TextInputDialog tid = new TextInputDialog(item.getValue().toString());
                    tid.setTitle(String.format("Rename %s",item.getValue().toString()));
                    tid.showAndWait().ifPresent(newname -> item.getValue().nameProperty().set(newname));
                    tvDevices.fireEvent(new TreeItem.TreeModificationEvent<>(TreeItem.valueChangedEvent(),item,item.getValue()));
                }
            });

            MenuItem miScan = new MenuItem("Scan");
            miScan.setOnAction(eh -> {
                TreeItem<DeviceTreeData> item = tvDevices.getSelectionModel().getSelectedItem();

                scanDevice(item);

                item.getChildren().sort(Comparator.comparing(ti -> {
                    if(ti.getValue() instanceof Interface iface) {
                        return iface.getIndex();
                    } else {
                        return -1;
                    }
                }));
            });

            MenuItem miAddGraph = new MenuItem("Add Graph");
            miAddGraph.setOnAction(ae -> {
                TreeItem<DeviceTreeData> item = tvDevices.getSelectionModel().getSelectedItem();
                if(item != null) {
                    DeviceTreeData dtd = tvDevices.getSelectionModel().getSelectedItem().getValue();
                    if(dtd instanceof Interface iface) {
                        addMonitorInterface(iface);
                    }
                }
            });

            ctx.getItems().addAll(miAddGraph,miRename,miScan);
            tvDevices.setContextMenu(ctx);

            txtFilterMac.textProperty().addListener((ob,ov,nv) -> {
                TreeItem<DeviceTreeData> nr = filterTree(nv);
                tvDevices.setRoot(nr);
            });

        }

        @FXML
        private void onAddDevice(ActionEvent e) {
            CommunityTargetDialog dialog = new CommunityTargetDialog();
            Optional<Device> oDevice = dialog.showAndWait();

            oDevice.ifPresent(device -> {
                System.out.println("Device: "+device.getTarget());
                this.config.get().addDevice(device);
                addDeviceToTree(device);
            });
        }

        private void createDeviceTreeNode(TreeItem<DeviceTreeData> dti, Device device) {
            Platform.runLater(() -> {
                for(Interface iface : device.getInterfaces()) {
                    TreeItem<DeviceTreeData> iti = new TreeItem<>(iface);

                    if(iface.getPortVlan() != null && iface.getPortVlan().isTrunk()) {
                        if(iface.getLLDPNeighbors().isEmpty()) {
                            for(Endpoint ep : iface.getEndpoints()) {
                                TreeItem<DeviceTreeData> eti = new TreeItem<>(ep);
                                iti.getChildren().add(eti);
                            }
                        } else {
                            for(LLDPNeighbor neighbor : iface.getLLDPNeighbors()) {
                                TreeItem<DeviceTreeData> nti = new TreeItem<>(neighbor);
                                iti.getChildren().add(nti);
                                if(neighbor.capabilitiesEnabled().contains("wlanAccessPoint")) {
                                    //NOTE: this may be wrong if there are multiple neighbors connected to this port
                                    for(Endpoint ep : iface.getEndpoints()) {
                                        TreeItem<DeviceTreeData> eti = new TreeItem<>(ep);
                                        nti.getChildren().add(eti);
                                    }
                                }
                            }
                        }
                    } else {
                        //if there is more than one entry here, there is a good chance there is a Hub
                        for(Endpoint ep : iface.getEndpoints()) {
                            TreeItem<DeviceTreeData> eti = new TreeItem<>(ep);
                            iti.getChildren().add(eti);
                        }
                    }
                    dti.getChildren().add(iti);
                }
            });
        }

        private void scanDevice(TreeItem<DeviceTreeData> item) {
            if(item.getValue() instanceof Device device && item.getChildren().isEmpty() ) {
                poller.getInterfaces(device,() -> createDeviceTreeNode(item,device));
            }
        }

        @FXML
        private void onScanAllDevices(ActionEvent e) {
            //Scan all Device Nodes

            root.getChildren().stream().filter(dtn -> dtn.getValue() instanceof Device).forEach(this::scanDevice);

        }

        public void addMonitorInterface(Interface iface) {
            poller.addInterface(iface);
            InterfaceMonitor monitor = poller.getMonitor(iface);
            lstMonitor.getItems().add(monitor);

            monitor.setOnClose((eh) -> { lstMonitor.getItems().remove(monitor); poller.removeInterface(monitor.getInterface()); });
            monitor.setOnMoveUp((eh) -> {
                int idx = lstMonitor.getItems().indexOf(monitor);
                if(idx != 0) {
                    Collections.swap(lstMonitor.getItems(),idx,idx-1);
                }
            });
            monitor.setOnMoveDown((eh) -> {
                int idx = lstMonitor.getItems().indexOf(monitor);

                Collections.swap(lstMonitor.getItems(),idx,idx+1);
            });
        }

        public void addDeviceToTree(Device d) {
            TreeItem<DeviceTreeData> item = new TreeItem<>(d);
            root.getChildren().add(item);
        }
        @FXML
        public void onDumpEndpoints(ActionEvent ex) {

            FileChooser fc = new FileChooser();
            File output = fc.showSaveDialog(null);

            record CsvEndpoint(String device, String strInterface, String mac) { }

            List<CsvEndpoint> csvEndpoints = root.getChildren().stream().flatMap(ti -> {
                if(ti.getValue() instanceof Device device) {
                    return device.getInterfaces().stream().flatMap(iface -> iface.getEndpoints().stream());
                } else { return Stream.empty(); }
            }).map( endpoint -> new CsvEndpoint(endpoint.getInterface().getDevice().getDisplayName(),endpoint.getInterface().getDisplayName(),endpoint.getDisplayName()) ).toList();

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(output)) ) {
                for(CsvEndpoint ep : csvEndpoints) {
                    bw.write(String.format("\"%s\",\"%s\",\"%s\"",ep.device,ep.strInterface,ep.mac));
                    bw.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }

}
