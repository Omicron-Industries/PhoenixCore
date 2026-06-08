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

        // Ponder system
        provider.add("phoenixcore.ponder.hint", "View structure guide");

        // EBF
        provider.add("phoenixcore.ponder.ebf.title", "Electric Blast Furnace");
        provider.add("phoenixcore.ponder.ebf.step1_overview",
                "The Electric Blast Furnace smelts metals at extreme temperatures using heating coils.");
        provider.add("phoenixcore.ponder.ebf.step2_controller",
                "This is the controller block. Place it on the front-bottom of the structure.");
        provider.add("phoenixcore.ponder.ebf.step3_coils",
                "The two middle layers must be filled entirely with heating coil blocks.");
        provider.add("phoenixcore.ponder.ebf.step4_cupronickel",
                "Cupronickel coils provide the minimum temperature — enough for basic alloys.");
        provider.add("phoenixcore.ponder.ebf.step5_kanthal",
                "Kanthal coils raise the temperature cap, unlocking higher-tier recipes.");
        provider.add("phoenixcore.ponder.ebf.step6_hatches",
                "Place at least one Input Bus, Output Bus, Energy Hatch, and Muffler Hatch on the outer casing.");

        // Vacuum Freezer
        provider.add("phoenixcore.ponder.freezer.title", "Vacuum Freezer");
        provider.add("phoenixcore.ponder.freezer.step1_overview",
                "The Vacuum Freezer cools materials to extremely low temperatures.");
        provider.add("phoenixcore.ponder.freezer.step2_hatches",
                "Place your hatches on the outer casing — any face except the controller.");

        // Large Chemical Reactor
        provider.add("phoenixcore.ponder.lcr.title", "Large Chemical Reactor");
        provider.add("phoenixcore.ponder.lcr.step1_overview",
                "The Large Chemical Reactor processes chemical recipes with higher efficiency than its single-block counterpart.");
        provider.add("phoenixcore.ponder.lcr.step2_pipe", "The centre of the middle layer must be a PTFE pipe casing.");
        provider.add("phoenixcore.ponder.lcr.step3_coil",
                "Exactly one heating coil must be placed somewhere in the cross layer.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_0", "Coil position variant 1 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_1", "Coil position variant 2 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_2", "Coil position variant 3 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_3", "Coil position variant 4 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_4", "Coil position variant 5 of 5.");
        provider.add("phoenixcore.ponder.lcr.step5_hatches",
                "Hatches go on the outer casing faces. You need energy, item, and fluid IO.");

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
        provider.add("config.jade.plugin_phoenixcore.phantasia_jade", "Phantasia Info");

        // Specialized Tank Tooltips
        provider.add("jade.phoenixcore.source_tank_header", "Source Tank Content");
        provider.add("jade.phoenixcore.source_tank_format", "Capacity %s / %s - %d%%");
        provider.add("config.jade.plugin_phoenixcore.source_tank_info", "Source Tank Info");

        // Keybinds
        provider.add("key.categories.phoenixcore", "PhoenixCore");
        provider.add("key.phoenixcore.wing_flight_gui", "Wing Flight Settings");
        provider.add("key.phoenixcore.tesla_mode", "Enable Tesla Mode");
        provider.add("key.phoenixcore.tesla_discharge", "Activate Tesla Discharge");
        provider.add("key.phoenixcore.manipulator_menu", "Matter Manipulator Menu");
        provider.add("key.phoenixcore.open_ponder", "View Structure Guide");
        provider.add("key.phoenixcore.phantasia_menu", "Open Phantasia Menu");
        provider.add("key.phoenixcore.recipe_builder", "Open Recipe Builder");
        provider.add("key.phoenixcore.spray_can_menu", "Open Spray Can Menu");

        // Chameleon Spray Can & Materials
        provider.add("creativetab.phoenix_creative_tab", "Phoenix Core");
        provider.add("fluid.phoenixcore.prismatic_paint", "Prismatic Paint"); // Registers the material fluid name
        provider.add("gui.phoenixcore.color_select.title", "Select Color");
        provider.add("behaviour.paintspray.chameleon.status.color", "§7Mode: §f%s");
        provider.add("behaviour.paintspray.chameleon.status.solvent", "§7Mode: §dSolvent");
        provider.add("behaviour.paintspray.chameleon.tooltip.current_color", "Current Color: %s");
        provider.add("behaviour.paintspray.chameleon.tooltip.solvent", "Current: Solvent");
        provider.add("behaviour.paintspray.chameleon.tooltip.info", "Scroll or use the Keybind to change color.");
        provider.add("behaviour.paintspray.chameleon.tooltip.fluid", "§7Paint: %s / %s mB"); // Formats: current / max
                                                                                             // fluid units
        provider.add("behaviour.paintspray.chameleon.message.out_of_paint", "§cOut of Prismatic Paint!");
        provider.add("behaviour.paintspray.solvent.short", "Solvent");

        // Tesla Binder Item
        provider.add("item.phoenixcore.tesla_binder.linked", "§aLinked to: §f%s");
        provider.add("item.phoenixcore.tesla_binder.unlinked", "§cNot Linked");
        provider.add("item.phoenixcore.tesla_binder.frequency", "§7Frequency: §b%s");

        provider.add("item.phoenixcore.chameleon_spray_can.with_color", "%s (%s)");

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
