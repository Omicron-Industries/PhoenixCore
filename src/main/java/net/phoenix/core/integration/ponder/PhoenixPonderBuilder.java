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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PhoenixPonderBuilder {

    private final PonderSceneRegistrationHelper<Item> helper;
    private final Set<Item> items = new HashSet<>();

    public PhoenixPonderBuilder(PonderSceneRegistrationHelper<Item> helper) {
        this.helper = helper;
    }

    public PhoenixPonderBuilder forItems(Ingredient ingredient) {
        if (!ingredient.isEmpty()) {
            this.items.addAll(Arrays.stream(ingredient.getItems())
                    .map(ItemStack::getItem)
                    .collect(Collectors.toSet()));
        }
        return this;
    }

    public PhoenixPonderBuilder scene(String name, String title, ExtendedPonderStoryBoard scene) {
        return scene(name, title, "ponderjs:basic", scene);
    }

    public PhoenixPonderBuilder scene(String name, String title, String structureName, ExtendedPonderStoryBoard scene) {
        ResourceLocation id = PhoenixCore.appendPonderJSNamespaceToId(name);
        PonderStoryBoardWrapper wrapper = new PonderStoryBoardWrapper(scene);
        for (var item : items) {
            helper.addStoryBoard(item, PhoenixCore.appendPonderJSNamespaceToId(structureName),
                    new PonderStoryBoard() {

                        @Override
                        public void program(SceneBuilder builder, SceneBuildingUtil util) {
                            builder.title(id.getPath(), title);
                            wrapper.program(builder, util);
                        }
                    }, id);
            PhoenixCore.PONDER_NAMESPACES.add(id.getNamespace());
        }
        return this;
    }

    public void register() {}

    public static class PonderStoryBoardWrapper implements PonderStoryBoard {

        private final ExtendedPonderStoryBoard storyBoard;

        protected PonderStoryBoardWrapper(ExtendedPonderStoryBoard storyBoard) {
            this.storyBoard = storyBoard;
        }

        @Override
        public void program(SceneBuilder builder, SceneBuildingUtil util) {
            try {
                // Pass builder directly as delegate — NOT extracting PonderScene
                ExtendedSceneBuilder extended = new ExtendedSceneBuilder(builder);
                storyBoard.program(extended, new SceneBuildingUtilDelegate(util));
            } catch (Exception e) {
                PonderErrorHelper.yeet(e);
            }
        }
    }
}
