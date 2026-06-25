package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TerminalScreen extends Screen {

    private final Screen parent;
    private final List<String> consoleHistory = new ArrayList<>();

    private String inputBuffer = "";
    private int cursorPosition = 0;
    private int frameTickCounter = 0;

    public TerminalScreen(Screen parent) {
        super(Component.literal("PHOENIX SYSTEM TERMINAL"));
        this.parent = parent;

        // Initial system boot logs
        consoleHistory.add("§a[SYS] Initializing Core Terminal Protocol...");
        consoleHistory.add("§e[WARN] Remote connection unencrypted.");
        consoleHistory.add("§b[READY] Enter directives below. Support color tags (& or §).");
    }

    @Override
    protected void init() {
        this.clearWidgets();

        // Return button placed out of the terminal layout boundaries
        this.addRenderableWidget(Button.builder(Component.literal("§c[ DISCONNECT ]"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(this.width - 115, this.height - 30, 100, 20).build());
    }

    /**
     * Captures raw alphanumeric string typed characters globally from the OS hook loop.
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Only accept standard printable keyboard characters
        if (codePoint >= 32 && codePoint != 127) {
            // Midline Insertion: Split string at cursor pointer location and stitch the new character in
            String left = inputBuffer.substring(0, cursorPosition);
            String right = inputBuffer.substring(cursorPosition);

            inputBuffer = left + codePoint + right;
            cursorPosition++; // Shift cursor index right
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    /**
     * Intercepts function keys, escape mechanics, backspaces, and directional navigation vectors.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return true;
        }

        // 1. Cursor Backspace Action (Delete character BEHIND cursor)
        if (keyCode == InputConstants.KEY_BACKSPACE && cursorPosition > 0 && !inputBuffer.isEmpty()) {
            String left = inputBuffer.substring(0, cursorPosition - 1);
            String right = inputBuffer.substring(cursorPosition);
            inputBuffer = left + right;
            cursorPosition--;
            return true;
        }

        // 2. Cursor Delete Action (Delete character In FRONT of cursor)
        if (keyCode == InputConstants.KEY_DELETE && cursorPosition < inputBuffer.length()) {
            String left = inputBuffer.substring(0, cursorPosition);
            String right = inputBuffer.substring(cursorPosition + 1);
            inputBuffer = left + right;
            return true;
        }

        // 3. Move Cursor Left
        if (keyCode == InputConstants.KEY_LEFT) {
            if (cursorPosition > 0) {
                cursorPosition--;
            }
            return true;
        }

        // 4. Move Cursor Right
        if (keyCode == InputConstants.KEY_RIGHT) {
            if (cursorPosition < inputBuffer.length()) {
                cursorPosition++;
            }
            return true;
        }

        // 5. Jump to Start (HOME Key)
        if (keyCode == InputConstants.KEY_HOME) {
            cursorPosition = 0;
            return true;
        }

        // 6. Jump to End (END Key)
        if (keyCode == InputConstants.KEY_END) {
            cursorPosition = inputBuffer.length();
            return true;
        }

        // 7. Execution (ENTER Key Command Processing)
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (!inputBuffer.trim().isEmpty()) {
                executeTerminalDirective(inputBuffer);
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void executeTerminalDirective(String command) {
        // Format ampersands to standard format markers automatically
        String formattedCmd = command.replace("&", "§");

        // Push raw input string into historical display cache lists
        consoleHistory.add("§7$ " + formattedCmd);

        // Process clear command locally
        if (command.equalsIgnoreCase("clear")) {
            consoleHistory.clear();
            consoleHistory.add("§b[SYS] Console buffer cleared.");
        } else {
            // Echo or hook up your custom mod handling logic pipelines right here!
            consoleHistory.add("§cUnknown Directive System Response: §f" + formattedCmd);
        }

        // Flush and clean local storage targets for next execution loops
        this.inputBuffer = "";
        this.cursorPosition = 0;
    }

    @Override
    public void tick() {
        super.tick();
        frameTickCounter++; // Driving variable used to compute blinking cursor visibility updates
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        // Core Layout Positioning Definitions
        int termX = 20;
        int termY = 20;
        int termW = this.width - 40;
        int termH = this.height - 60;

        // Draw solid black retro backing frame
        graphics.fill(termX, termY, termX + termW, termY + termH, 0xFA050805);
        graphics.renderOutline(termX, termY, termW, termH, 0xFF33AA33);

        // --- Render Scrolling Historical Command Output Area ---
        int maxVisibleLines = (termH - 25) / 12;
        int startLineIdx = Math.max(0, consoleHistory.size() - maxVisibleLines);
        int currentLineY = termY + 10;

        for (int i = startLineIdx; i < consoleHistory.size(); i++) {
            String logLine = consoleHistory.get(i).replace("&", "§");
            graphics.drawString(this.font, logLine, termX + 12, currentLineY, 0xFFFFFF);
            currentLineY += 12;
        }

        // --- Render Interactive CommandLine Input Area ---
        int inputLineY = termY + termH - 16;
        graphics.fill(termX + 5, inputLineY - 3, termX + termW - 5, inputLineY + 11, 0xFF0D130D);
        graphics.renderOutline(termX + 5, inputLineY - 3, termW - 10, 14, 0xFF225522);

        // Auto-convert color identifiers live while writing
        String renderText = inputBuffer.replace("&", "§");
        String fixedPrompt = "§aroot@phoenix:~# §f" + renderText;
        graphics.drawString(this.font, fixedPrompt, termX + 10, inputLineY, 0xFFFFFF);

        // --- PERFORMANCE TRIUMPH: Custom Midline Blinking Cursor Rendering Engine ---
        // Calculate the physical offset length of the user input string up to the cursor pointer position index
        String stringUpToCursor = "root@phoenix:~# " + inputBuffer.substring(0, cursorPosition);

        // Strip out formatting characters (§ + code) from the spatial layout tracking calculations,
        // otherwise the cursor will render shifted far too many pixels to the right!
        String cleanStringForLength = stringUpToCursor.replaceAll("§[0-9a-fk-orA-FK-ORxX]", "");
        int pixelCursorOffset = this.font.width(cleanStringForLength);

        // Blink logic calculation cycle rules (Changes visibility flags every 10 ticks)
        if ((frameTickCounter / 10) % 2 == 0) {
            int cursorRenderX = termX + 10 + pixelCursorOffset;
            graphics.fill(cursorRenderX, inputLineY - 1, cursorRenderX + 2, inputLineY + 9, 0xFF00FF00);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
