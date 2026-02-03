package treimers.net.whathaveyoudone;

import javafx.application.Application;
import javafx.stage.Stage;
import treimers.net.whathaveyoudone.controller.MainController;

public class WHYDApplication  extends Application {
    public static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        new MainController().start(stage);
    }
}
