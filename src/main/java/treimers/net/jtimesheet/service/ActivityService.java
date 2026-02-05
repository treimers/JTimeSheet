package treimers.net.jtimesheet.service;

import java.time.LocalDateTime;

import treimers.net.jtimesheet.model.Activity;

public class ActivityService {
    public boolean canMergeWithLast(Activity lastActivity, Activity activity) {
        if (lastActivity == null) {
            return false;
        }
        if (!lastActivity.getCustomerId().equals(activity.getCustomerId())) {
            return false;
        }
        if (!lastActivity.getProjectId().equals(activity.getProjectId())) {
            return false;
        }
        if (!lastActivity.getTaskId().equals(activity.getTaskId())) {
            return false;
        }
        LocalDateTime lastTo = Activity.parseStoredDateTime(lastActivity.getTo());
        LocalDateTime newFrom = Activity.parseStoredDateTime(activity.getFrom());
        return lastTo != null && lastTo.equals(newFrom);
    }
}
