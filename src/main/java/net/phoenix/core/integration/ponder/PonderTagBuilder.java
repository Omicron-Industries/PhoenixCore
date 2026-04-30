package net.phoenix.core.integration.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ponder.util.PonderPlatform;
import net.phoenix.core.integration.ponder.util.TagRegistryInternalAccess;

import com.google.common.collect.Multimap;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class PonderTagBuilder {

    public void createTag(String id, ItemStack displayItem, String title, String description,
                          @Nullable Ingredient ingredient) {
        PhoenixCore.getPonderTagByName(id).ifPresent(tag -> {
            throw new IllegalArgumentException("Tag " + id + " already exists");
        });

        ResourceLocation idWithNamespace = PhoenixCore.appendPonderJSNamespaceToId(id);
        PonderTag ponderTag = new PonderTag(
                idWithNamespace,           // ResourceLocation id
                null,                      // @Nullable ResourceLocation textureIconLocation
                new ItemStack(displayItem.getItem()), // ItemStack itemIcon
                new ItemStack(displayItem.getItem())  // ItemStack mainItem
        );

        // Tags are registered via the registration event helper, not listTag().
        // Store the tag for use during the registration event.
        PhoenixCore.PENDING_TAGS.add(ponderTag);

        if (ingredient != null) {
            add(ponderTag, ingredient);
        }
        PhoenixCore.PONDER_NAMESPACES.add(idWithNamespace.getNamespace());
    }

    public void createTag(String id, ItemStack displayItem, String title, String description) {
        createTag(id, displayItem, title, description, null);
    }

    public void removeTag(PonderTag... tags) {
        for (PonderTag tag : tags) {
            Set<ResourceLocation> items = PonderIndex.getTagAccess().getItems(tag);
            // getListedTags() returns a List — remove is supported
            PonderIndex.getTagAccess().getListedTags().remove(tag);
            remove(tag, items);
        }
    }

    public void add(PonderTag tag, Ingredient ingredient) {
        if (ingredient.isEmpty()) return;
        Multimap<ResourceLocation, ResourceLocation> componentTagMap = ((TagRegistryInternalAccess) PonderIndex
                .getTagAccess()).ponderjs$getComponentTagMap();
        for (ItemStack stack : ingredient.getItems()) {
            ResourceLocation itemId = PonderPlatform.getItemName(stack.getItem());
            componentTagMap.put(itemId, tag.getId());
        }
    }

    public void remove(PonderTag tag, Ingredient ingredient) {
        if (ingredient.isEmpty()) return;
        Set<ResourceLocation> ids = Arrays.stream(ingredient.getItems())
                .map(ItemStack::getItem)
                .map(PonderPlatform::getItemName)
                .collect(Collectors.toSet());
        remove(tag, ids);
    }

    private void remove(PonderTag tag, Set<ResourceLocation> items) {
        Multimap<ResourceLocation, ResourceLocation> componentTagMap = ((TagRegistryInternalAccess) PonderIndex
                .getTagAccess()).ponderjs$getComponentTagMap();
        for (ResourceLocation item : items) {
            if (componentTagMap.remove(item, tag.getId())) {
                PhoenixCore.LOGGER.info("Removed ponder tag " + tag.getId() + " from item " + item);
            }
        }
    }

    public void register() {}
}
