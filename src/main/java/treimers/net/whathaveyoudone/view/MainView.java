package treimers.net.whathaveyoudone.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainView {
    private final BorderPane root;

    public MainView(MenuBar menuBar, ToolBar toolBar, Node content) {
        root = new BorderPane();
        root.setTop(new VBox(menuBar, toolBar));
        root.setCenter(content);
        BorderPane.setMargin(content, new Insets(10));
    }

    public Parent getRoot() {
        return root;
    }
}
