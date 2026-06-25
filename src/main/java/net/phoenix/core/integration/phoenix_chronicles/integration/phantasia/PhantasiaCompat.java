package net.phoenix.core.integration.phoenix_chronicles.integration.phantasia;

import net.minecraftforge.fml.ModList;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.C2SPhantasiaTaskCompletePacket;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewMachineTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewSceneTask;
import net.phoenix.core.network.PhoenixNetwork;

// ═══════════════════════════════════════════════════════════════════════════════
// Phantasia API imports — uncomment once 'phantasia' is added as a compile dep
// in build.gradle:
//
// dependencies {
// compileOnly("net.phoenixvine:phantasia:${phantasia_version}")
// // or: implementation fg.deobf("net.phoenixvine:phantasia:${phantasia_version}")
// }
//
// import net.phoenixvine.phantasia.api.PhantasiaAPI;
// import net.phoenixvine.phantasia.api.PhantasiaBlockInspectCompat;
// import net.phoenixvine.phantasia.api.events.PhantasiaEvents;
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Phantasia mod integration for Phoenix Chronicles.
 *
 * This class is structured but NOT yet wired up — Phantasia isn't published yet.
 *
 * ── TO ACTIVATE ──────────────────────────────────────────────────────────────
 *
 * 1. Add Phantasia as a compile-time dependency in build.gradle (see imports above).
 *
 * 2. In PhoenixCore's mod constructor (or FMLCommonSetupEvent), add:
 * if (PhantasiaCompat.isAvailable()) PhantasiaCompat.init();
 *
 * 3. Register C2SPhantasiaTaskCompletePacket in PhoenixNetwork:
 * CHANNEL.registerMessage(nextId++, C2SPhantasiaTaskCompletePacket.class,
 * C2SPhantasiaTaskCompletePacket::encode,
 * C2SPhantasiaTaskCompletePacket::new,
 * C2SPhantasiaTaskCompletePacket::handle);
 *
 * 4. Uncomment the imports above and the API calls marked [UNCOMMENT] below.
 *
 * 5. Register ViewMachineTask and ViewSceneTask in QuestTask deserialization:
 * In QuestFileLoader (or wherever task types are parsed from SNBT), add:
 * case "view_machine" -> new ViewMachineTask(taskId, desc,
 * tag.getString("machine_id"),
 * tag.contains("min_seconds") ? tag.getFloat("min_seconds") : 3.0f);
 * case "view_scene" -> new ViewSceneTask(taskId, desc,
 * tag.getString("scene_id"),
 * tag.contains("min_seconds") ? tag.getFloat("min_seconds") : 5.0f);
 *
 * ── WHAT THIS GIVES YOU ──────────────────────────────────────────────────────
 *
 * • view_machine tasks — quest objective that completes when the player views
 * a Phantasia build guide for a specific machine (with a configurable time floor).
 *
 * • view_scene tasks — same, but for multi-machine scenes.
 *
 * • Block inspector compat — quests that involve a machine (view_machine tasks)
 * show a "Needed for: <quest>" hint in Phantasia's block inspector overlay.
 *
 * • Multiblock provider — PhoenixCore's own machines are registered with Phantasia
 * so they appear in the viewer without separate data-pack JSON.
 * (See PhoenixMultiblockProvider for the implementation.)
 */
public class PhantasiaCompat {

    public static final String PHANTASIA_MOD_ID = "phantasia";

    /** Guard for all compat calls — check this before calling init(). */
    public static boolean isAvailable() {
        return ModList.get().isLoaded(PHANTASIA_MOD_ID);
    }

    /**
     * Call from your mod constructor or FMLCommonSetupEvent, guarded by isAvailable().
     * All lines marked [UNCOMMENT] require the Phantasia dep to be on the classpath.
     */
    public static void init() {
        // Register block inspector compat — adds "Needed for: <quest>" hints
        // to Phantasia's right-click block inspector overlay.
        // [UNCOMMENT] PhantasiaBlockInspectCompat.register((block, lines, setRole) -> {
        // for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
        // for (QuestTask task : node.getTasks()) {
        // if (task instanceof ViewMachineTask vmt) {
        // // [UNCOMMENT] if (PhantasiaAPI.isAvailable(vmt.getMachineId())) {
        // // lines.add(net.minecraft.network.chat.Component.literal(
        // // "§8Needed for: §7" + node.getTitle().getString()));
        // // }
        // }
        // }
        // }
        // });
    }

