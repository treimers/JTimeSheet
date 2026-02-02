package treimers.net.whathaveyoudone.ui;

import javafx.scene.control.TreeCell;

class ProjectTreeCell extends TreeCell<NodeData> {
    private final ManagementDialog dialog;

    ProjectTreeCell(ManagementDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    protected void updateItem(NodeData data, boolean empty) {
        super.updateItem(data, empty);
        if (empty || data == null) {
            setText(null);
            setContextMenu(dialog.emptyMenu());
            return;
        }
        setText(data.displayName());
        setContextMenu(dialog.contextMenuFor(data));
    }
}
