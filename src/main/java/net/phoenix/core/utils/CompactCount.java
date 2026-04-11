package net.phoenix.core.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.text.NumberFormat;
import java.util.Locale;

public final class CompactCount {

    private CompactCount() {}

    public static Component createCompactComponentWithTooltip(String original) {
        String compacted = compactIfNumeric(original);

        if (compacted.equals(original)) {
            return Component.literal(original);
        }

        MutableComponent component = Component.literal(compacted);

        String cleaned = original.replace(",", "").replace("_", "").trim();
        String hoverText = original;

        try {
            long value = Long.parseLong(cleaned);
            hoverText = NumberFormat.getNumberInstance(Locale.US).format(value);
        } catch (NumberFormatException ignored) {}

        String finalHoverText = hoverText;
        component.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(finalHoverText))));

        return component;
    }

    public static String compactIfNumeric(String s) {
        if (s == null || s.isEmpty()) return s;

        String cleaned = s.replace(",", "").replace("_", "").trim();
        if (cleaned.isEmpty()) return s;

        for (int i = 0; i < cleaned.length(); i++) {
            if (!Character.isDigit(cleaned.charAt(i))) return s;
        }

        long v;
        try {
            v = Long.parseLong(cleaned);
        } catch (Throwable t) {
            return s;
        }

        if (v < 10_000) return s;

        if (v >= 1_000_000_000L) return fmt(v, 1_000_000_000L, "B");
        if (v >= 1_000_000L) return fmt(v, 1_000_000L, "M");
        return fmt(v, 1_000L, "k");
    }

    public static String fmt(long v, long unit, String suf) {
        double d = (double) v / (double) unit;

        if (d >= 100.0) return ((long) d) + suf;

        double t = Math.floor(d * 10.0) / 10.0;
        if (t == Math.floor(t)) return ((long) t) + suf;
        return t + suf;
    }
}
