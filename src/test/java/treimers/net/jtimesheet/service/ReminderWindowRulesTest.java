package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Direct tests for {@link ReminderWindowRules}. Ensures full coverage of docs/Rules.md
 * "Reminder-Zeitfenster" (4 rules: vor Fenster, nach Fenster, kein Wochentag, im Fenster).
 */
@org.junit.jupiter.api.DisplayName("ReminderWindowRules (Rules.md: Reminder-Zeitfenster)")
class ReminderWindowRulesTest {

    private static final String TEST_CASES_RESOURCE = "treimers/net/jtimesheet/service/isWithinWindowTestCases.json";

    static Stream<Arguments> rulesMdReminderFensterCases() throws IOException {
        String json = getResourceAsString(TEST_CASES_RESOURCE);
        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode defaultSettings = root.get("defaultSettings");
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
            AppSettings settings = buildSettings(defaultSettings);
            builder.add(Arguments.of(description, now, settings, expected));
        }
        return builder.build();
    }

    private static String getResourceAsString(String resource) throws IOException {
        try (var in = ReminderWindowRulesTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static AppSettings buildSettings(JsonNode s) {
        AppSettings settings = new AppSettings();
        if (s != null && s.has("windowStart") && s.has("windowEnd")) {
            settings.setReminderWindow(
                LocalTime.parse(s.get("windowStart").asText()),
                LocalTime.parse(s.get("windowEnd").asText()));
        }
        if (s != null && s.has("weekdays") && s.get("weekdays").isArray()) {
            Set<DayOfWeek> days = new java.util.HashSet<>();
            for (JsonNode d : s.get("weekdays")) {
                days.add(DayOfWeek.valueOf(d.asText()));
            }
            settings.setReminderWeekdays(EnumSet.copyOf(days));
        }
        return settings;
    }

    @ParameterizedTest(name = "Rules.md Reminder-Zeitfenster: {0}")
    @MethodSource("rulesMdReminderFensterCases")
    void isWithinWindowGemäßRulesMd(String description, LocalDateTime now, AppSettings settings, boolean expected) {
        assertEquals(expected, ReminderWindowRules.isWithinWindow(now, settings), description);
    }
}
