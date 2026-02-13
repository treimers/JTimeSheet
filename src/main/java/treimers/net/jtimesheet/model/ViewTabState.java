package treimers.net.jtimesheet.model;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * State for a view tab: fixed customer (and optionally project, task) plus
 * variable filters (selected tasks, date range) that the user can change in the tab.
 */
public class ViewTabState {
    private final ViewLevel level;
    private final Customer fixedCustomer;
    private final Project fixedProject;
    private final Task fixedTask;

    private final ObservableList<String> selectedTaskIds = FXCollections.observableArrayList();
    private LocalDate fromDate;
    private LocalDate toDate;
    private String presetKey = "CUSTOM";

    public ViewTabState(ViewLevel level, Customer fixedCustomer, Project fixedProject, Task fixedTask) {
        this.level = level;
        this.fixedCustomer = fixedCustomer;
        this.fixedProject = fixedProject;
        this.fixedTask = fixedTask;
    }

    public ViewLevel getLevel() {
        return level;
    }

    public Customer getFixedCustomer() {
        return fixedCustomer;
    }

    public Project getFixedProject() {
        return fixedProject;
    }

    public Task getFixedTask() {
        return fixedTask;
    }

    public ObservableList<String> getSelectedTaskIds() {
        return selectedTaskIds;
    }

    public void setSelectedTaskIds(List<String> ids) {
        selectedTaskIds.clear();
        if (ids != null) {
            selectedTaskIds.addAll(ids);
        }
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getPresetKey() {
        return presetKey;
    }

    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey != null ? presetKey : "CUSTOM";
    }

    /** Title for the tab (e.g. "Kunde A" or "Kunde A / Projekt B"). */
    public String getTabTitle() {
        if (fixedCustomer == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(fixedCustomer.getName());
        if (fixedProject != null) {
            sb.append(" / ").append(fixedProject.getName());
            if (fixedTask != null) {
                sb.append(" / ").append(fixedTask.getName());
            }
        }
        return sb.toString();
    }
}