    // ── Client-side Phantasia event subscribers ───────────────────────────────
    //
    // This inner class is registered on the Forge event bus. When Phantasia is
    // added as a dep, remove the surrounding comment block so the @SubscribeEvent
    // methods become active.
    //
    // The annotation @Mod.EventBusSubscriber(value = Dist.CLIENT) ensures these
    // are only registered on the logical client — Phantasia events are client-only.

    /*
     * [UNCOMMENT THIS ENTIRE BLOCK when Phantasia dep is available]
     * 
     * @Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
     * public static class ClientEvents {
     * 
     * @SubscribeEvent
     * public static void onMachineViewerClose(PhantasiaEvents.ViewerClose event) {
     * Player player = Minecraft.getInstance().player;
     * if (player == null) return;
     * 
     * String closedMachineId = event.getMachineId();
     * float secondsViewed = event.getSecondsViewed();
     * 
     * player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
     * for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
     * QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
     * if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;
     * 
     * for (QuestTask task : node.getTasks()) {
     * if (!(task instanceof ViewMachineTask vmt)) continue;
     * if (task.isCompletedFor(player)) continue;
     * if (!closedMachineId.equals(vmt.getMachineId())) continue;
     * if (secondsViewed < vmt.getMinSeconds()) continue;
     * 
     * // Mark locally so the UI updates immediately without waiting for server round-trip
     * vmt.markCompletedClient(player);
     * 
     * // Notify the server so it persists and triggers quest completion checks
     * PhoenixNetwork.CHANNEL.sendToServer(
     * new C2SPhantasiaTaskCompletePacket(node.getId(), task.getTaskId()));
     * }
     * }
     * });
     * }
     * 
     * @SubscribeEvent
     * public static void onSceneViewerClose(PhantasiaEvents.SceneViewerClose event) {
     * Player player = Minecraft.getInstance().player;
     * if (player == null) return;
     * 
     * String closedSceneId = event.getSceneId();
     * float secondsViewed = event.getSecondsViewed();
     * 
     * player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
     * for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
     * QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
     * if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;
     * 
     * for (QuestTask task : node.getTasks()) {
     * if (!(task instanceof ViewSceneTask vst)) continue;
     * if (task.isCompletedFor(player)) continue;
     * if (!closedSceneId.equals(vst.getSceneId())) continue;
     * if (secondsViewed < vst.getMinSeconds()) continue;
     * 
     * vst.markCompletedClient(player);
     * 
     * PhoenixNetwork.CHANNEL.sendToServer(
     * new C2SPhantasiaTaskCompletePacket(node.getId(), task.getTaskId()));
     * }
     * }
     * });
     * }
     * }
     * 
     */  // end [UNCOMMENT BLOCK]

    // ── "View in Phantasia" button helper ─────────────────────────────────────
    //
    // Use this in ChronicleOverviewScreen / QuestTasksScreen to show a
    // "View in Phantasia ▶" button next to any ViewMachineTask.
    //
    // Example (in your screen's render or mouseClicked):
    //
    // if (PhantasiaCompat.canOpenForTask(task)) {
    // // render the button at (bx, by)
    // // on click: PhantasiaCompat.openForTask(task, this);
    // }

    /**
     * Returns true if the given task is a ViewMachineTask whose machine id is
     * known to Phantasia. Safe to call even when Phantasia is absent — returns false.
     */
    public static boolean canOpenForTask(QuestTask task) {
        if (!isAvailable()) return false;
        if (task instanceof ViewMachineTask vmt) {
            // [UNCOMMENT] return PhantasiaAPI.hasScript(vmt.getMachineId());
            return false; // placeholder until dep is available
        }
        if (task instanceof ViewSceneTask vst) {
            // [UNCOMMENT] return PhantasiaAPI.hasScene(vst.getSceneId());
            return false;
        }
        return false;
    }

