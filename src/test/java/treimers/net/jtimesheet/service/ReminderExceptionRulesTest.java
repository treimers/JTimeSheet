package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Tests for {@link ReminderExceptionRules}. See docs/Rules.md "Reminder-Ausnahmen".
 */
@DisplayName("ReminderExceptionRules")
class ReminderExceptionRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 2, 3, 10, 0);

    @Nested
    @DisplayName("hasActivityEndingNowOrInFuture")
    class HasActivityEndingNowOrInFuture {

        @Test
        @DisplayName("null activities → false")
        void nullActivities() {
            assertFalse(ReminderExceptionRules.hasActivityEndingNowOrInFuture(null, NOW));
        }

        @Test
        @DisplayName("empty activities → false")
        void emptyActivities() {
            assertFalse(ReminderExceptionRules.hasActivityEndingNowOrInFuture(Collections.emptyList(), NOW));
        }

        @Test
        @DisplayName("activity ending in past → false")
        void activityEndingInPast() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 08:00", "2025-02-03 09:00"));
            assertFalse(ReminderExceptionRules.hasActivityEndingNowOrInFuture(activities, NOW));
        }

        @Test
        @DisplayName("activity ending now → true (rule 2)")
        void activityEndingNow() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 09:00", "2025-02-03 10:00"));
            assertTrue(ReminderExceptionRules.hasActivityEndingNowOrInFuture(activities, NOW));
        }

        @Test
        @DisplayName("activity ending in future → true (rule 2)")
        void activityEndingInFuture() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 09:00", "2025-02-03 11:00"));
            assertTrue(ReminderExceptionRules.hasActivityEndingNowOrInFuture(activities, NOW));
        }
    }

    @Nested
    @DisplayName("hasNoCustomerProjectTask")
    class HasNoCustomerProjectTask {

        @Test
        @DisplayName("null customers → true (rule 3)")
        void nullCustomers() {
            assertTrue(ReminderExceptionRules.hasNoCustomerProjectTask(null));
        }

        @Test
        @DisplayName("empty customers → true (rule 3)")
        void emptyCustomers() {
            assertTrue(ReminderExceptionRules.hasNoCustomerProjectTask(Collections.emptyList()));
        }

        @Test
        @DisplayName("customer with no projects → true")
        void customerNoProjects() {
            Customer c = new Customer("id1", "Kunde");
            assertTrue(ReminderExceptionRules.hasNoCustomerProjectTask(List.of(c)));
        }

        @Test
        @DisplayName("customer with project but no tasks → true")
        void projectNoTasks() {
            Customer c = new Customer("id1", "Kunde");
            c.getProjects().add(new Project("p1", "Projekt"));
            assertTrue(ReminderExceptionRules.hasNoCustomerProjectTask(List.of(c)));
        }

        @Test
        @DisplayName("customer with project and task → false")
        void hasCustomerProjectTask() {
            Customer c = new Customer("id1", "Kunde");
            Project p = new Project("p1", "Projekt");
            p.getTasks().add(new Task("t1", "Task"));
            c.getProjects().add(p);
            assertFalse(ReminderExceptionRules.hasNoCustomerProjectTask(List.of(c)));
        }
    }

    @Nested
    @DisplayName("shouldSuppressReminder")
    class ShouldSuppressReminder {

        @Test
        @DisplayName("no activities, no customers → suppress (rule 3)")
        void noDataSuppress() {
            assertTrue(ReminderExceptionRules.shouldSuppressReminder(
                Collections.emptyList(), null, NOW));
        }

        @Test
        @DisplayName("activity ending in future → suppress (rule 2)")
        void activityInFutureSuppress() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 09:00", "2025-02-03 11:00"));
            Customer c = new Customer("id1", "Kunde");
            Project p = new Project("p1", "Projekt");
            p.getTasks().add(new Task("t1", "Task"));
            c.getProjects().add(p);
            assertTrue(ReminderExceptionRules.shouldSuppressReminder(
                activities, List.of(c), NOW));
        }

        @Test
        @DisplayName("only past activities, has customer/project/task → do not suppress")
        void pastActivitiesWithData() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 08:00", "2025-02-03 09:00"));
            Customer c = new Customer("c1", "Kunde");
            Project p = new Project("p1", "Projekt");
            p.getTasks().add(new Task("t1", "Task"));
            c.getProjects().add(p);
            assertFalse(ReminderExceptionRules.shouldSuppressReminder(
                activities, List.of(c), NOW));
        }

        @Test
        @DisplayName("no customer/project/task → suppress (rule 3)")
        void noCustomerProjectTaskSuppress() {
            List<Activity> activities = List.of(
                activity("c1", "p1", "t1", "2025-02-03 08:00", "2025-02-03 09:00"));
            Customer c = new Customer("id1", "Kunde"); // no projects
            assertTrue(ReminderExceptionRules.shouldSuppressReminder(
                activities, List.of(c), NOW));
        }
    }

    private static Activity activity(String customerId, String projectId, String taskId, String from, String to) {
        return new Activity(customerId, projectId, taskId, from, to);
    }
}
