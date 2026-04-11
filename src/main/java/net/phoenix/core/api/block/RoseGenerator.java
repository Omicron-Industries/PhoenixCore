package net.phoenix.core.api.block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RoseGenerator {

    // Using your exact provided path as the base
    private static final String BASE_PATH = "C:/Users/conno/Desktop/PFT Core/PhoenixCore/src/main/resources/assets/phoenixcore/";

    public static void main(String[] args) {
        List<String> materials = List.of(
                "amethyst", "apatite", "bauxite", "cinnabar", "cobalt", "cobaltite", "copper", "diamond",
                "electrotine", "emerald", "galena", "gold", "ilmenite", "invar", "iron", "lapis",
                "lead", "lepidolite", "malachite", "nickel", "opal", "pitchblende", "pyrope", "realgar",
                "ruby", "salt", "sapphire", "scheelite", "silicon", "silver", "steel", "stibnite", "topaz",
                "tricalcium_phosphate", "tungstate", "zinc", "barite", "bastnasite", "bismuth", "chromite",
                "graphite", "molybdenum", "oilsands", "platinum", "pyrochlore", "pyrolusite", "sphalerite",
                "sulfur", "tantalite", "tetrahedrite", "thorium", "titanium", "vanadium_magnetite",
                "nether_quartz", "rock_salt", "sodalite", "coal", "redstone", "tin", "obsidian",
                "netherite", "certus_quartz", "voidglass_shard", "saltpeter", "fluorite", "source_gem",
                "glowstone", "ice", "ignisium", "resonant_ender", "fluix", "sponge", "sculk", "slime",
                "magma", "blaze", "bone", "zombie", "withered", "ghostly", "silky", "prismarine"
        );

        try {
            Path bsDir = Paths.get(BASE_PATH + "blockstates/");
            Path itemDir = Paths.get(BASE_PATH + "models/item/");

            // Create directories if they don't exist
            Files.createDirectories(bsDir);
            Files.createDirectories(itemDir);

            for (String mat : materials) {
                String name = mat.toLowerCase() + "_crystal_rose";

                // 1. Write Blockstate
                String bsJson = "{\"variants\":{\"\":{\"model\":\"phoenixcore:block/crystal_rose\"}}}";
                Files.writeString(bsDir.resolve(name + ".json"), bsJson);

                // 2. Write Item Model
                String itemJson = "{\"parent\":\"phoenixcore:block/crystal_rose\"}";
                Files.writeString(itemDir.resolve(name + ".json"), itemJson);
            }

            System.out.println("DONE! Generated " + materials.size() + " sets of JSON files.");
            System.out.println("Files written to: " + BASE_PATH);

        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not write to the path. Make sure IntelliJ has permission!");
            e.printStackTrace();
        }
    }
}