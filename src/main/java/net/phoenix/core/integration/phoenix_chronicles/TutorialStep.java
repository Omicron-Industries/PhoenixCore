package net.phoenix.core.integration.phoenix_chronicles;

public record TutorialStep(String text, String highlight) {

    public static final String HL_NONE = "none";
    public static final String HL_SIDEBAR = "sidebar";
    public static final String HL_CANVAS = "canvas";
    public static final String HL_TOOLBAR = "toolbar";

    public TutorialStep(String text) {
        this(text, HL_NONE);
    }

    public boolean hasHighlight() {
        return highlight != null && !highlight.isBlank() && !highlight.equals(HL_NONE);
    }

    public boolean isNodeHighlight() {
        return highlight != null && highlight.startsWith("node:");
    }

    public String nodeHighlightId() {
        return isNodeHighlight() ? highlight.substring(5) : null;
    }
}