    /**
     * Opens the Phantasia viewer for a ViewMachineTask or ViewSceneTask.
     * Safe no-op when Phantasia is absent or the machine/scene isn't registered.
     *
     * @param task   the task to open a viewer for
     * @param parent the Screen Phantasia should return to on close
     */
    public static void openForTask(QuestTask task, net.minecraft.client.gui.screens.Screen parent) {
        if (!isAvailable()) return;
        if (task instanceof ViewMachineTask vmt) {
            // [UNCOMMENT] PhantasiaAPI.openForMachine(vmt.getMachineId(), parent);
        } else if (task instanceof ViewSceneTask vst) {
            // [UNCOMMENT] PhantasiaAPI.openScene(vst.getSceneId(), parent);
        }
    }

    // ── Embedded 3D preview widget ────────────────────────────────────────────
    //
    // These helpers let ChronicleOverviewScreen manage a PhantasiaMachinePreview
    // without importing the Phantasia type directly. All methods are safe no-ops
    // (or return null) when Phantasia is absent.
    //
    // The preview is stored as Object in the screen to avoid the compile dep.
    // Cast to PhantasiaMachinePreview inside the [UNCOMMENT] blocks once the dep
    // is on the classpath.

    /**
     * Creates a preview for the first ViewMachineTask on the given quest node,
     * or returns null if Phantasia is absent / no qualifying task exists.
     *
     * Call this when selectedNode changes (not on every rebuild — recreating the
     * preview resets the camera and triggers an async reload).
     */
    public static Object createPreviewForNode(net.phoenix.core.integration.phoenix_chronicles.QuestNode node) {
        if (!isAvailable() || node == null) return null;
        for (QuestTask task : node.getTasks()) {
            if (task instanceof ViewMachineTask vmt) {
                // [UNCOMMENT]
                // var preview = PhantasiaAPI.createPreview(vmt.getMachineId());
                // if (preview != null) {
                // preview.setAutoSpin(20f); // gentle spin
                // }
                // return preview;
                return null; // placeholder
            }
        }
        return null;
    }

    /**
     * Advances the preview camera animation. Call once per render frame
     * (before rendering) if preview is non-null.
     */
    public static void tickPreview(Object preview) {
        if (preview == null) return;
        // [UNCOMMENT] ((net.phoenixvine.phantasia.api.PhantasiaMachinePreview) preview).tick();
    }

    /**
     * Renders the preview into the given screen rectangle.
     * Safe to call before isPreviewReady() — Phantasia shows a "Loading…" placeholder.
     */
    public static void renderPreview(Object preview,
                                     net.minecraft.client.gui.GuiGraphics g,
                                     int x, int y, int w, int h, float partialTick) {
        if (preview == null) return;
        // [UNCOMMENT]
        // ((net.phoenixvine.phantasia.api.PhantasiaMachinePreview) preview)
        // .render(g, x, y, w, h, partialTick);
    }

    /**
     * Returns true once the async pattern load is complete and the 3D view is live.
     * Use to drive your own placeholder while loading if desired.
     */
    public static boolean isPreviewReady(Object preview) {
        if (preview == null) return false;
        // [UNCOMMENT] return ((net.phoenixvine.phantasia.api.PhantasiaMachinePreview) preview).isReady();
        return false;
    }

    /**
     * Forwards a mouse click to the preview (lets the player drag-to-rotate).
     * Returns true if the preview consumed the click.
     *
     * @param x/y/w/h the rectangle the preview was rendered into
     */
    public static boolean previewMouseClicked(Object preview,
                                              double mx, double my,
                                              int x, int y, int w, int h,
                                              net.minecraft.client.gui.screens.Screen parent) {
        if (preview == null) return false;
        // [UNCOMMENT]
        // return ((net.phoenixvine.phantasia.api.PhantasiaMachinePreview) preview)
        // .mouseClicked(mx, my, x, y, w, h, parent);
        return false;
    }

    /**
     * Releases GL resources. Call from Screen.onClose() and whenever the selected
     * node changes so the old preview doesn't leak a dummy world.
     */
    public static void closePreview(Object preview) {
        if (preview == null) return;
        // [UNCOMMENT] ((net.phoenixvine.phantasia.api.PhantasiaMachinePreview) preview).close();
    }
}
