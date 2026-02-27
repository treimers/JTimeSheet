package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Tests for reminder suggestion logic (what to suggest as customer/project/task and time range).
 * <p>
 * Dokumentation der Reminder-Szenarien:
 * <ol>
 *   <li>Beim Programmstart kommt kein Reminder. (Implementierung in ReminderService, kein Test hier.)</li>
 *   <li>Außerhalb der Reminder-Intervalle (Start, Ende, Wochentag) → kein Reminder.</li>
 *   <li>Keine Aktivität heute (über alle Kunden) → Reminder mit letzter Kundenaktivität als Default, Zeit now−1h bis now.</li>
 *   <li>Keine vergangenen Kundenaktivitäten → erster Kunde, erstes Projekt, erste Task, Zeit now−1h bis now.</li>
 *   <li>Ende der letzten Kundenaktivität heute in der Vergangenheit → Reminder mit dieser Aktivität, Zeit Ende bis jetzt.</li>
 *   <li>Ende der letzten Kundenaktivität in der Zukunft → kein Reminder.</li>
 *   <li>Ende in der Zukunft + Anwender wählt Add Activity → letzte Lücke als Vorschlag.</li>
 *   <li>Im Reminder-Dialog Kunden wechseln → letzte Aktivität dieses Kunden (auch vorherige Tage) oder erstes Projekt/Task.</li>
 *   <li>Dialog mit end=now offen lassen, Intervall verstreicht → Endzeit wird weitergesetzt; bei Lücke nicht (SuggestionType).</li>
 * </ol>
 * <p>
 * Die Testfälle werden aus {@code reminderSuggestionLogicTestCases.json} (compute) bzw.
 * {@code isWithinWindowTestCases.json} (isNowWithinReminderWindow) geladen.
 */
@DisplayName("ReminderSuggestionLogic")
class ReminderSuggestionLogicTest {

    private static final String TEST_CASES_RESOURCE = "treimers/net/jtimesheet/service/reminderSuggestionLogicTestCases.json";
    private static final String IS_WITHIN_WINDOW_TEST_CASES_RESOURCE = "treimers/net/jtimesheet/service/isWithinWindowTestCases.json";

    private ReminderSuggestionLogic logic;

    @BeforeEach
    void setUp() {
        logic = new ReminderSuggestionLogic();
    }

