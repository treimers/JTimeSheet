package treimers.net.whathaveyoudone.model;

public enum Language {
    ENGLISH("en"),
    GERMAN("de");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Language fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ENGLISH;
        }
        if ("GERMAN".equalsIgnoreCase(code) || code.toLowerCase().startsWith("de")) {
            return GERMAN;
        }
        if ("ENGLISH".equalsIgnoreCase(code) || code.toLowerCase().startsWith("en")) {
            return ENGLISH;
        }
        return ENGLISH;
    }
}
