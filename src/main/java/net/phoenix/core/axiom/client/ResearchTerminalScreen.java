package net.phoenix.core.axiom.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.axiom.AxiomDataType;
import net.phoenix.core.axiom.terminal.ResearchTerminalBlockEntity;

/**
 * Research Terminal GUI — shows accumulated data per type and the research tree.
 * Tree canvas and node rendering will be added in the next pass.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchTerminalScreen extends Screen {

    private final ResearchTerminalBlockEntity terminal;

    public ResearchTerminalScreen(ResearchTerminalBlockEntity terminal) {
        super(Component.literal("Axiom Research"));
        this.terminal = terminal;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);

        // Data summary — placeholder until the full tree canvas is built
        int y = 20;
        g.drawCenteredString(font, "§bAxiom Research Terminal", width / 2, y, 0xFFFFFF);
        y += 16;
        for (AxiomDataType type : AxiomDataType.values()) {
            if (!type.isAvailable()) continue;
            String line = type.displayName + ": " + terminal.getStored(type) + " / " + terminal.getCapacity(type);
            g.drawString(font, type.displayComponent().getString() + " " + line, 20, y, 0xFFFFFF, false);
            y += 10;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
