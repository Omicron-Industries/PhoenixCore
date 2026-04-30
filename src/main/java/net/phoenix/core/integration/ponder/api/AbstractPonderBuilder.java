package net.phoenix.core.integration.ponder.api;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.world.item.Item;
import net.phoenix.core.integration.ponder.util.TagRegistryInternalAccess;

import java.util.Set;

public abstract class AbstractPonderBuilder<S extends AbstractPonderBuilder<S>> {

    protected Set<Item> items;
    protected final PonderSceneRegistrationHelper<Item> helper;

    public AbstractPonderBuilder(Set<Item> items, PonderSceneRegistrationHelper<Item> helper) {
        this.items = items;
        this.helper = helper;
    }

    protected abstract S getSelf();

    public S tag(PonderTag... tags) {
        for (PonderTag tag : tags) {
            // Use mixin accessor to add items to the tag's component map
            TagRegistryInternalAccess access = (TagRegistryInternalAccess) PonderIndex.getTagAccess();
            for (Item item : items) {
                access.ponderjs$getComponentTagMap().put(
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item),
                        tag.getId());
            }
        }
        return getSelf();
    }
}
