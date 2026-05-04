package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PhantasiaSceneSelectionScreen extends Screen {

    private final Screen parent;
    public static final List<MultiblockMachineDefinition> PHANTASIA_SCENES = new ArrayList<>();

    public PhantasiaSceneSelectionScreen(Screen parent) {
        super(Component.literal("Phantasia Selection"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int x = (this.width - buttonWidth) / 2;
        int y = 40;

        for (MultiblockMachineDefinition def : PHANTASIA_SCENES) {
            // Standard Vanilla Button
            this.addRenderableWidget(Button.builder(
                    Component.literal(def.getLangValue()),
                    (btn) -> {
                        Minecraft.getInstance().setScreen(new PhantasiaSceneWidget(
                                Minecraft.getInstance().level,
                                def,
                                this // Use this screen as parent to come back here
                        ));
                    })
                    .bounds(x, y, buttonWidth, 20)
                    .build());

            y += 25;
        }

        // Back Button at bottom
        this.addRenderableWidget(Button.builder(Component.literal("Back"), (btn) -> this.onClose())
                .bounds(x, this.height - 30, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
