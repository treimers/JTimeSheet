package treimers.net.jtimesheet.model;

import java.util.List;

/**
 * Predefined color palettes for calendar entries (per customer).
 * Each palette provides 7 colors; CalendarFX uses STYLE_1 … STYLE_7.
 */
public enum CalendarColorPalette {
    /** Standard calendar-like colors. */
    DEFAULT,

    /** Softer, pastel-like colors. */
    PASTEL,

    /** Strong, high-contrast colors. */
    HIGH_CONTRAST;

    /** Hex colors for indices 0–6 (style 1–7). */
    public List<String> getHexColors() {
        return switch (this) {
            case DEFAULT -> List.of(
                "#77C04B", "#5B9BD5", "#ED7D31", "#A5A5A5", "#FFC000", "#4472C4", "#70AD47"
            );
            case PASTEL -> List.of(
                "#B8D4E3", "#F5C6AA", "#C9E4B8", "#E8D4F0", "#FFF4B8", "#D4E8E8", "#F0D4D4"
            );
            case HIGH_CONTRAST -> List.of(
                "#2563EB", "#DC2626", "#059669", "#7C3AED", "#D97706", "#0891B2", "#BE185D"
            );
        };
    }

    /** Color at index 0–6. */
    public String getHexColor(int index) {
        List<String> colors = getHexColors();
        return colors.get(Math.max(0, Math.min(index, colors.size() - 1)));
    }

    public static CalendarColorPalette fromName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT;
        }
        for (CalendarColorPalette p : values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return DEFAULT;
    }
}
