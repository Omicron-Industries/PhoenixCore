package net.phoenix.core.integration.phoenix_chronicles;

/**
 * One step in a quest-attached tutorial sequence.
 *
 * <p>Highlight targets (used to punch a spotlight through the dim overlay):
 * <ul>
 *   <li>{@code "none"}       — no spotlight, dim covers the whole screen
 *   <li>{@code "sidebar"}    — the chapter sidebar
 *   <li>{@code "canvas"}     — the main quest canvas area
 *   <li>{@code "toolbar"}    — the toolbar/search strip
 *   <li>{@code "node:{id}"} — a specific quest node (e.g. {@code "node:tutorial/step1"})
 * </ul>
 *
 * <p>SNBT format inside a quest file:
 * <pre>
 * tutorial_steps: [
 *   {text: "Welcome to Chronicles!", highlight: "none"},
 *   {text: "The sidebar lists your chapters.", highlight: "sidebar"},
 *   {text: "Click a node to view quest details.", highlight: "canvas"}
 * ]
 * </pre>
 */
public record TutorialStep(String text, String highlight) {

    public static final String HL_NONE    = "none";
    public static final String HL_SIDEBAR = "sidebar";
    public static final String HL_CANVAS  = "canvas";
    public static final String HL_TOOLBAR = "toolbar";

    /** Convenience constructor — no spotlight. */
    public TutorialStep(String text) {
        this(text, HL_NONE);
    }

    public boolean hasHighlight() {
        return highlight != null && !highlight.isBlank() && !highlight.equals(HL_NONE);
    }

    public boolean isNodeHighlight() {
        return highlight != null && highlight.startsWith("node:");
    }

    /** Returns the quest-id path portion for a {@code "node:{id}"} highlight, or {@code null}. */
    public String nodeHighlightId() {
        return isNodeHighlight() ? highlight.substring(5) : null;
    }
}
