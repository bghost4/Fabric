package com.derpderphurr.network.fabric.ui;

import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import com.derpderphurr.network.fabric.config.Config;
import javafx.scene.layout.VBox;

public class VisualNetworkMap extends VBox {

    private final Graph<String, String> graph;
    private final SmartGraphPanel<String, String> graphView;

    public VisualNetworkMap() {
        graph = new GraphEdgeList<>();

        SmartPlacementStrategy initialPlacement = new SmartCircularSortedPlacementStrategy();
        graphView = new SmartGraphPanel<>(graph, initialPlacement);

    }

    public void init() {
        graphView.init();
    }

    public void renderConfig(Config conf) {
        conf.getDevices().forEach(d -> graph.insertVertex(d.getSysName()));


    }

}
