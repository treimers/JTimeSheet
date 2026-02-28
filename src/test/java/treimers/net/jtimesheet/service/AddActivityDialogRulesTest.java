package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;
import treimers.net.jtimesheet.service.AddActivityDialogRules.BlockedReason;

/**
 * Tests for {@link AddActivityDialogRules}. See docs/Rules.md "Add-Activity-Ausnahmen".
 */
@DisplayName("AddActivityDialogRules")
class AddActivityDialogRulesTest {

    @Nested
    @DisplayName("getBlockedReason")
    class GetBlockedReason {

        @Test
        @DisplayName("null customers → NO_CUSTOMERS (rule 1)")
        void nullCustomers() {
            assertEquals(BlockedReason.NO_CUSTOMERS, AddActivityDialogRules.getBlockedReason(null));
        }

        @Test
        @DisplayName("empty customers → NO_CUSTOMERS (rule 1)")
        void emptyCustomers() {
            assertEquals(BlockedReason.NO_CUSTOMERS,
                AddActivityDialogRules.getBlockedReason(Collections.emptyList()));
        }

        @Test
        @DisplayName("customer with no projects → NO_PROJECTS (rule 2)")
        void noProjects() {
            Customer c = new Customer("id1", "Kunde");
            assertEquals(BlockedReason.NO_PROJECTS, AddActivityDialogRules.getBlockedReason(List.of(c)));
        }

        @Test
        @DisplayName("customer with project but no tasks → NO_TASKS (rule 3)")
        void noTasks() {
            Customer c = new Customer("id1", "Kunde");
            c.getProjects().add(new Project("p1", "Projekt"));
            assertEquals(BlockedReason.NO_TASKS, AddActivityDialogRules.getBlockedReason(List.of(c)));
        }

        @Test
        @DisplayName("customer with project and task → NONE (dialog can be shown)")
        void hasCustomerProjectTask() {
            Customer c = new Customer("id1", "Kunde");
            Project p = new Project("p1", "Projekt");
            p.getTasks().add(new Task("t1", "Task"));
            c.getProjects().add(p);
            assertEquals(BlockedReason.NONE, AddActivityDialogRules.getBlockedReason(List.of(c)));
        }

        @Test
        @DisplayName("second customer has project+task → NONE")
        void secondCustomerHasData() {
            Customer c1 = new Customer("id1", "Kunde1");
            Customer c2 = new Customer("id2", "Kunde2");
            Project p = new Project("p1", "Projekt");
            p.getTasks().add(new Task("t1", "Task"));
            c2.getProjects().add(p);
            assertEquals(BlockedReason.NONE, AddActivityDialogRules.getBlockedReason(List.of(c1, c2)));
        }
    }

    @Nested
    @DisplayName("BlockedReason message keys")
    class BlockedReasonMessageKeys {

        @Test
        @DisplayName("NONE has no message key")
        void noneHasNullKey() {
            assertEquals(null, BlockedReason.NONE.getMessageKey());
        }

        @Test
        @DisplayName("NO_CUSTOMERS has expected key")
        void noCustomersKey() {
            assertEquals("activity.add.blocked.no.customers", BlockedReason.NO_CUSTOMERS.getMessageKey());
        }

        @Test
        @DisplayName("NO_PROJECTS has expected key")
        void noProjectsKey() {
            assertEquals("activity.add.blocked.no.projects", BlockedReason.NO_PROJECTS.getMessageKey());
        }

        @Test
        @DisplayName("NO_TASKS has expected key")
        void noTasksKey() {
            assertEquals("activity.add.blocked.no.tasks", BlockedReason.NO_TASKS.getMessageKey());
        }
    }
}
