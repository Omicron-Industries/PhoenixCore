package net.phoenix.core.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixMachineLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add("phoenixcore.soul_lens.tooltip.flavor", "The Veil is thinner than you realize.");
        provider.add("phoenixcore.soul_lens.tooltip.1", "Your way of checking on the Soul of the World.");
        provider.add("gtceu.bio_engine", "Bio Aetheric Engine");

        provider.add("block.phoenixcore.fission_blanket.info_header", "Breeder Blanket Specifications:");
        provider.add("phoenixcore.blanket.input", "Breeding Target Input");

        provider.add("phoenixcore.tooltip.amount", "Yield Batch Size");
        provider.add("phoenixcore.tooltip.required_fuel_tier", "Required Driver Fuel");

        provider.add("emi_info.phoenixcore.required_shield", "Required Shield: %s");
        provider.add("emi_info.phoenixcore.shield_heal", "Shield Health Restored: +%s");
        provider.add("emi_info.phoenixcore.shield_damage", "Shield Damage Applied: -%s");

        provider.add("tooltip.phoenixcore.tesla_hatch.laser_input",
                "§bOptical Collimator§r: Concentrates energy into a coherent Tesla-Laser beam.");
        provider.add("tooltip.phoenixcore.tesla_hatch.laser_output",
                "§bPhotonic Receptor§r: Decodes high-frequency laser flux back into EU.");
        provider.add("tooltip.phoenixcore.tesla_hatch.input",
                "§bWireless Transmitter§r: Siphons energy into the Tesla Cloud.");
        provider.add("tooltip.phoenixcore.tesla_hatch.output",
                "§bWireless Receiver§r: Broadcasts energy from the Tesla Cloud.");
        provider.add("tooltip.phoenixcore.tesla_hatch.lore", "§6Nevvonian Core Tech: Frequency Locked.");

        provider.add("tech.phoenixcore.laser.input.low", "Tesla Optical Collimator");
        provider.add("tech.phoenixcore.laser.input.mid", "Tesla Optical Collimation Grid");
        provider.add("tech.phoenixcore.laser.input.high", "Tesla Phased Beam Matrix");

        provider.add("tech.phoenixcore.laser.output.low", "Tesla Photonic Coalescer");
        provider.add("tech.phoenixcore.laser.output.mid", "Tesla Photonic Coalescence Array");
        provider.add("tech.phoenixcore.laser.output.high", "Tesla Photonic Coalescence Matrix");

        provider.add("gui.phoenixcore.heat_exchanger.heat_exchange_surface", "Exchange Columns: %d");
        provider.add("gui.phoenixcore.heat_exchanger.current_efficiency", "Thermal Conductivity: Tier %d");
        provider.add("gui.phoenixcore.missing_spring", "Missing Heat Exchange Spring!");

        provider.add("gui.phoenixcore.source_hatch.label.import", "Source Input Hatch");
        provider.add("gui.phoenixcore.source_hatch.label.export", "Source Output Hatch");
        provider.add("gui.phoenixcore.source_hatch.source", "Source Stored: %s");
        provider.add("phoenix.core.recipe.source_in", "Source Consumed: %s.");
        provider.add("phoenix.core.recipe.source_out", "Source Yield: %s.");
        provider.add("tooltip.phoenixcore.source_hatch.consumption", "§cMax Source Consumption:§d %s");
        provider.add("tooltip.phoenixcore.source_hatch.capacity", "§cMax Source Capacity:§d %s");
        provider.add("recipe.capability.source.name", "Source");

        provider.add("tooltip.phoenix.empty_component.0", "This is an empty component, provides no stats.");
        provider.add("tooltip.phoenix.empty_component.1", "Useful for filling out a Fission Reactor.");
        provider.add("phoenixcore.not_formed", "Structure not formed!");
        provider.add("phoenixcore.status.safe_idle", "Status: §aIDLE");
        provider.add("phoenixcore.status.safe_working", "Status: §6ACTIVE");
        provider.add("phoenixcore.status.danger_timer", "§cCRITICAL: Meltdown in %s seconds!");
        provider.add("phoenixcore.status.no_coolant", "§eWARNING: Coolant Supply Exhausted");
        provider.add("phoenixcore.nuke_radius", "Blast area: %s");

        provider.add("phoenixcore:u242_fuel_pellet", "U-242 Fuel Pellet");
        provider.add("phoenixcore:thorium_fuel_pellet", "Thorium Fuel Pellet");
        provider.add("phoenixcore:critical_steam", "Supercritical Steam");
        provider.add("phoenixcore:hot_sodium_potassium", "Hot Sodium-Potassium");

        provider.add("phoenixcore.blanket.potential_outputs", "Potential Transmutations:");
        provider.add("phoenixcore.blanket.bias_hint",
                "§d§oHigher instability yields are favored by a Fast Neutron Spectrum (High Heat/Bias).");
        provider.add("phoenixcore.blanket_outputs", "§7Possible Products:");
        provider.add("phoenixcore.blanket_input", "§7Target Material: §f%s");
        provider.add("phoenixcore.blanket_output", "§7Breeding Product: §f%s");
        provider.add("phoenixcore.blanket_desc", "Irradiate target materials to produce specialized isotopes.");
        provider.add("phoenixcore.blanket_cycle", "Transmutes §f%s§7 units every §6%s§7 seconds");

        provider.add("phoenixcore.neutron_bias", "§7Neutron Bias: §f%s");
        provider.add("phoenixcore.spectrum_shift", "§7Spectrum Shift: §f%s");
        provider.add("phoenixcore.current_heat", "Core Temperature: %s HU");
        provider.add("phoenixcore.net_heat", "Net Heat Change: %s HU/t");
        provider.add("phoenixcore.heat_production", "Heat Production: %s");
        provider.add("phoenixcore.eu_generation", "Output: %s EU/t");
        provider.add("phoenixcore.parallels", "Parallel Processing: %sx");

        provider.add("phoenixcore.moderator", "Primary Moderator: %s");
        provider.add("phoenixcore.moderator_fuel_discount", "Fuel Efficiency: +%s%%");
        provider.add("phoenixcore.cooler", "Primary Cooling: %s");
        provider.add("phoenixcore.coolant", "Coolant: %s");
        provider.add("phoenixcore.coolant_rate", "Coolant Flow: %s mb/t");
        provider.add("phoenixcore.coolant_output", "Hot Coolant Produced: %s");
        provider.add("phoenixcore.coolant_status.ok", "§aCoolant Supply OK");
        provider.add("phoenixcore.coolant_status.empty", "§cCoolant Supply Depleted");
        provider.add("phoenixcore.summary", "Cooling: %s / %s HU/t");
        provider.add("phoenixcore.cooling_power", "§bCooling Capacity: §f%s HU/t");

        provider.add("phoenixcore.fuel_cycle", "Consumes §f%s§7 units every §6%s§7 seconds");
        provider.add("phoenixcore.depleted_fuel", "§7Depleted Fuel: §f%s");
        provider.add("phoenixcore.fuel_usage", "Fuel Consumption: §f%s");
        provider.add("phoenixcore.fuel_required", "§7Requires Fuel: §f%s");
        provider.add("phoenixcore.coolant_required", "§3Required Coolant: §f%s");

        provider.add("block.phoenixcore.fission_cooler.capacity", "§bCooling Capacity: §f%s HU/t");
        provider.add("block.phoenixcore.fission_cooler.required_coolant", "§3Required Coolant: §f%s");
        provider.add("block.phoenixcore.fission_moderator.multiplier", "§6Heat Multiplier: §f%sx");
        provider.add("block.phoenixcore.fission_moderator.parallel", "§aParallel Bonus: §f+%s");
        provider.add("block.phoenixcore.fission_moderator.shift", "Hold Shift for details");
        provider.add("block.phoenixcore.fission_moderator.info_header", "Fission Moderator");
        provider.add("block.phoenixcore.fission_moderator.boost", "EU Boost: %s");
        provider.add("block.phoenixcore.fission_moderator.fuel_discount", "Fuel Discount: %s");
        provider.add("block.phoenixcore.fission_cooler.info_header", "Fission Cooler");
        provider.add("block.phoenixcore.fission_fuel_rod.info_header", "Fission Fuel Rod");

        provider.add("phoenixcore.current_heat_display", "Core Temperature: %s / %s HU");
        provider.add("phoenixcore.status.scram", "§c§lSCRAM ACTIVE");

        provider.add("block.phoenixcore.fission_scram_hatch.desc",
                "Stops fuel consumption and heat generation when receiving a Redstone signal.");

        provider.add("phoenixcore.machine.fission_scram_hatch.tooltip",
                "§cEmergency Reactor Brake§r: Halts the reactor on §fany§r redstone signal.");
        provider.add("phoenixcore.machine.fission_scram_hatch.tooltip2",
                "§8No configuration. No mercy. Build your circuit carefully.");

        provider.add("phoenixcore.machine.fission_advanced_scram_hatch.tooltip",
                "§6Precision SCRAM Control§r: Triggers only above a configured signal strength,");
        provider.add("phoenixcore.machine.fission_advanced_scram_hatch.tooltip2",
                "§7and only after a sustained signal. Configurable via UI.");

        provider.add("phoenixcore.machine.fission_stability_sensor.tooltip",
                "§eThermal Monitor§r: Emits a §fproportional§r redstone signal based on core heat.");

        provider.add("gui.phoenixcore.stability_sensor.title", "Thermal Stability Configuration");
        provider.add("gui.phoenixcore.stability_sensor.min", "Min Heat Threshold %");
        provider.add("gui.phoenixcore.stability_sensor.max", "Max Heat Threshold %");
        provider.add("gui.phoenixcore.stability_sensor.invert", "Invert Signal");

        provider.add("gui.phoenixcore.advanced_stability_sensor.title", "Advanced Thermal Stability Configuration");
        provider.add("gui.phoenixcore.advanced_stability_sensor.min", "Min Heat Threshold %");
        provider.add("gui.phoenixcore.advanced_stability_sensor.max", "Max Heat Threshold %");
        provider.add("gui.phoenixcore.advanced_stability_sensor.strength", "Emit Strength (1–15)");
        provider.add("gui.phoenixcore.advanced_stability_sensor.invert", "Invert Signal");
        provider.add("gui.phoenixcore.advanced_stability_sensor.hint1", "Emits fixed strength on back face only.");
        provider.add("gui.phoenixcore.advanced_stability_sensor.hint2", "Pair with an Advanced SCRAM Hatch.");

        provider.add("gui.phoenixcore.advanced_scram.title", "Advanced SCRAM Configuration");
        provider.add("gui.phoenixcore.advanced_scram.threshold", "Min Signal Strength (1–15)");
        provider.add("gui.phoenixcore.advanced_scram.sustain", "Sustain Duration (ticks)");
        provider.add("gui.phoenixcore.advanced_scram.status_armed", "§c● SCRAMMED — Reactor HALTED");
        provider.add("gui.phoenixcore.advanced_scram.status_arming", "§eArming: %d / %d ticks");
        provider.add("gui.phoenixcore.advanced_scram.status_standby", "§a● Standby — Reactor Permitted");
        provider.add("gui.phoenixcore.advanced_scram.status_triggered", "§cArmed and triggered.");
        provider.add("gui.phoenixcore.advanced_scram.status_waiting", "§7Waiting for signal...");
        provider.add("gui.phoenixcore.advanced_scram.hint1", "Signal must meet strength threshold");
        provider.add("gui.phoenixcore.advanced_scram.hint2", "for the full sustain duration to SCRAM.");

        provider.add("phoenixcore.status.scram_basic", "§c§lSCRAM ACTIVE §8(Basic Hatch)");
        provider.add("phoenixcore.status.scram_advanced", "§6§lSCRAM ACTIVE §8(Advanced Hatch)");
        provider.add("phoenixcore.status.scram_arming", "§e§lSCRAM ARMING: §f%d / %d ticks");

        provider.add("phoenix.multiblock.pattern.info.multiple_fuel_rods",
                "Requires Fuel Rods. These generate base heat and determine recipe parallels.");
        provider.add("phoenix.multiblock.pattern.info.multiple_blankets",
                "Requires Blanket Rods. These act as targets for transmutation in Breeder cycles.");
        provider.add("phoenix.multiblock.pattern.info.multiple_moderators",
                "Moderators adjust heat generation and can provide EU or Parallel bonuses.");
        provider.add("phoenix.multiblock.pattern.info.multiple_coolers",
                "Coolers remove heat based on their tier and provided coolant fluid.");

        provider.add("gtceu.high_performance_breeder_reactor",
                "High-Performance Breeder Reactor");
        provider.add("gtceu.heat_exchanging",
                "Heat Exchanging");
        provider.add("gtceu.source_extraction",
                "Source Extraction");
        provider.add("gtceu.source_imbuement",
                "Source Imbuement");
        provider.add("gtceu.source_reactor",
                "Source Reactor");
        provider.add("gtceu.advanced_pressurized_fission_reactor",
                "Advanced Pressurized Fission Reactor");
        provider.add("gtceu.pressurized_fission_reactor", "Pressurized Fission Reactor");

        provider.add("gtceu.honey_chamber", "Honey Chamber");
        provider.add("gtceu.please", "Please Multiblock");
        provider.add("gtceu.simulated_colony", "Simulated Colony");
        provider.add("gtceu.comb_decanting", "Comb Decanter");
        provider.add("gtceu.swarm_nurturing", "Swarm Nurturing Chamber");
        provider.add("gtceu.apis_progenitor", "Apis Progenitor");

        provider.add("gtceu.tooltip.tier", "Tier: %s");

        provider.add("config.jade.plugin_phoenixcore.source_machine_info", "Source Machine Information");
        provider.add("config.jade.plugin_phoenixcore.plasma_furnace_info", "High-Pressure Plasma Arc Furnace Info");
        provider.add("config.jade.plugin_phoenixcore.tesla_network_info", "Tesla Network Information");
        provider.add("config.jade.plugin_phoenixcore.fission_machine_info", "Fission Machine Info");

        provider.add("jade.phoenixcore.shield_state", "Shield State: %s");
        provider.add("jade.phoenixcore.shield_health", "Shield Health: %d");
        provider.add("jade.phoenixcore.shield_cooldown", "Shield Recharging: %ds");
        provider.add("jade.phoenixcore.plasma_boost_duration", "Power Multiplier: %s");

        provider.add("jade.phoenixcore.plasma_boost_active", "Plasma Boost: %s Active");
        provider.add("jade.phoenixcore.no_plasma_boost", "No Plasma Catalyst");
        provider.add("jade.phoenixcore.tesla_stored", "Stored: ");
        provider.add("jade.phoenixcore.tesla_receiving", "Receiving: %s EU/t");
        provider.add("jade.phoenixcore.tesla_providing", "Providing: %s EU/t");
        provider.add("block.phoenixcore.tesla_battery.tooltip_empty", "§7A hollow casing. Provides no storage.");
        provider.add("block.phoenixcore.tesla_battery.tooltip_filled", "§aCapacity: §f%s EU");
        provider.add("config.jade.plugin_phoenixcore.imbuer_threads_info", "Alchemical Imbuer Threads Info");

        provider.add("jade.phoenixcore.blanket_input", "Blanket Fuel: %s");
        provider.add("jade.phoenixcore.blanket_output", "Breeding Product: %s");
        provider.add("jade.phoenixcore.blanket_amount", "Base per cycle: %s");
        provider.add("jade.phoenixcore.heat", "§cCore Heat: %s HU");
        provider.add("jade.phoenixcore.fission_meltdown_timer", "§6MELTDOWN: %s seconds!");
        provider.add("jade.phoenixcore.fission_safe", "§aCore Stable");
        provider.add("jade.phoenixcore.fission_no_coolant", "§cNO COOLANT DETECTED");
        provider.add("jade.phoenixcore.fission_heating", "§eCORE HEATING UP");

        provider.add("jade.phoenixcore.source_giving", "Producing Source");
        provider.add("jade.phoenixcore.source_taking", "Consuming Source");
        provider.add("jade.phoenixcore.source_consumption", "Source Consumption:");
        provider.add("jade.phoenixcore.source_production", "Source Production:");

        provider.add("multiblock.tooltip.machinetype", "Machine Type: %s");
        provider.add("multiblock.yellowline", "§e━━━━━━━━━━━━━━━━━━━━");
        provider.add("multiblock.underyellowline", "Hold §e§lSHIFT§r to display structure details!");
        provider.add("multiblock.structureadvtooltip", "Structure:");

        provider.add("multiblock.pchaccess1", "\u00A0\u00A0\u00A0§9Parallel Control Hatch: ✓");
        provider.add("multiblock.pchaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock can use PCHs to increase it's efficiency.");
        provider.add("multiblock.subtickaccess1", "\u00A0\u00A0\u00A0§3SubTick: ✓");
        provider.add("multiblock.subtickaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock performs subtick recipes!");
        provider.add("multiblock.perfocaccess1", "\u00A0\u00A0\u00A0§dPerfect OCs: ✓");
        provider.add("multiblock.perfocaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock supports perfect overclocks (4/4).");
        provider.add("multiblock.nooc1", "\u00A0\u00A0\u00A0§cOverclocks: X");
        provider.add("multiblock.nooc2", "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock can not overclock.");

        provider.add("multiblock.laseraccess1", "\u00A0\u00A0\u00A0§6Laser Target Access: ✓");
        provider.add("multiblock.laseraccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock can be powered with Laser Target Hatches.");
        provider.add("multiblock.needlaseraccess1", "\u00A0\u00A0\u00A0§6Laser Target Access: ✓");
        provider.add("multiblock.needlaseraccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock MUST be powered with Laser Target Hatches.");
        provider.add("multiblock.nopower1", "\u00A0\u00A0\u00A0§cEnergy Output: X");
        provider.add("multiblock.nopower2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock does NOT produce/use §3Energy§7.");
        provider.add("multiblock.energyoutputaccess1", "\u00A0\u00A0\u00A0§3Energy Output: ✓");
        provider.add("multiblock.energyoutputaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock provides §3Energy§7 output!");

        provider.add("multiblock.sourceoutputaccess1", "\u00A0\u00A0\u00A0§zSource Output: §3✓");
        provider.add("multiblock.sourceoutputaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock provides §zSource§7 output!");
        provider.add("multiblock.sourceinputaccess1", "\u00A0\u00A0\u00A0§zSource Input: §3✓");
        provider.add("multiblock.sourceinputaccess2",
                "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0§7This multiblock requires §zSource §7input!");

        provider.add("multiblock.tooltip.controller", "\u00A0\u00A0\u00A0Controller: %s");
        provider.add("multiblock.tooltip.iteminput", "\u00A0\u00A0\u00A0Input Bus: %s");
        provider.add("multiblock.tooltip.fluidinput", "\u00A0\u00A0\u00A0Input Hatch: %s");
        provider.add("multiblock.tooltip.itemoutput", "\u00A0\u00A0\u00A0Output Bus: %s");
        provider.add("multiblock.tooltip.fluidoutput", "\u00A0\u00A0\u00A0Output Hatch: %s");
        provider.add("multiblock.tooltip.energy", "\u00A0\u00A0\u00A0Energy Input: %s");
        provider.add("multiblock.tooltip.energyoutput", "\u00A0\u00A0\u00A0Energy Output: %s");
        provider.add("multiblock.tooltip.maintenance", "\u00A0\u00A0\u00A0Maintenance Hatch: %s");
        provider.add("multiblock.tooltip.muffler", "\u00A0\u00A0\u00A0Muffler Hatch: %s");
        provider.add("multiblock.tooltip.pch", "\u00A0\u00A0\u00A0Parallel Control Hatch: %s");

        provider.add("gtultimate.custom.tooltip_one_energy_hatch", "§fAccepts §lEXACTLY §61 energy hatch.");
        provider.add("gtultimate.custom.tooltip_dimensional_anchor",
                "§9§oOpens stable rifts to other dimensions, determined by its placement.\\n§7These gateways allow for accelerated resource extraction unique to each realm.\\n§7Requires distinct recipes for Overworld, Nether, and End configurations.");

        String anchorDesc = """
                §9§oOpens stable rifts to other dimensions, determined by its placement.
                §7These gateways allow for accelerated resource extraction unique to each realm.
                §7Requires distinct recipes for Overworld, Nether, and End configurations.""";
        String fabricatorDesc = "§d§oUtilizes raw aetherial energy to create complex constructs.\n§5Transmutes pure magical essence into tangible matter.";
        String alchemicalImbuerDesc = """
                §7The hub of your §zSource network§7, acting as a link for natural §zsoul§7.
                §7Handles the §rextraction§7 of §zsource§r from §rcarbon§7 based sources as well as imbuing §zsource§r into §runique materials.
                §7Source §rproduction and consumption§7 is decided by the current base §zsoul, §rflora§7, and §rharmonization§7 in your area.""";
        String sourceReactorDesc = """
                §7A §zSource§7 based reactor capable of converting §fmundane materials§7 for use in further §zSource§7 related processes.
                Reactor ability is handled by the current §zsoul§7 of your area.\s
                §zSource gem §7blocks can also be used near the reactor to provide further boost.\s
                Reactor will §cNOT RUN§7 below a §zsoul§7 cap of 1.""";
        String bioEngineDesc = """
                §7An §zEngine§7 capable of converting §zSource§7 into power.
                §7It's power flows throughout it's chassis §zdenoting it's strength.
                §fEU provided §7is dependent on the base §zsoul§7 and §aflora §7power in your area.
                §7Caps out at a §r5x boost.""";

        String largeSteamSifterDesc = "§bSifts through the chaff to get to the good stuff. \n" +
                "§7Good Vibrations.";

        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_large_steam_sifter",
                largeSteamSifterDesc);
        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_dimensional_anchor", anchorDesc);
        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_aetherial_fabricator", fabricatorDesc);
        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_alchemical_imbuer", alchemicalImbuerDesc);
        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_source_reactor", sourceReactorDesc);
        PhoenixLangHandler.multilineLang(provider, "gtultimate.custom.tooltip_bio_engine", bioEngineDesc);

        PhoenixLangHandler.multiLang(provider, "tooltip.phoenixcore.shield_stability_hatch", "Outputs shield stability",
                "as a redstone signal.");

        provider.add("phoenixcore.machine.multiblock.source_tank.tooltip",
                "Fill and drain through the controller or source hatches.");
        provider.add("phoenixcore.universal.tooltip.source_storage_capacity", "§zSource §9Capacity: §f%d mB");
    }
}
