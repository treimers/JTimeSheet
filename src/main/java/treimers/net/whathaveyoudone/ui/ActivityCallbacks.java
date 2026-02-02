package treimers.net.whathaveyoudone.ui;

public interface ActivityCallbacks {
    int countActivitiesForCustomer(String customerName);
    int countActivitiesForProject(String customerName, String projectName);
    int countActivitiesForTask(String customerName, String projectName, String taskName);
    void updateActivitiesForCustomerRename(String oldName, String newName);
    void updateActivitiesForProjectRename(String customerName, String oldName, String newName);
    void updateActivitiesForTaskRename(String customerName, String projectName, String oldName, String newName);
    void removeActivitiesForCustomer(String customerName);
    void removeActivitiesForProject(String customerName, String projectName);
    void removeActivitiesForTask(String customerName, String projectName, String taskName);
}
