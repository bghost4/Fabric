package com.derpderphurr.network.fabric.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        MainWindow mw = new MainWindow();
        primaryStage.setScene(new Scene(mw));
        primaryStage.setTitle("Network Fabic Monitor");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
