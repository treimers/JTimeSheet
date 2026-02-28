package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Direct tests for {@link ReminderIntervalRules}. Ensures full coverage of docs/Rules.md
 * "Reminder-Intervall" (10 rules: due on boundary, not on boundary, before/after window,
 * no weekday, :00/:15/:30/:45, Sonntag-Override).
 */
@org.junit.jupiter.api.DisplayName("ReminderIntervalRules (Rules.md: Reminder-Intervall)")
class ReminderIntervalRulesTest {

    private static final String TEST_CASES_RESOURCE = "treimers/net/jtimesheet/service/isReminderDueTestCases.json";

    static Stream<Arguments> rulesMdReminderIntervallCases() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            String json = getResourceAsString(TEST_CASES_RESOURCE);
            root = mapper.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
            AppSettings settings = buildSettings(defaultSettings, c.has("settings") ? c.get("settings") : null);
            builder.add(Arguments.of(description, now, settings, expected));
        }
        return builder.build();
    }

    private static String getResourceAsString(String resource) throws IOException {
        try (var in = ReminderIntervalRulesTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static AppSettings buildSettings(JsonNode defaults, JsonNode overlay) {
        AppSettings settings = new AppSettings();
        JsonNode s = defaults != null ? defaults : overlay;
        if (s != null && s.has("windowStart") && s.has("windowEnd")) {
            settings.setReminderWindow(
                LocalTime.parse(s.get("windowStart").asText()),
                LocalTime.parse(s.get("windowEnd").asText()));
        }
        if (s != null && s.has("intervalMinutes")) {
            settings.setReminderIntervalMinutes(s.get("intervalMinutes").asInt());
        }
        if (s != null && s.has("weekdays") && s.get("weekdays").isArray()) {
            Set<DayOfWeek> days = new java.util.HashSet<>();
            for (JsonNode d : s.get("weekdays")) {
                days.add(DayOfWeek.valueOf(d.asText()));
            }
            settings.setReminderWeekdays(java.util.EnumSet.copyOf(days));
        }
        if (overlay != null && overlay != defaults) {
            if (overlay.has("windowStart") && overlay.has("windowEnd")) {
                settings.setReminderWindow(
                    LocalTime.parse(overlay.get("windowStart").asText()),
                    LocalTime.parse(overlay.get("windowEnd").asText()));
            }
            if (overlay.has("intervalMinutes")) {
                settings.setReminderIntervalMinutes(overlay.get("intervalMinutes").asInt());
            }
            if (overlay.has("weekdays") && overlay.get("weekdays").isArray()) {
                Set<DayOfWeek> days = new java.util.HashSet<>();
                for (JsonNode d : overlay.get("weekdays")) {
                    days.add(DayOfWeek.valueOf(d.asText()));
                }
                settings.setReminderWeekdays(java.util.EnumSet.copyOf(days));
            }
        }
        return settings;
    }

    @ParameterizedTest(name = "Rules.md Reminder-Intervall: {0}")
    @MethodSource("rulesMdReminderIntervallCases")
    void isReminderDueGemäßRulesMd(String description, LocalDateTime now, AppSettings settings, boolean expected) {
        assertEquals(expected, ReminderIntervalRules.isReminderDue(now, settings), description);
    }
}
