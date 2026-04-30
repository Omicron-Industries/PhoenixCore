package net.phoenix.core.integration.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ponder.api.ExtendedPonderStoryBoard;
import net.phoenix.core.integration.ponder.api.ExtendedSceneBuilder;
import net.phoenix.core.integration.ponder.api.SceneBuildingUtilDelegate;
import net.phoenix.core.integration.ponder.util.PonderErrorHelper;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PonderBuilder {

    private final PonderSceneRegistrationHelper<ResourceLocation> helper;

    public PonderBuilder(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        this.helper = helper;
    }

    /**
     * Returns a fresh ForItemsBuilder scoped exclusively to the given items.
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

        public ForItemsBuilder scene(String name, String title, ExtendedPonderStoryBoard scene) {
            return scene(name, title, "phoenixcore:gregtech_multiblocks/blank_64", scene);
        }

        /**
         * @param name          scene id path (e.g. "gregtech_multiblocks/electric_blast_furnace")
         *                      — the phoenixcore namespace is prepended automatically.
         * @param title         human-readable title registered via registerSharedText.
         * @param structureName full namespaced schematic id string
         *                      (e.g. "phoenixcore:gregtech_multiblocks/blank_64").
         *                      Passed directly as a ResourceLocation — NOT run through
         *                      appendPonderJSNamespaceToId.
         * @param scene         storyboard lambda.
         */
        public ForItemsBuilder scene(String name, String title, String structureName, ExtendedPonderStoryBoard scene) {
            ResourceLocation id = PhoenixCore.appendPonderJSNamespaceToId(name);
            ResourceLocation structureId = new ResourceLocation(structureName);
            PonderStoryBoardWrapper wrapper = new PonderStoryBoardWrapper(scene);

            for (var item : items) {
                ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (itemId == null) continue;

                helper.addStoryBoard(itemId, structureId,
                        (builder, util) -> {
                            builder.title(id.getPath(), title);
                            wrapper.program(builder, util);
                        }, id);
            }
            return this;
        }

        public void register() {}
    }

    public void register() {}

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
