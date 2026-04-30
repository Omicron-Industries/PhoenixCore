package net.phoenix.core.integration.ponder;

import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

public class PhoenixPonderLang {

    // Path for generated lang files in PhoenixCore
    public static final String PATH = "phoenixcore/assets/ponder_generated/lang/%lang%.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * @param langName as String
     * @return true if a new lang file was created
     */
    public boolean generate(String langName) {
        File file = new File(PATH.replace("%lang%", langName));

        JsonObject existingLang = read(file);
        JsonObject currentLang = createFromLocalization();

        if (currentLang.equals(existingLang)) {
            return false;
        }

        PhoenixCore.LOGGER.info(
                "Phoenix Ponder - New lang file differ from existing lang file, generating new lang file.\n Old Lang size: {} \n\n New lang size: {}",
                existingLang == null ? 0 : existingLang.size(),
                currentLang.size());

        return write(file, currentLang);
    }

    private boolean write(File file, JsonObject currentLang) {
        try {
            String output = GSON.toJson(currentLang);
            FileUtils.writeStringToFile(file, output, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            PhoenixCore.LOGGER.error(e);
        }

        return false;
    }

    @Nullable
    protected JsonObject read(File file) {
        if (file.exists()) {
            try {
                String s = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                return GSON.fromJson(s, JsonObject.class);
            } catch (IOException e) {
                PhoenixCore.LOGGER.error(e);
            }
        }
        return null;
    }

    public JsonObject createFromLocalization() {
        PhoenixCore.PONDER_STORIES_MANAGER.compileLang();
        JsonObject object = new JsonObject();

        // Create or access the instance of PonderLocalization
        // Usually, Ponder provides a shared instance for recording
        PonderLocalization localization = new PonderLocalization();

        PhoenixCore.PONDER_NAMESPACES.forEach(namespace -> {
            // Now calling the method on the instance 'localization'
            localization.provideLang(namespace, object::addProperty);
        });

        return object;
    }
}
