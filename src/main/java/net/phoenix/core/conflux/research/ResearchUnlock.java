package net.phoenix.core.conflux.research;

import com.google.gson.JsonObject;

public record ResearchUnlock(String type, String value) {

    public static ResearchUnlock fromJson(JsonObject obj) {
        return new ResearchUnlock(
                obj.get("type").getAsString(),
                obj.get("value").getAsString());
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.addProperty("value", value);
        return obj;
    }
}
