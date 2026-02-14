package treimers.net.jtimesheet;

import javafx.application.Application;
import javafx.stage.Stage;
import treimers.net.jtimesheet.controller.MainController;

public class JTimeSheetApplication extends Application {
    public static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        new MainController().start(stage, getHostServices());
    }
}
