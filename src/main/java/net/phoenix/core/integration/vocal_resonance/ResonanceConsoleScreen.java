package net.phoenix.core.integration.vocal_resonance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.RecordItem;
import net.phoenix.core.integration.vocal_vibrancy.VocalVibrancyClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResonanceConsoleScreen extends Screen {

    private final ResonantJukeboxMachine machine;

    private EditBox searchBox, urlBox;
    private final List<Button> registryButtons = new ArrayList<>();
    private List<ResourceLocation> filteredSounds = new ArrayList<>();
    private List<ResourceLocation> musicDiscs = new ArrayList<>();

    private int scrollOffset = 0;
    private boolean discDropdownOpen = false;
    private final List<Button> discButtons = new ArrayList<>();

    private String lastRenderedSelectedSound = "";

    public ResonanceConsoleScreen(ResonantJukeboxMachine machine) {
        super(Component.literal("Resonance Mainframe"));
        this.machine = machine;
    }

    @Override
    protected void init() {
        super.init();

        VocalVibrancyClient.startTracking(machine.getPos());

        int panelWidth = this.width / 3;
        int center = this.width / 2;
        this.lastRenderedSelectedSound = machine.selectedLibrarySound;

        this.searchBox = new EditBox(font, 20, 50, panelWidth - 40, 20, Component.literal("Search..."));
        this.searchBox.setValue(machine.searchTerm);
        this.searchBox.setResponder(val -> {
            machine.searchTerm = val;
            this.scrollOffset = 0;
            updateFilteredSounds();
        });
        this.addRenderableWidget(this.searchBox);

        this.musicDiscs = scanForMusicDiscs();
        this.addRenderableWidget(Button.builder(Component.literal("§6Select Music Disc ▼"), b -> {
            this.discDropdownOpen = !this.discDropdownOpen;
            updateDiscDropdown();
        }).pos(center - (panelWidth / 2) + 10, 50).size(panelWidth - 20, 20).build());

        this.urlBox = new EditBox(font, this.width - panelWidth + 20, 50, panelWidth - 40, 20,
                Component.literal("Enter Stream URL..."));
        this.urlBox.setMaxLength(256);
        this.urlBox.setValue(machine.currentStreamUrl);
        this.addRenderableWidget(this.urlBox);

        this.addRenderableWidget(Button.builder(Component.literal("§bConnect Stream"), b -> {
            machine.currentStreamUrl = urlBox.getValue();
            machine.syncAndGeneralUpdate();
        }).pos(this.width - panelWidth + 20, 75).size(panelWidth - 40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§aRETURN TO FACTORY"), b -> this.onClose())
                .pos(center - 100, this.height - 30).size(200, 20).build());

        updateFilteredSounds();
    }

    @Override
    public void tick() {
        super.tick();

        if (!machine.selectedLibrarySound.equals(lastRenderedSelectedSound)) {
            this.lastRenderedSelectedSound = machine.selectedLibrarySound;
            updateFilteredSounds();
            if (discDropdownOpen) {
                updateDiscDropdown();
            }
        }
    }

    @Override
    public void onClose() {
        VocalVibrancyClient.stopTracking(machine.getPos());
        super.onClose();
    }

    private List<ResourceLocation> scanForMusicDiscs() {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof RecordItem)
                .map(item -> ((RecordItem) item).getSound().getLocation())
                .collect(Collectors.toList());
    }

    private void updateFilteredSounds() {
        this.registryButtons.forEach(this::removeWidget);
        this.registryButtons.clear();

        this.filteredSounds = BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .filter(loc -> loc.toString().contains(machine.searchTerm.toLowerCase()))
                .sorted((a, b) -> a.getPath().compareTo(b.getPath()))
                .toList();

        int btnX = 20;
        int btnWidth = (this.width / 3) - 40;
        int startY = 80;
        int maxVisible = (this.height - 120) / 14;

        for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisible, filteredSounds.size()); i++) {
            ResourceLocation res = filteredSounds.get(i);
            boolean isSelected = machine.selectedLibrarySound.equals(res.toString());

            Button btn = Button.builder(
                    Component.literal((isSelected ? "§b▶ " : "§7") + res.getPath()),
                    b -> {
                        machine.selectedLibrarySound = res.toString();
                        machine.currentStreamUrl = "";
                        urlBox.setValue("");
                        machine.syncAndGeneralUpdate();
                        updateFilteredSounds();
                    })
                    .pos(btnX, startY)
                    .size(btnWidth, 12)
                    .build();

            this.registryButtons.add(btn);
            this.addRenderableWidget(btn);
            startY += 13;
        }
    }

    private void updateDiscDropdown() {
        this.discButtons.forEach(this::removeWidget);
        this.discButtons.clear();
        if (!discDropdownOpen) return;

        int startY = 72;
        int btnX = this.width / 2 - (this.width / 6) + 10;
        int btnWidth = this.width / 3 - 20;

        for (ResourceLocation discSound : musicDiscs) {
            Button b = Button.builder(Component.literal("§e" + discSound.getPath()), btn -> {
                machine.selectedLibrarySound = discSound.toString();
                machine.currentStreamUrl = "";
                urlBox.setValue("");
                machine.syncAndGeneralUpdate();
                this.discDropdownOpen = false;
                updateDiscDropdown();
            }).pos(btnX, startY).size(btnWidth, 12).build();

            this.discButtons.add(b);
            this.addRenderableWidget(b);
            startY += 13;
            if (startY > this.height - 60) break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < this.width / 3.0) {
            this.scrollOffset = Math.max(0, Math.min(filteredSounds.size() - 1, scrollOffset - (int) delta));
            updateFilteredSounds();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int third = this.width / 3;

        graphics.fill(10, 25, third - 10, this.height - 40, 0x33000000);
        graphics.fill(third + 5, 25, (third * 2) - 5, this.height - 40, 0x33000000);
        graphics.fill((third * 2) + 10, 25, this.width - 10, this.height - 40, 0x33000000);

        graphics.drawCenteredString(font, "§bAUDIO REGISTRY", third / 2, 35, 0xFFFFFF);
        graphics.drawCenteredString(font, "§6QUICK SELECT", this.width / 2, 35, 0xFFFFFF);
        graphics.drawCenteredString(font, "§dREMOTE LINK", this.width - (third / 2), 35, 0xFFFFFF);
        graphics.drawString(font, "§8Direct streams only.", (third * 2) + 20, 100, 0xFFFFFF);

        if (!filteredSounds.isEmpty()) {
            graphics.drawString(font,
                    "§8" + (scrollOffset + 1) + "-" + (scrollOffset + registryButtons.size()) +
                            " / " + filteredSounds.size(),
                    20, this.height - 52, 0xFFFFFF);
        }

        int boxY = this.height - 95;
        int boxX = (third * 2) + 20;
        graphics.fill(boxX, boxY, this.width - 20, boxY + 50, 0xAA000000);

        if (!machine.currentStreamUrl.isEmpty()) {
            String displayUrl = machine.currentStreamUrl.length() > 34 ?
                    machine.currentStreamUrl.substring(0, 31) + "..." : machine.currentStreamUrl;
            graphics.drawString(font, "§7Stream: §f" + displayUrl, boxX + 5, boxY + 5, 0xFFFFFF);
            graphics.drawString(font, "§7" + machine.streamTitle, boxX + 5, boxY + 20, 0xFFFFFF);
        } else if (!machine.selectedLibrarySound.isEmpty()) {
            graphics.drawString(font, "§7Target: §f" + machine.selectedLibrarySound, boxX + 5, boxY + 5, 0xFFFFFF);
            graphics.drawString(font, "§7Status: §aTRANSMITTING", boxX + 5, boxY + 20, 0xFFFFFF);
            int barWidth = (int) (60 * machine.currentLiveBass);
            graphics.fill(boxX + 5, boxY + 35, boxX + 5 + barWidth, boxY + 40, 0xFF00FFFF);
        } else {
            graphics.drawString(font, "§8IDLE...", boxX + 5, boxY + 20, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
