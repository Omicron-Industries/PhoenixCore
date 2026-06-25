package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side store for per-quest tutorial step progress.
 *
 * <p>Step index semantics:
 * <ul>
 *   <li>0 (default) = not yet started / at first step
 *   <li>N           = currently on step N
 *   <li>-1          = dismissed or all steps completed
 * </ul>
 *
 * <p>Progress is persisted to {@code config/phoenix_chronicles/tutorial_progress.dat}
 * (binary NBT) so it survives restarts without any server-side plumbing.
 */
public class TutorialProgressTracker {

    private static final Map<String, Integer> STEPS = new HashMap<>();
    private static Path dataFile;
    private static boolean initialized = false;

    // ── Init ──────────────────────────────────────────────────────────────────

    public static void init(Path chroniclesConfigDir) {
        dataFile = chroniclesConfigDir.resolve("tutorial_progress.dat");
        load();
        initialized = true;
    }

    public static boolean isInitialized() { return initialized; }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Current step index, or 0 if never started, or -1 if dismissed/completed. */
    public static int getStep(String questId) {
        return STEPS.getOrDefault(questId, 0);
    }

    public static boolean isDismissed(String questId) {
        return STEPS.getOrDefault(questId, 0) < 0;
    }

    // ── Mutations (each saves to disk) ────────────────────────────────────────

    public static void advance(String questId, int totalSteps) {
        int next = getStep(questId) + 1;
        STEPS.put(questId, next >= totalSteps ? -1 : next);
        save();
    }

    public static void back(String questId) {
        STEPS.put(questId, Math.max(0, getStep(questId) - 1));
        save();
    }

    public static void dismiss(String questId) {
        STEPS.put(questId, -1);
        save();
    }

    public static void reset(String questId) {
        STEPS.remove(questId);
        save();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            CompoundTag tag = NbtIo.read(dataFile.toFile());
            if (tag != null) tag.getAllKeys().forEach(k -> STEPS.put(k, tag.getInt(k)));
        } catch (IOException ignored) {}
    }

    private static void save() {
        if (dataFile == null) return;
        try {
            CompoundTag tag = new CompoundTag();
            STEPS.forEach(tag::putInt);
            Files.createDirectories(dataFile.getParent());
            NbtIo.write(tag, dataFile.toFile());
        } catch (IOException ignored) {}
    }
}
