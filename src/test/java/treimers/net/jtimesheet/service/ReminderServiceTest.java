package treimers.net.jtimesheet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
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

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Tests for reminder timing: when is a reminder due (interval boundary, window, weekday).
 * <p>
 * Dokumentation der Reminder-Szenarien (Timing):
 * <ul>
 *   <li><b>Szenario 1:</b> Beim Programmstart kommt kein Reminder. (Timer ruft Callback nicht sofort auf.)</li>
 *   <li><b>Szenario 2:</b> Außerhalb der Reminder-Intervalle (Start, Ende, Wochentag) → isReminderDue false.</li>
 * </ul>
 * <p>
 * Die Testfälle für {@link ReminderService#isReminderDue} werden aus
 * {@code isReminderDueTestCases.json} geladen.
 */
@DisplayName("ReminderService")
class ReminderServiceTest {

    private static final String TEST_CASES_RESOURCE = "treimers/net/jtimesheet/service/isReminderDueTestCases.json";

    private ReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReminderService();
    }

    static Stream<Arguments> isReminderDueTestCases() {
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
        try (var in = ReminderServiceTest.class.getClassLoader().getResourceAsStream(resource)) {
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
            settings.setReminderWeekdays(EnumSet.copyOf(days));
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
                settings.setReminderWeekdays(EnumSet.copyOf(days));
            }
        }
        return settings;
    }

    @Nested
    @DisplayName("Szenario 2: isReminderDue – wann kommt ein Reminder (Intervall, Fenster, Wochentag)")
    class IsReminderDue {

        @ParameterizedTest(name = "{0}")
        @MethodSource("treimers.net.jtimesheet.service.ReminderServiceTest#isReminderDueTestCases")
        @DisplayName("isReminderDue(now, settings) gemäß JSON-Spezifikation")
        void fromJson(String description, LocalDateTime now, AppSettings settings, boolean expected) {
            assertEquals(expected, service.isReminderDue(now, settings), description);
        }
    }

    @Nested
    @DisplayName("tick(now) – Testbarkeit (Parameter 'jetzt')")
    class Tick {

        @Test
        @DisplayName("tick(now) wirft nicht, wenn nicht gestartet")
        void tickSafeWhenNotStarted() {
            service.tick(LocalDateTime.of(2025, 2, 3, 16, 15));
        }
    }
}
