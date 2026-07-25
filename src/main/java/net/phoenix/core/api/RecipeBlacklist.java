package net.phoenix.core.api;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RecipeBlacklist {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Set<ResourceLocation> BY_ID = new HashSet<>();
    public static final Set<String> BY_MOD = new HashSet<>();
    public static final Set<String> BY_OUTPUT = new HashSet<>();
    public static final Set<String> BY_INPUT = new HashSet<>();
    private static final Path REMOVALS_DIR = FMLPaths.CONFIGDIR.get().resolve("phoenix_core/removals");

    public static void load() {
        try {

            if (!Files.exists(REMOVALS_DIR)) {
                Files.createDirectories(REMOVALS_DIR);
            }

            generateExamplesIfEmpty();

            BY_ID.clear();
            BY_MOD.clear();
            BY_OUTPUT.clear();
            BY_INPUT.clear();

            try (Stream<Path> paths = Files.list(REMOVALS_DIR)) {
                paths.filter(path -> path.toString().endsWith(".json"))
                        .forEach(RecipeBlacklist::loadSingleFile);
            }

        } catch (Exception e) {
            System.err.println("Failed to load Phoenix Core removal folder: " + e.getMessage());
        }
    }

    public static boolean shouldRemoveParsed(ResourceLocation id, Recipe<?> recipe) {
        if (BY_ID.contains(id) || BY_MOD.contains(id.getNamespace())) return true;

        String outputId = BuiltInRegistries.ITEM.getKey(recipe.getResultItem(RegistryAccess.EMPTY).getItem())
                .toString();
        if (BY_OUTPUT.contains(outputId)) return true;

        for (Ingredient ingredient : recipe.getIngredients()) {
            for (ItemStack stack : ingredient.getItems()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (BY_INPUT.contains(itemId)) return true;
            }
        }
        return false;
    }

    private static void loadSingleFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            RecipeConfig fileConfig = GSON.fromJson(reader, RecipeConfig.class);
            if (fileConfig != null) {

                fileConfig.remove_by_id.forEach(s -> BY_ID.add(new ResourceLocation(s)));
                BY_MOD.addAll(fileConfig.remove_by_mod);
                BY_OUTPUT.addAll(fileConfig.remove_by_output);
                BY_INPUT.addAll(fileConfig.remove_by_input);
            }
        } catch (Exception e) {
            System.err.println("Error reading removal file " + path.getFileName() + ": " + e.getMessage());
        }
    }

    public static boolean shouldRemoveRaw(ResourceLocation id, JsonElement json) {
        if (BY_ID.contains(id) || BY_MOD.contains(id.getNamespace())) return true;

        if (!json.isJsonObject()) return false;
        JsonObject obj = json.getAsJsonObject();

        if (obj.has("result")) {
            JsonElement result = obj.get("result");
            String resStr = "";
            if (result.isJsonPrimitive()) resStr = result.getAsString();
            else if (result.isJsonObject() && result.getAsJsonObject().has("item"))
                resStr = result.getAsJsonObject().get("item").getAsString();

            if (BY_OUTPUT.contains(resStr)) return true;
        }

        String rawJson = obj.toString();
        for (String inputItem : BY_INPUT) {
            if (rawJson.contains("\"item\":\"" + inputItem + "\"") ||
                    rawJson.contains("\"item\": \"" + inputItem + "\""))
                return true;
        }

        return false;
    }

    private static void generateExamplesIfEmpty() throws Exception {
        try (Stream<Path> stream = Files.list(REMOVALS_DIR)) {
            if (stream.findAny().isEmpty()) {

                RecipeConfig vanillaExample = new RecipeConfig();
                vanillaExample.remove_by_id.add("minecraft:example_recipe_id");
                Files.writeString(REMOVALS_DIR.resolve("vanilla_tweaks.json"), GSON.toJson(vanillaExample));

                RecipeConfig modExample = new RecipeConfig();
                modExample.remove_by_mod.add("example_mod_id");
                modExample.remove_by_output.add("example_mod:broken_item");
                Files.writeString(REMOVALS_DIR.resolve("mod_removals.json"), GSON.toJson(modExample));
            }
        }
    }
}
