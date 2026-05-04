package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Setter;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhantasiaSceneWidget extends Screen {

    // ── scene ─────────────────────────────────────────────────────────────────
    private final PhantasiaFakeLevel fakeLevel;
    private final CustomSceneRenderer renderer;
    private Vector3f center = new Vector3f(0, 0, 0);

    // ── camera state ──────────────────────────────────────────────────────────
    private float yaw = 45.0f;
    private float pitch = 30.0f;
    private float zoom = 10.0f;

    // ── input ─────────────────────────────────────────────────────────────────
    private boolean dragging = false;

    // ── back button ───────────────────────────────────────────────────────────
    @Setter
    private Runnable backCallback;
    private static final int BTN_X = 8, BTN_Y = 8, BTN_W = 72, BTN_H = 20;
    private final Screen parent;

    // ─────────────────────────────────────────────────────────────────────────

    public PhantasiaSceneWidget(net.minecraft.world.level.Level ignoredWorld,
                                MultiblockMachineDefinition definition,
                                Screen parent) {
        super(Component.literal("Phantasia Scene"));
        this.parent = parent;

        this.fakeLevel = new PhantasiaFakeLevel();

        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int pixelW = (int) (Minecraft.getInstance().getWindow().getScreenWidth() * guiScale);
        int pixelH = (int) (Minecraft.getInstance().getWindow().getScreenHeight() * guiScale);
        this.renderer = new CustomSceneRenderer(this.fakeLevel, pixelW, pixelH);
        this.renderer.setClearColor(0xFF0B0B10);

        this.center = buildScene(definition);
        this.fakeLevel.finalizeLoad();
        initializeMultiblock();
        this.fakeLevel.refreshModelData();

        updateCamera();
    }

    // ── multiblock initialization ─────────────────────────────────────────────

    private void initializeMultiblock() {
        boolean controllerFound = false;
        var blockEntities = new java.util.ArrayList<>(fakeLevel.getBlockEntities().values());

        for (BlockEntity be : blockEntities) {
            if (!(be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity machineBe)) continue;
            var machine = machineBe.getMetaMachine();
            if (!(machine instanceof MultiblockControllerMachine controller)) continue;

            controllerFound = true;
            System.out.println(
                    "[Phantasia] Found controller: " + machine.getClass().getName() + " @ " + be.getBlockPos());

            controller.setFrontFacing(net.minecraft.core.Direction.NORTH);
            controller.setFlipped(false);

            // The GTCEu machine model reads IS_FORMED directly from
            // machine.getRenderState() at draw time — not from ModelData.
            // So we just need setRenderState() to update the field on the machine object.
            var renderState = controller.getRenderState();
            System.out.println("[Phantasia] Current render state: " + renderState);
            System.out.println("[Phantasia] hasProperty(IS_FORMED): " +
                    renderState.hasProperty(GTMachineModelProperties.IS_FORMED));
            System.out.println("[Phantasia] Render state properties: " + renderState.getValues().keySet());

            if (renderState.hasProperty(GTMachineModelProperties.IS_FORMED)) {
                var newState = renderState.setValue(GTMachineModelProperties.IS_FORMED, true);
                controller.setRenderState(newState);
                System.out.println("[Phantasia] IS_FORMED set to true. Verify: " +
                        controller.getRenderState().getValue(GTMachineModelProperties.IS_FORMED));
            } else {
                // IS_FORMED is not registered on this definition's render state.
                // This means onStructureFormed() is the only way to mark it formed
                // but it requires a real pattern check. Try it guarded.
                System.out.println(
                        "[Phantasia] IS_FORMED not in render state — attempting onStructureFormed() with guard.");
                try {
                    controller.onStructureFormed();
                    System.out.println("[Phantasia] onStructureFormed() succeeded. isFormed=" + controller.isFormed());
                } catch (Exception e) {
                    System.out.println("[Phantasia] onStructureFormed() threw: " + e);
                    // Last resort: force isFormed field via reflection
                    try {
                        var field = MultiblockControllerMachine.class.getDeclaredField("isFormed");
                        field.setAccessible(true);
                        field.set(controller, true);
                        System.out.println("[Phantasia] isFormed forced via reflection.");
                    } catch (Exception ex) {
                        System.out.println("[Phantasia] Reflection fallback also failed: " + ex);
                    }
                }
            }
        }

        // Notify all machine BEs of neighbor change so hatches update their appearance
        for (BlockEntity be : blockEntities) {
            if (be instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity machineBe) {
                try {
                    machineBe.getMetaMachine().onNeighborChanged(null, be.getBlockPos(), false);
                } catch (Exception ignored) {}
            }
        }

        if (!controllerFound) {
            System.out.println("[Phantasia] WARNING: No controller found. Machine will look unformed.");
        }
    }

    // ── scene ─────────────────────────────────────────────────────────────────

    private Vector3f buildScene(MultiblockMachineDefinition definition) {
        Set<BlockPos> allPos = new HashSet<>();

        int sizeX = 5, sizeZ = 5;
        if (definition != null) {
            var shapes = definition.getMatchingShapes();
            if (!shapes.isEmpty()) {
                var bi = shapes.get(0).getBlocks();
                sizeX = bi[0][0].length;
                sizeZ = bi.length;
            }
        }
        int padX = sizeX / 2 + 2, padZ = sizeZ / 2 + 2;

        BlockState deepslate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.defaultBlockState();

        for (int bx = -padX; bx <= padX; bx++) {
            for (int bz = -padZ; bz <= padZ; bz++) {
                BlockPos p = new BlockPos(bx, -2, bz);
                fakeLevel.placeBlock(p, deepslate);
                allPos.add(p);
            }
        }

        for (int bx = -padX; bx <= padX; bx++) {
            for (int bz = -padZ; bz <= padZ; bz++) {
                boolean border = bx == -padX || bx == padX || bz == -padZ || bz == padZ;
                BlockPos p = new BlockPos(bx, -1, bz);
                fakeLevel.placeBlock(p, border ? smoothStone : deepslate);
                allPos.add(p);
            }
        }

        System.out.println("[Phantasia] Placed " + allPos.size() + " baseplate blocks into PhantasiaFakeLevel.");

        if (definition != null) {
            List<MultiblockShapeInfo> shapes = definition.getMatchingShapes();
            if (!shapes.isEmpty()) {
                com.lowdragmc.lowdraglib.utils.BlockInfo[][][] blocksInfo = shapes.get(0).getBlocks();
                int offX = -(sizeX / 2);
                int offZ = -(sizeZ / 2);

                int mbCount = 0;
                for (int z = 0; z < blocksInfo.length; z++)
                    for (int y = 0; y < blocksInfo[z].length; y++)
                        for (int x2 = 0; x2 < blocksInfo[z][y].length; x2++) {
                            com.lowdragmc.lowdraglib.utils.BlockInfo info = blocksInfo[z][y][x2];
                            BlockState state = (info != null &&
                                    info != com.lowdragmc.lowdraglib.utils.BlockInfo.EMPTY) ? info.getBlockState() :
                                            Blocks.AIR.defaultBlockState();
                            if (!state.isAir()) {
                                BlockPos pos = new BlockPos(x2 + offX, y, z + offZ);
                                fakeLevel.placeBlock(pos, state);
                                allPos.add(pos);
                                mbCount++;
                            }
                        }
                System.out.println("[Phantasia] Placed " + mbCount + " multiblock blocks.");
            } else {
                System.out.println("[Phantasia] No matching shapes found for: " + definition.getId());
            }
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : allPos) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        int span = Math.max(Math.max(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1);
        zoom = (float) (3.5 * Math.sqrt(Math.max(span, 1)));

        return new Vector3f(
                (minX + maxX) / 2.0f + 0.5f,
                (minY + maxY) / 2.0f + 0.5f,
                (minZ + maxZ) / 2.0f + 0.5f);
    }

    // ── camera ────────────────────────────────────────────────────────────────

    private void updateCamera() {
        renderer.setCameraLookAt(center, zoom,
                Math.toRadians(pitch), Math.toRadians(yaw));
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    public void destroy() {
        renderer.deleteCacheBuffer();
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        renderer.render(graphics, 0, 0, this.width, this.height);

        boolean hover = mouseX >= BTN_X && mouseX <= BTN_X + BTN_W && mouseY >= BTN_Y && mouseY <= BTN_Y + BTN_H;
        graphics.fill(BTN_X, BTN_Y, BTN_X + BTN_W, BTN_Y + BTN_H,
                hover ? 0xCCFFFFFF : 0xAA000000);
        graphics.renderOutline(BTN_X, BTN_Y, BTN_W, BTN_H,
                hover ? 0xFFAAAAAA : 0xFF555555);
        var font = Minecraft.getInstance().font;
        String label = "\u2190 Back";
        graphics.drawString(font, label,
                BTN_X + (BTN_W - font.width(label)) / 2,
                BTN_Y + (BTN_H - font.lineHeight) / 2 + 1,
                hover ? 0xFF222222 : 0xFFCCCCCC, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {}

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= BTN_X && mx <= BTN_X + BTN_W && my >= BTN_Y && my <= BTN_Y + BTN_H) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        dragging = true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!dragging) return false;
        yaw = (yaw + (float) dx + 360f) % 360f;
        pitch = (float) Mth.clamp(pitch + dy, -89.9, 89.9);
        updateCamera();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        zoom = (float) Mth.clamp(zoom + (delta < 0 ? 0.5 : -0.5), 1.0, 999.0);
        updateCamera();
        return true;
    }
}
