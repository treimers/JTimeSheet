package treimers.net.whathaveyoudone;

import javafx.application.Application;
import javafx.stage.Stage;
import treimers.net.whathaveyoudone.controller.MainController;

public class WhatHaveYouDone extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        new MainController().start(stage);
    }
}
