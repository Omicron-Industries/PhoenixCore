package net.phoenix.core.integration.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ponder.api.ExtendedPonderStoryBoard;
import net.phoenix.core.integration.ponder.api.ExtendedSceneBuilder;
import net.phoenix.core.integration.ponder.api.SceneBuildingUtilDelegate;
import net.phoenix.core.integration.ponder.util.PonderErrorHelper;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around {@link PonderSceneRegistrationHelper} that provides an
 * isolated, per-item scene registration API using the {@code phoenixcore} namespace.
 */
public record PonderBuilder(PonderSceneRegistrationHelper<ResourceLocation> helper) {

    /**
     * Returns a fresh {@link ForItemsBuilder} scoped exclusively to the given items.
     * The returned builder is ISOLATED — its item set is fixed at construction time
     * and never shared with other calls, preventing cross-machine scene contamination.
     */
    public ForItemsBuilder forItems(Ingredient ingredient) {
        Set<Item> scopedItems = new HashSet<>();
        if (!ingredient.isEmpty()) {
            Arrays.stream(ingredient.getItems())
                    .map(ItemStack::getItem)
                    .forEach(scopedItems::add);
        }
        return new ForItemsBuilder(helper, scopedItems);
    }

    /**
     * Scoped builder whose item set is fixed at construction time.
     * Each call to {@link PonderBuilder#forItems} creates a new independent instance.
     */
    public static class ForItemsBuilder {

        private final PonderSceneRegistrationHelper<ResourceLocation> helper;
        private final Set<Item> items;

        ForItemsBuilder(PonderSceneRegistrationHelper<ResourceLocation> helper, Set<Item> items) {
            this.helper = helper;
            this.items = items;
        }

        /**
         * Convenience overload — uses the default {@code phoenixcore:blank_48} structure.
         *
         * @param name  bare scene path (e.g. {@code "gregtech_multiblocks/electric_blast_furnace"})
         *              — the {@code phoenixcore} namespace is prepended automatically.
         * @param title human-readable title registered via {@code registerSharedText}.
         * @param scene storyboard lambda.
         */
        public ForItemsBuilder scene(String name, String title, ExtendedPonderStoryBoard scene) {
            return scene(name, title, "phoenixcore:blank_48", scene);
        }

        /**
         * Registers a Ponder scene for every item in this builder's item set.
         *
         * @param name          bare scene path (e.g. {@code "gregtech_multiblocks/electric_blast_furnace"})
         *                      — resolved to {@code phoenixcore:<name>} via {@link PhoenixCore#ponderIdOf}.
         * @param title         human-readable title registered via {@code registerSharedText}.
         * @param structureName full namespaced schematic id string (e.g. {@code "phoenixcore:blank_48"}).
         *                      Passed directly as a {@link ResourceLocation}.
         * @param scene         storyboard lambda.
         */
        public ForItemsBuilder scene(String name, String title, String structureName, ExtendedPonderStoryBoard scene) {
            // Resolves to "phoenixcore:<name>" (or passes through if already namespaced).
            ResourceLocation id = PhoenixCore.ponderIdOf(name);
            ResourceLocation structureId = new ResourceLocation(structureName);
            PonderStoryBoardWrapper wrapper = new PonderStoryBoardWrapper(scene);

            // Register the namespace so PhoenixPonderLang.createFromLocalization()
            // produces lang entries for it. Without this PONDER_NAMESPACES stays empty
            // and the lang file is always blank for auto-generated scenes.
            PhoenixCore.PONDER_NAMESPACES.add(id.getNamespace());

            for (var item : items) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId == null) continue;

                helper.addStoryBoard(itemId, structureId,
                        (builder, util) -> {
                            builder.title(id.getPath(), title);
                            wrapper.program(builder, util);
                        }, id);
            }
            return this;
        }

        /** No-op — registration happens eagerly inside {@link #scene}. */
        public void register() {}
    }

    /** No-op — registration happens eagerly inside {@link ForItemsBuilder#scene}. */
    public void register() {}

    // -------------------------------------------------------------------------

    public static class PonderStoryBoardWrapper implements PonderStoryBoard {

        private final ExtendedPonderStoryBoard storyBoard;

        protected PonderStoryBoardWrapper(ExtendedPonderStoryBoard storyBoard) {
            this.storyBoard = storyBoard;
        }

        @Override
        public void program(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil util) {
            try {
                ExtendedSceneBuilder extended = new ExtendedSceneBuilder(builder);
                storyBoard.program(extended, new SceneBuildingUtilDelegate(util));
            } catch (Exception e) {
                PonderErrorHelper.yeet(e);
            }
        }
    }
}