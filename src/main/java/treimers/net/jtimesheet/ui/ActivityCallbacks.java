package treimers.net.jtimesheet.ui;

public interface ActivityCallbacks {
    int countActivitiesForCustomer(String customerId);
    int countActivitiesForProject(String customerId, String projectId);
    int countActivitiesForTask(String customerId, String projectId, String taskId);
    void removeActivitiesForCustomer(String customerId);
    void removeActivitiesForProject(String customerId, String projectId);
    void removeActivitiesForTask(String customerId, String projectId, String taskId);
}
