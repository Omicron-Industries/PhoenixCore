package net.phoenix.core.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.phoenix.chromatic_codes.api.ChromaticEffectsRegistry;
import net.phoenix.chromatic_codes.config.ModConfig;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.SelectChromaticCodePacket;

import java.util.ArrayList;
import java.util.List;

public class ChromaticEffectSelectScreen extends Screen {

    private final InteractionHand hand;
    private final List<Character> availableCodes = new ArrayList<>();
    private static final int ENTRY_HEIGHT = 18;

    public ChromaticEffectSelectScreen(InteractionHand hand) {
        super(Component.literal("Select Chromatic Effect"));
        this.hand = hand;

        // --- SECTION 1: Pull from Custom Colors ---
        for (String entry : ModConfig.INSTANCE.colors.customColors) {
            String codeStr = entry.split(":")[0];
            if (!codeStr.isEmpty()) {
                availableCodes.add(codeStr.charAt(0));
            }
        }

        // --- SECTION 2: Pull from Custom Gradients ---
        for (String entry : ModConfig.INSTANCE.colors.customGradients) {
            String codeStr = entry.split(":")[0];
            if (!codeStr.isEmpty()) {
                availableCodes.add(codeStr.charAt(0));
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int x = centerX - 100;
        int y = 50;

        guiGraphics.drawCenteredString(this.font, "§b§lCHROMATIC_SPRAY_INTERFACE", centerX, 20, 0x00FF00);
        guiGraphics.drawCenteredString(this.font, "§7Select active flux code", centerX, 32, 0xAAAAAA);

        for (Character code : availableCodes) {
            // Preview using the actual effect
            Component preview = ChromaticEffectsRegistry.parseCustomEffects("&" + code + " CODE_TYPE: " + code);

            boolean hovering = mouseX >= x && mouseX <= x + 200 && mouseY >= y && mouseY <= y + (ENTRY_HEIGHT - 2);

            if (hovering) {
                guiGraphics.fill(x - 5, y - 2, x + 205, y + ENTRY_HEIGHT - 2, 0x2200FF00); // Green terminal hover
            }

            guiGraphics.drawString(this.font, preview, x, y, 0xFFFFFF);
            y += ENTRY_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.width / 2 - 100;
        int yStart = 50;

        for (int i = 0; i < availableCodes.size(); i++) {
            int entryY = yStart + (i * ENTRY_HEIGHT);

            if (mouseX >= x && mouseX <= x + 200 && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                char selectedCode = availableCodes.get(i);

                PhoenixNetwork.CHANNEL.sendToServer(new SelectChromaticCodePacket(hand, selectedCode));

                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
