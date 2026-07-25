package net.phoenix.core.integration.recipe_helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.PacketRecipeBuilderGenerate;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeBuilderScreen extends AbstractContainerScreen<RecipeBuilderMenu> {

    static final int GUI_W = RecipeBuilderMenu.GUI_W;
    static final int GUI_H = RecipeBuilderMenu.GUI_H;

    RecipeTypeDropdown recipeTypeDropdown;
    EditBox recipeIdBox, durationBox, eutBox, sourceInBox, sourceOutBox;
    public SlotPanel itemInputPanel;
    public SlotPanel itemOutputPanel;
    public FluidSlotPanel fluidInputPanel;
    public FluidSlotPanel fluidOutputPanel;
    AmountEditor amountEditor;

    @Getter
    private final List<ConditionsScreen.ConditionEntry> conditions = new ArrayList<>();

    private enum Page {
        GENERAL,
        ITEMS,
        FLUIDS
    }

    private Page currentPage = Page.GENERAL;

    public RecipeBuilderScreen(RecipeBuilderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.titleLabelX = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos, y = this.topPos;

        int tw = 80;
        addRenderableWidget(Button.builder(Component.literal("General"),
                b -> setPage(Page.GENERAL)).bounds(x + 4, y + 13, tw, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Items"),
                b -> setPage(Page.ITEMS)).bounds(x + 84, y + 13, tw, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Fluids"),
                b -> setPage(Page.FLUIDS)).bounds(x + 164, y + 13, tw, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Conditions →"),
                b -> openConditions()).bounds(x + 244, y + 13, 90, 14).build());

        recipeTypeDropdown = new RecipeTypeDropdown(x + 6, y + 42, 156, 14, this);
        recipeIdBox = box(x + 168, y + 42, 164, "recipe_id", 64);
        durationBox = box(x + 6, y + 72, 54, "duration", 12);
        eutBox = box(x + 64, y + 72, 72, "EUt", 20);
        sourceInBox = box(x + 140, y + 72, 58, "src in", 10);
        sourceOutBox = box(x + 202, y + 72, 58, "src out", 10);

        itemInputPanel = new SlotPanel(x + 6, y + 42, 15, "Item Inputs", this);
        itemOutputPanel = new SlotPanel(x + 6, y + 82, 15, "Item Outputs", this);
        addRenderableWidget(itemInputPanel);
        addRenderableWidget(itemOutputPanel);

        fluidInputPanel = new FluidSlotPanel(x + 6, y + 42, 12, "Fluid Inputs", this);
        fluidOutputPanel = new FluidSlotPanel(x + 6, y + 80, 12, "Fluid Outputs", this);
        addRenderableWidget(fluidInputPanel);
        addRenderableWidget(fluidOutputPanel);

        addRenderableWidget(recipeTypeDropdown);

        amountEditor = new AmountEditor(0, 0, font);
        addRenderableWidget(amountEditor);

        int btnY = y + 153;
        addRenderableWidget(Button.builder(Component.literal("Clear"),
                b -> onClear()).bounds(x + 6, btnY, 46, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Copy KJS"),
                b -> onCopyKjs()).bounds(x + 56, btnY, 64, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Copy Java"),
                b -> onCopyJava()).bounds(x + 124, btnY, 64, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Send to Server"),
                b -> onSendToServer()).bounds(x + 192, btnY, 90, 14).build());

        setPage(Page.GENERAL);
        RecipeBuilderState.INSTANCE.restore(this);
    }

    private void openConditions() {
        RecipeBuilderState.INSTANCE.save(this);
        Minecraft.getInstance().setScreen(new ConditionsScreen(this, conditions));
    }

    public void setConditions(List<ConditionsScreen.ConditionEntry> list) {
        this.conditions.clear();
        this.conditions.addAll(list);
    }

    private void setPage(Page page) {
        this.currentPage = page;
        boolean g = page == Page.GENERAL, i = page == Page.ITEMS, f = page == Page.FLUIDS;
        recipeTypeDropdown.visible = g;
        recipeIdBox.visible = durationBox.visible = eutBox.visible = sourceInBox.visible = sourceOutBox.visible = g;
        itemInputPanel.visible = i;
        itemOutputPanel.visible = i;
        fluidInputPanel.setVisible(f);
        fluidOutputPanel.setVisible(f);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (currentPage == Page.GENERAL) {
            drawLabel(g, "Recipe Type", leftPos + 6, topPos + 32);
            drawLabel(g, "Recipe ID", leftPos + 168, topPos + 32);
            drawLabel(g, "Duration", leftPos + 6, topPos + 62);
            drawLabel(g, "EUt", leftPos + 64, topPos + 62);
            drawLabel(g, "Source In", leftPos + 140, topPos + 62);
            drawLabel(g, "Source Out", leftPos + 202, topPos + 62);
        }
        if (currentPage == Page.ITEMS) {
            ItemStack s = itemInputPanel.getStackUnderMouse(mx, my);
            if (s.isEmpty()) s = itemOutputPanel.getStackUnderMouse(mx, my);
            if (!s.isEmpty()) g.renderTooltip(font, s, mx, my);
        }
        if (!conditions.isEmpty()) {
            String badge = "§d" + conditions.size() + " condition" + (conditions.size() == 1 ? "" : "s");
            g.drawString(font, badge, leftPos + 246, topPos + 29, 0xFFFFFF, false);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mx, int my) {}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + GUI_W, y + GUI_H, 0xFF080008);
        g.fill(x + 1, y + 1, x + GUI_W - 1, y + GUI_H - 1, 0xFF0F000F);
        g.fill(x, y, x + GUI_W, y + 12, 0xFF15002A);
        g.fill(x, y + 11, x + GUI_W, y + 12, 0xFF5C2E7A);
        g.drawString(font, "§5Recipe Builder", x + 5, y + 2, 0xFFFFFF, false);
        g.fill(x, y + 12, x + GUI_W, y + 29, 0xFF0A000A);
        g.fill(x, y + 28, x + GUI_W, y + 29, 0xFF2E1A3A);
        g.fill(x + 4, y + 29, x + GUI_W - 4, y + 150, 0xFF080008);
        drawBorder(g, x + 4, y + 29, x + GUI_W - 4, y + 150, 0xFF3A1A5A);
        int divY = y + RecipeBuilderMenu.INV_Y - 8;
        g.fill(x + 4, divY, x + GUI_W - 4, divY + 1, 0xFF2E1A3A);
        int invX = x + RecipeBuilderMenu.INV_X;
        int invY = y + RecipeBuilderMenu.INV_Y;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlot(g, invX + col * 18, invY + row * 18);
        int hbY = invY + RecipeBuilderMenu.HOTBAR_OFFSET;
        g.fill(invX, hbY - 3, invX + 9 * 18, hbY - 2, 0xFF2E1A3A);
        for (int col = 0; col < 9; col++)
            drawSlot(g, invX + col * 18, hbY);
    }

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF1A0A2A);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFF5C2E7A);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A1A3A);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (amountEditor.visible) return amountEditor.mouseClicked(mx, my, btn);

        if (my < topPos + RecipeBuilderMenu.INV_Y - 8) {
            if (currentPage == Page.ITEMS) {
                if (itemInputPanel.mouseClicked(mx, my, btn)) return true;
                if (itemOutputPanel.mouseClicked(mx, my, btn)) return true;
            }
            if (currentPage == Page.FLUIDS) {
                if (fluidInputPanel.mouseClicked(mx, my, btn)) return true;
                if (fluidOutputPanel.mouseClicked(mx, my, btn)) return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (my < topPos + RecipeBuilderMenu.INV_Y - 8) {
            if (itemInputPanel.mouseReleased(mx, my, btn)) return true;
            if (itemOutputPanel.mouseReleased(mx, my, btn)) return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (amountEditor.visible) return amountEditor.keyPressed(key, scan, mod);
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char code, int mod) {
        if (amountEditor.visible) return amountEditor.charTyped(code, mod);
        return super.charTyped(code, mod);
    }

    @Override
    public void onClose() {
        RecipeBuilderState.INSTANCE.save(this);
        super.onClose();
    }

    public void openEditor(double mx, double my, Object target, int amt) {
        amountEditor.open((int) mx, (int) my, target, amt, () -> {});
    }

    private void onCopyKjs() {
        assert minecraft != null;
        minecraft.keyboardHandler.setClipboard(buildKjs());
        notify("§aCopied KJS!");
    }

    private void onCopyJava() {
        assert minecraft != null;
        minecraft.keyboardHandler.setClipboard(buildJava());
        notify("§aCopied Java datagen!");
    }

    private void onSendToServer() {
        String java = buildJava();
        PhoenixNetwork.CHANNEL.sendToServer(new PacketRecipeBuilderGenerate(java));
        assert minecraft != null;
        minecraft.keyboardHandler.setClipboard(java);
        notify("§aSent to server + copied Java!");
    }

    private void onClear() {
        recipeIdBox.setValue("");
        durationBox.setValue("");
        eutBox.setValue("");
        sourceInBox.setValue("");
        sourceOutBox.setValue("");
        recipeTypeDropdown.setSelectedIdx(0);
        itemInputPanel.clear();
        itemOutputPanel.clear();
        fluidInputPanel.clear();
        fluidOutputPanel.clear();
        conditions.clear();
        RecipeBuilderState.INSTANCE.clear();
    }

    private void notify(String msg) {
        assert minecraft != null;
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(Component.literal(msg), false);
    }

    private String buildKjs() {
        String machine = toKjsMachine(recipeTypeDropdown.getSelected());
        String id = fb(recipeIdBox.getValue(), "my_recipe");
        String dur = fb(durationBox.getValue(), "200");
        String eut = fb(eutBox.getValue(), "GTValues.VA[GTValues.LV]");
        int srcIn = parseInt(sourceInBox.getValue());
        int srcOut = parseInt(sourceOutBox.getValue());

        StringBuilder sb = new StringBuilder();
        sb.append("event.recipes.gtceu.").append(machine).append("(\"").append(id).append("\")\n");

        List<String> iIn = new ArrayList<>();
        for (SlotPanel.SlotEntry e : itemInputPanel.getEntries()) {
            if (e.stack == null || e.stack.isEmpty()) continue;
            String reg = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toString();
            iIn.add("\"" + (e.count > 1 ? e.count + "x " : "") + reg + "\"");
        }
        if (!iIn.isEmpty()) sb.append("    .itemInputs(").append(String.join(", ", iIn)).append(")\n");

        List<String> fIn = new ArrayList<>();
        for (FluidSlotPanel.FluidEntry e : fluidInputPanel.getEntries()) {
            if (e.fluidExpr == null || e.fluidExpr.isBlank()) continue;
            fIn.add("\"" + e.fluidExpr + " " + e.amount + "\"");
        }
        if (!fIn.isEmpty()) sb.append("    .inputFluids(").append(String.join(", ", fIn)).append(")\n");

        List<String> iOut = new ArrayList<>();
        for (SlotPanel.SlotEntry e : itemOutputPanel.getEntries()) {
            if (e.stack == null || e.stack.isEmpty()) continue;
            String reg = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toString();
            iOut.add("\"" + (e.count > 1 ? e.count + "x " : "") + reg + "\"");
        }
        if (!iOut.isEmpty()) sb.append("    .itemOutputs(").append(String.join(", ", iOut)).append(")\n");

        List<String> fOut = new ArrayList<>();
        for (FluidSlotPanel.FluidEntry e : fluidOutputPanel.getEntries()) {
            if (e.fluidExpr == null || e.fluidExpr.isBlank()) continue;
            fOut.add("\"" + e.fluidExpr + " " + e.amount + "\"");
        }
        if (!fOut.isEmpty()) sb.append("    .outputFluids(").append(String.join(", ", fOut)).append(")\n");

        if (srcIn > 0) sb.append("    // Source in: ").append(srcIn).append("\n");
        if (srcOut > 0) sb.append("    // Source out: ").append(srcOut).append("\n");

        sb.append("    .duration(").append(dur).append(")\n");
        sb.append("    .EUt(").append(eut).append(")");

        for (ConditionsScreen.ConditionEntry e : conditions)
            sb.append("\n").append(e.toKjsCode());

        return sb.toString();
    }

    private static String toKjsMachine(String type) {
        String n = type;
        for (String suf : new String[] { "_RECIPES", "_FUELS" }) {
            if (n.endsWith(suf)) {
                n = n.substring(0, n.length() - suf.length());
                break;
            }
        }
        return n.toLowerCase();
    }

    private String buildJava() {
        String type = recipeTypeDropdown.getSelectedCallPrefix();
        String id = fb(recipeIdBox.getValue(), "my_recipe");
        String dur = fb(durationBox.getValue(), "200");
        String eut = fb(eutBox.getValue(), "VA[LV]");
        int srcIn = parseInt(sourceInBox.getValue());
        int srcOut = parseInt(sourceOutBox.getValue());

        StringBuilder sb = new StringBuilder();
        sb.append(type).append(".recipeBuilder(\"").append(id).append("\")\n");

        for (SlotPanel.SlotEntry e : itemInputPanel.getEntries()) {
            if (e.stack == null || e.stack.isEmpty()) continue;
            String reg = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toString();
            if (e.notConsumable)
                sb.append("        .notConsumable(ForgeRegistries.ITEMS.getValue(new ResourceLocation(\"").append(reg)
                        .append("\")))\n");
            else
                sb.append("        .inputItems(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(\"")
                        .append(reg).append("\")), ").append(e.count).append("))\n");
        }
        for (SlotPanel.SlotEntry e : itemOutputPanel.getEntries()) {
            if (e.stack == null || e.stack.isEmpty()) continue;
            String reg = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toString();
            sb.append("        .outputItems(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(\"")
                    .append(reg).append("\")), ").append(e.count).append("))\n");
        }
        for (FluidSlotPanel.FluidEntry e : fluidInputPanel.getEntries()) {
            if (e.fluidExpr == null || e.fluidExpr.isBlank()) continue;
            sb.append("        .inputFluids(").append(e.fluidExpr).append(".getFluid(").append(e.amount).append("))\n");
        }
        for (FluidSlotPanel.FluidEntry e : fluidOutputPanel.getEntries()) {
            if (e.fluidExpr == null || e.fluidExpr.isBlank()) continue;
            sb.append("        .outputFluids(").append(e.fluidExpr).append(".getFluid(").append(e.amount)
                    .append("))\n");
        }
        if (srcIn > 0)
            sb.append("        .input(SourceRecipeCapability.CAP, new SourceIngredient(").append(srcIn).append("))\n");
        if (srcOut > 0) sb.append("        .output(SourceRecipeCapability.CAP, new SourceIngredient(").append(srcOut)
                .append("))\n");
        sb.append("        .duration(").append(dur).append(")\n");
        sb.append("        .EUt(").append(eut).append(")\n");
        for (ConditionsScreen.ConditionEntry e : conditions) sb.append(e.toJavaCode());
        sb.append("        .save(provider);");
        return sb.toString();
    }

    private void drawLabel(GuiGraphics g, String t, int x, int y) {
        g.drawString(font, t, x, y, 0x886688, false);
    }

    private void drawBorder(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        g.fill(x0, y0, x1, y0 + 1, col);
        g.fill(x0, y1 - 1, x1, y1, col);
        g.fill(x0, y0, x0 + 1, y1, col);
        g.fill(x1 - 1, y0, x1, y1, col);
    }

    private EditBox box(int x, int y, int w, String hint, int maxLen) {
        EditBox b = new EditBox(font, x, y, w, 12, Component.empty());
        b.setHint(Component.literal(hint));
        b.setMaxLength(maxLen);
        addRenderableWidget(b);
        return b;
    }

    private static String fb(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.trim();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatAmount(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0).replace(".0M", "M");
        if (n >= 1_000) return String.format("%.1fk", n / 1_000.0).replace(".0k", "k");
        return String.valueOf(n);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public net.minecraft.client.gui.Font getFont() {
        return font;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> @NotNull T addRenderableWidget(@NotNull T w) {
        return super.addRenderableWidget(w);
    }
}
