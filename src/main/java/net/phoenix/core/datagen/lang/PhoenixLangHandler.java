package net.phoenix.core.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixLangHandler {

    public static void init(RegistrateLangProvider provider) {
        // Tool Renaming & General Items
        provider.add("metaarmor.message.step_assist.disabled", "PhoenixTech™ Suite: StepAssist Disabled");
        provider.add("metaarmor.message.step_assist.enabled", "PhoenixTech™ Suite: StepAssist Enabled");
        provider.add("item.gtceu.tool.ev_screwdriver", "%s Electric Screwdriver (EV)");
        provider.add("item.gtceu.tool.mv_screwdriver", "%s Electric Screwdriver (MV)");
        provider.add("item.gtceu.tool.luv_screwdriver", "%s Electric Screwdriver (LuV)");
        provider.add("item.gtceu.tool.zpm_screwdriver", "%s Electric Screwdriver (ZPM)");
        provider.add("item.gtceu.tool.zpm_drill", "%s Drill (ZPM)");
        provider.add("item.gtceu.tool.luv_drill", "%s Drill (LuV)");
        provider.add("item.gtceu.tool.mv_wrench", "%s Wrench (MV)");
        provider.add("item.gtceu.tool.ev_wrench", "%s Wrench (EV)");
        provider.add("item.gtceu.tool.luv_wrench", "%s Wrench (LuV)");
        provider.add("item.gtceu.tool.zpm_wrench", "%s Wrench (ZPM)");
        provider.add("item.gtceu.tool.mv_buzzsaw", "%s Buzzsaw (MV)");
        provider.add("item.gtceu.tool.ev_buzzsaw", "%s Buzzsaw  (EV)");
        provider.add("item.gtceu.tool.luv_buzzsaw", "%s Buzzsaw  (LuV)");
        provider.add("item.gtceu.tool.zpm_buzzsaw", "%s Buzzsaw  (ZPM)");
        provider.add("item.gtceu.tool.mv_chainsaw", "%s Buzzsaw  (MV)");
        provider.add("item.gtceu.tool.ev_chainsaw", "%s Buzzsaw  (EV)");
        provider.add("item.gtceu.tool.luv_chainsaw", "%s Buzzsaw  (LuV)");
        provider.add("item.gtceu.tool.zpm_chainsaw", "%s Buzzsaw  (ZPM)");
        provider.add("item.gtceu.tool.mv_wirecutter", "%s Wire Cutters  (MV)");
        provider.add("item.gtceu.tool.ev_wirecutter", "%s Wire Cutters  (EV)");
        provider.add("item.gtceu.tool.luv_wirecutter", "%s Wire Cutters (LuV)");
        provider.add("item.gtceu.tool.zpm_wirecutter", "%s Wire Cutters  (ZPM)");

        // Shield States
        provider.add("shield.phoenixcore.type.normal", "Normal");
        provider.add("shield.phoenixcore.type.inactive", "Inactive");
        provider.add("shield.phoenixcore.type.decayed", "Decayed");
        provider.add("shield.phoenixcore.current_shield", "Shield Status: %s");

        // Lore & General Tooltips
        provider.add("tooltip.phoenixcore.crystal_rose.generic", "A crystalline flower of immense power.");
        provider.add("tooltip.phoenixcore.crystal_rose.made_from", "Forged from %s.");
        provider.add("tooltip.phoenixcore.nanites.generic", "Microscopic machines swarming with potential.");
        provider.add("tooltip.phoenixcore.nanites.made_from", "Constructed from %s.");
        provider.add("metaarmor.tooltip.wings", "Contains Phoenix Wings");
        provider.add("metaarmor.tooltip.tesla_connection", "Controls Tesla Network Connection");

        // Jade Threaded Output Tooltips
        provider.add("gtceu.top.recipe_output", "Predicted Output:");
        provider.add("item.phoenixcore.jade.thread_header", "Alchemical Thread #%s: %s%%");
        provider.add("item.phoenixcore.jade.threads_active", "Active Alchemical Threads:");
        provider.add("gtceu.gui.content.range", "%s - %s");
        provider.add("gtceu.gui.content.times_item", "x %s");

        // General Source Machine Tooltips
        provider.add("jade.phoenixcore.source_container", "Source Energy");

        // Specialized Tank Tooltips
        provider.add("jade.phoenixcore.source_tank_header", "Source Tank Content");
        provider.add("jade.phoenixcore.source_tank_format", "Capacity %s / %s - %d%%");
        provider.add("config.jade.plugin_phoenixcore.source_tank_info", "Source Tank Info");

        // Keybinds

        provider.add("key.categories.phoenixcore", "PhoenixCore");
        provider.add("key.phoenixcore.wing_flight_gui", "Wing Flight Settings");
        provider.add("key.phoenixcore.tesla_mode", "Enable Tesla Mode");
        provider.add("key.phoenixcore.tesla_discharge", "Activate Tesla Discharge");

        // Tesla Binder Item
        provider.add("item.phoenixcore.tesla_binder.linked", "§aLinked to: §f%s");
        provider.add("item.phoenixcore.tesla_binder.unlinked", "§cNot Linked");
        provider.add("item.phoenixcore.tesla_binder.frequency", "§7Frequency: §b%s");

        // Placeholder/System Info
        multiLang(provider, "gtceu.placeholder_info.shieldStability",
                "Returns the stability of the shield.",
                "Note that not having a shield projected may result in nonsense values of integrity.",
                "Usage:",
                "  {shieldStability} -> shield integrity: (integrity, in percent)");
    }

    public static void multiLang(RegistrateLangProvider provider, String key, String... values) {
        for (var i = 0; i < values.length; i++) {
            provider.add(getSubKey(key, i), values[i]);
        }
    }

    protected static void multilineLang(RegistrateLangProvider provider, String key, String multiline) {
        var lines = multiline.split("\n");
        multiLang(provider, key, lines);
    }

    protected static String getSubKey(String key, int index) {
        return key + "." + index;
    }
}
