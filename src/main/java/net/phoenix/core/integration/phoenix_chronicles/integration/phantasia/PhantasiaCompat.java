package net.phoenix.core.integration.phoenix_chronicles.integration.phantasia;

import net.minecraftforge.fml.ModList;
import net.phoenix.core.integration.phoenix_chronicles.QuestNode;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewMachineTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ViewSceneTask;

public class PhantasiaCompat {

    public static final String PHANTASIA_MOD_ID = "phantasia";

    public static boolean isAvailable() {
        return ModList.get().isLoaded(PHANTASIA_MOD_ID);
    }

    public static void init() {}

    public static boolean canOpenForTask(QuestTask task) {
        if (!isAvailable()) return false;
        if (task instanceof ViewMachineTask vmt) {

            return false;
        }
        if (task instanceof ViewSceneTask vst) {

            return false;
        }
        return false;
    }

    public static void openForTask(QuestTask task, net.minecraft.client.gui.screens.Screen parent) {
        if (!isAvailable()) return;
        if (task instanceof ViewMachineTask vmt) {

        } else if (task instanceof ViewSceneTask vst) {

        }
    }

    public static Object createPreviewForNode(net.phoenix.core.integration.phoenix_chronicles.QuestNode node) {
        if (!isAvailable() || node == null) return null;
        for (QuestTask task : node.getTasks()) {
            if (task instanceof ViewMachineTask vmt) {

                return null;
            }
        }
        return null;
    }

    public static void tickPreview(Object preview) {
        if (preview == null) return;
    }

    public static void renderPreview(Object preview,
                                     net.minecraft.client.gui.GuiGraphics g,
                                     int x, int y, int w, int h, float partialTick) {
        if (preview == null) return;
    }

    public static boolean isPreviewReady(Object preview) {
        if (preview == null) return false;

        return false;
    }

    public static boolean previewMouseClicked(Object preview,
                                              double mx, double my,
                                              int x, int y, int w, int h,
                                              net.minecraft.client.gui.screens.Screen parent) {
        if (preview == null) return false;

        return false;
    }

    public static void closePreview(Object preview) {
        if (preview == null) return;
    }
}