    static Stream<Arguments> isWithinWindowTestCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = getResourceAsString(IS_WITHIN_WINDOW_TEST_CASES_RESOURCE);
        JsonNode root = mapper.readTree(json);
        AppSettings defaultSettings = buildAppSettings(root.get("defaultSettings"));
        JsonNode cases = root.get("cases");
        if (cases == null || !cases.isArray()) {
            return Stream.empty();
        }
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < cases.size(); i++) {
            JsonNode c = cases.get(i);
            String description = c.has("description") ? c.get("description").asText() : "";
            LocalDateTime now = LocalDateTime.parse(c.get("now").asText());
            boolean expected = c.get("expected").asBoolean();
            builder.add(Arguments.of(description, now, defaultSettings, expected));
        }
        return builder.build();
    }

    static Stream<Arguments> computeTestCases() throws IOException {
        JsonNode root = loadRoot();
        JsonNode defaultSettingsNode = root.get("defaultSettings");
        AppSettings defaultSettings = buildAppSettings(defaultSettingsNode);
        JsonNode cases = root.get("computeCases");
        if (cases == null || !cases.isArray()) {
            return Stream.empty();
        }
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < cases.size(); i++) {
            JsonNode c = cases.get(i);
            String description = c.has("description") ? c.get("description").asText() : "";
            JsonNode setup = c.get("setup");
            var pair = buildCustomersAndActivities(setup);
            List<Customer> customers = pair.customers;
            List<Activity> activities = pair.activities;
            JsonNode input = c.get("input");
            LocalDateTime now = LocalDateTime.parse(input.get("now").asText());
            Integer lastActivityIndex = input.has("lastActivityIndex") && !input.get("lastActivityIndex").isNull()
                ? input.get("lastActivityIndex").asInt() : null;
            Activity lastActivity = (lastActivityIndex != null && lastActivityIndex >= 0 && lastActivityIndex < activities.size())
                ? activities.get(lastActivityIndex) : null;
            String contextCustomerId = input.has("contextCustomerId") && !input.get("contextCustomerId").isNull()
                ? input.get("contextCustomerId").asText() : null;
            String contextProjectId = input.has("contextProjectId") && !input.get("contextProjectId").isNull()
                ? input.get("contextProjectId").asText() : null;
            boolean fromReminder = input.get("fromReminder").asBoolean();
            JsonNode expected = c.get("expected");
            builder.add(Arguments.of(description, now, customers, activities, lastActivity,
                contextCustomerId, contextProjectId, defaultSettings, fromReminder, expected));
        }
        return builder.build();
    }

    private static JsonNode loadRoot() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = getResourceAsString(TEST_CASES_RESOURCE);
        return mapper.readTree(json);
    }

    private static String getResourceAsString(String resource) throws IOException {
        try (var in = ReminderSuggestionLogicTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static AppSettings buildAppSettings(JsonNode s) {
        if (s == null) {
            return new AppSettings();
        }
        AppSettings settings = new AppSettings();
        if (s.has("windowStart") && s.has("windowEnd")) {
            settings.setReminderWindow(
                LocalTime.parse(s.get("windowStart").asText()),
                LocalTime.parse(s.get("windowEnd").asText()));
        }
        if (s.has("timeGridMinutes")) {
            settings.setTimeGridMinutes(s.get("timeGridMinutes").asInt());
        }
        if (s.has("weekdays") && s.get("weekdays").isArray()) {
            Set<DayOfWeek> days = new java.util.HashSet<>();
            for (JsonNode d : s.get("weekdays")) {
                days.add(DayOfWeek.valueOf(d.asText()));
            }
            settings.setReminderWeekdays(EnumSet.copyOf(days));
        }
        return settings;
    }

    private static record CustomersAndActivities(List<Customer> customers, List<Activity> activities) {}

    private static CustomersAndActivities buildCustomersAndActivities(JsonNode setup) {
        List<Customer> customers = new ArrayList<>();
        List<Activity> activities = new ArrayList<>();
        if (setup == null) {
            return new CustomersAndActivities(customers, activities);
        }
        JsonNode customersNode = setup.get("customers");
        if (customersNode != null && customersNode.isArray()) {
            for (JsonNode cust : customersNode) {
                String id = cust.get("id").asText();
                String name = cust.has("name") ? cust.get("name").asText() : id;
                Customer customer = new Customer(id, name);
                JsonNode projectsNode = cust.get("projects");
                if (projectsNode != null && projectsNode.isArray()) {
                    for (JsonNode proj : projectsNode) {
                        String pid = proj.get("id").asText();
                        String pname = proj.has("name") ? proj.get("name").asText() : pid;
                        Project project = new Project(pid, pname);
                        JsonNode tasksNode = proj.get("tasks");
                        if (tasksNode != null && tasksNode.isArray()) {
                            for (JsonNode t : tasksNode) {
                                String tid = t.get("id").asText();
                                String tname = t.has("name") ? t.get("name").asText() : tid;
                                project.getTasks().add(new Task(tid, tname));
                            }
                        }
                        customer.getProjects().add(project);
                    }
                }
                customers.add(customer);
            }
        }
        JsonNode activitiesNode = setup.get("activities");
        if (activitiesNode != null && activitiesNode.isArray()) {
            for (JsonNode a : activitiesNode) {
                String customerId = a.get("customerId").asText();
                String projectId = a.get("projectId").asText();
                String taskId = a.get("taskId").asText();
                LocalDateTime from = LocalDateTime.parse(a.get("from").asText());
                LocalDateTime to = LocalDateTime.parse(a.get("to").asText());
                activities.add(new Activity(customerId, projectId, taskId,
                    Activity.formatDateTime(from), Activity.formatDateTime(to)));
            }
        }
        return new CustomersAndActivities(customers, activities);
    }

    @Nested
    @DisplayName("Szenario 2: Zeit außerhalb des Reminder-Fensters (aus JSON)")
    class IsWithinWindowFromJson {

        @ParameterizedTest(name = "{0}")
        @MethodSource("treimers.net.jtimesheet.service.ReminderSuggestionLogicTest#isWithinWindowTestCases")
        void isWithinWindow(String description, LocalDateTime now, AppSettings settings, boolean expected) {
            assertEquals(expected, logic.isNowWithinReminderWindow(now, settings), description);
        }
    }

    @Nested
    @DisplayName("compute(…) – Szenarien 3–9 (aus JSON)")
    class ComputeFromJson {

        @ParameterizedTest(name = "{0}")
        @MethodSource("treimers.net.jtimesheet.service.ReminderSuggestionLogicTest#computeTestCases")
        void compute(
            String description,
            LocalDateTime now,
            List<Customer> customers,
            List<Activity> activities,
            Activity lastActivity,
            String contextCustomerId,
            String contextProjectId,
            AppSettings settings,
            boolean fromReminder,
            JsonNode expected) {
        ReminderSuggestion s = logic.compute(now, activities, customers, lastActivity,
            contextCustomerId, contextProjectId, settings, fromReminder);

        if (expected.has("blockedForReminder")) {
            assertEquals(expected.get("blockedForReminder").asBoolean(), s.isBlockedForReminder(), description + " (blockedForReminder)");
        }
        if (expected.has("suggestionType")) {
            assertEquals(ReminderSuggestion.SuggestionType.valueOf(expected.get("suggestionType").asText()),
                s.getSuggestionType(), description + " (suggestionType)");
        }
        if (expected.has("customerId")) {
            if (expected.get("customerId").isNull()) {
                assertNull(s.getCustomerId(), description + " (customerId)");
            } else {
                assertEquals(expected.get("customerId").asText(), s.getCustomerId(), description + " (customerId)");
            }
        }
        if (expected.has("projectId")) {
            if (expected.get("projectId").isNull()) {
                assertNull(s.getProjectId(), description + " (projectId)");
            } else {
                assertEquals(expected.get("projectId").asText(), s.getProjectId(), description + " (projectId)");
            }
        }
        if (expected.has("taskId")) {
            if (expected.get("taskId").isNull()) {
                assertNull(s.getTaskId(), description + " (taskId)");
            } else {
                assertEquals(expected.get("taskId").asText(), s.getTaskId(), description + " (taskId)");
            }
        }
        LocalDateTime[] range = s.getRange();
        if (expected.has("range")) {
            assertNotNull(range, description + " (range)");
            JsonNode rangeArr = expected.get("range");
            assertEquals(LocalDateTime.parse(rangeArr.get(0).asText()), range[0], description + " (range[0])");
            assertEquals(LocalDateTime.parse(rangeArr.get(1).asText()), range[1], description + " (range[1])");
        }
        if (expected.has("rangeEndIsNow") && expected.get("rangeEndIsNow").asBoolean()) {
            assertNotNull(range, description + " (range)");
            assertTrue(range[1].equals(now) || range[1].toLocalTime().equals(now.toLocalTime()),
                description + " (range[1] should be now)");
        }
        if (expected.has("rangeStartBeforeNow") && expected.get("rangeStartBeforeNow").asBoolean()) {
            assertNotNull(range, description + " (range)");
            assertTrue(range[0].isBefore(now), description + " (range[0] should be before now)");
        }
        if (expected.has("rangeStartEqualsEnd") && expected.get("rangeStartEqualsEnd").asBoolean()) {
            assertNotNull(range, description + " (range)");
            assertEquals(range[0], range[1], description + " (range start should equal end)");
        }
        if (expected.has("rangeNotNull") && expected.get("rangeNotNull").asBoolean()) {
            assertNotNull(range, description + " (range)");
        }
    }
    }

    // --- ReminderSuggestion: Auflösung von IDs zu Customer/Project/Task (für Dialog) ---

    @Nested
    @DisplayName("ReminderSuggestion: resolve Customer/Project/Task")
    class ReminderSuggestionResolve {

        private static final LocalDateTime NOW_MONDAY_10 = LocalDateTime.of(2025, 2, 3, 10, 0);
        private List<Customer> customers;

        @BeforeEach
        void setUpCustomers() {
            customers = new ArrayList<>();
        }

        private Customer addCustomer(String name) {
            Customer c = new Customer(name);
            customers.add(c);
            return c;
        }

        private Project addProject(Customer customer, String name) {
            Project p = new Project(name);
            customer.getProjects().add(p);
            return p;
        }

        private Task addTask(Project project, String name) {
            Task t = new Task(name);
            project.getTasks().add(t);
            return t;
        }

        @Test
        @DisplayName("resolveCustomer finds customer by id")
        void resolveCustomer() {
            Customer c = addCustomer("X");
            ReminderSuggestion s = ReminderSuggestion.suggest(
                    NOW_MONDAY_10.minusHours(1), NOW_MONDAY_10,
                    c.getId(), null, null, ReminderSuggestion.SuggestionType.DEFAULT_RANGE);
            assertEquals(c, s.resolveCustomer(customers));
        }

        @Test
        @DisplayName("resolveProject and resolveTask find project and task")
        void resolveProjectAndTask() {
            Customer c = addCustomer("C");
            Project p = addProject(c, "P");
            Task t = addTask(p, "T");
            ReminderSuggestion s = ReminderSuggestion.suggest(
                    NOW_MONDAY_10.minusHours(1), NOW_MONDAY_10,
                    c.getId(), p.getId(), t.getId(), ReminderSuggestion.SuggestionType.DEFAULT_RANGE);
            assertEquals(p, s.resolveProject(c));
            assertEquals(t, s.resolveTask(p));
        }

        @Test
        @DisplayName("suggestNowOnly: isBlockedForReminder, range [now, now] (Dauer 0)")
        void suggestNowOnly() {
            ReminderSuggestion s = ReminderSuggestion.suggestNowOnly(
                    NOW_MONDAY_10, "cid", "pid", "tid");
            assertTrue(s.isBlockedForReminder());
            LocalDateTime[] range = s.getRange();
            assertNotNull(range);
            assertEquals(NOW_MONDAY_10, range[0]);
            assertEquals(NOW_MONDAY_10, range[1]);
        }
    }
}
