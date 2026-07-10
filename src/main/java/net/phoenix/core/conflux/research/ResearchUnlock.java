package net.phoenix.core.conflux.research;

import com.google.gson.JsonObject;

/**
 * Something granted when a research node is completed.
 *
 * Types:
 *   "recipe_tag"  — unlocks all recipes tagged with {@code value} (checked by AxiomRecipeCondition)
 *   "flag"        — sets a named boolean flag queryable via PlayerResearchData.hasFlag()
 */
public record ResearchUnlock(String type, String value) {

    public static ResearchUnlock fromJson(JsonObject obj) {
        return new ResearchUnlock(
                obj.get("type").getAsString(),
                obj.get("value").getAsString()
        );
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.addProperty("value", value);
        return obj;
    }
}
