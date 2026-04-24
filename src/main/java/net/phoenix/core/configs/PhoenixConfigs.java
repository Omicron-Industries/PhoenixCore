package net.phoenix.core.configs;

import net.phoenix.core.PhoenixCore;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

import java.util.Map;

@Config(id = PhoenixCore.MOD_ID)
public class PhoenixConfigs {

    public static PhoenixConfigs INSTANCE;
    public static ConfigHolder<PhoenixConfigs> CONFIG_HOLDER;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(PhoenixConfigs.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public FeatureConfigs features = new FeatureConfigs();

    @Configurable
    public CleanroomConfig cleanroom = new CleanroomConfig();

    @Configurable
    public SourceHatchConfig sourceHatch = new SourceHatchConfig();

    @Configurable
    public ColorConfig colors = new ColorConfig();

    @Configurable
    public FissionConfigs fission = new FissionConfigs();

    @Configurable
    public static WingFlightConfigs wingFlight = new WingFlightConfigs();

    @Configurable
    public FissionBlockStatsConfigs fissionStats = new FissionBlockStatsConfigs();

    // --- COLOR CONFIG ---

    public static class ColorConfig {

        @Configurable
        @Configurable.Comment({
                "Add custom formatting codes here.",
                "Format: 'char:hex' (e.g., 'z:BF00FF')",
                "Note: The code is a single character, and hex should not include #"
        })
        public String[] customColors = new String[] {
                "z:BF00FF",
                "°:00F2FF",
                "p:FF2100"
        };
    }

    // --- SOURCE HATCH CONFIG ---

    public static class SourceHatchConfig {

        @Configurable
        @Configurable.Comment({
                "The radius (in blocks) in which a Source Hatch will scan for nearby Source Jars to pull from." })
        public int sourceJarCheckRadius = 12;
    }

    // Wing config

    public static class WingFlightConfigs {

        @Configurable
        @Configurable.Comment({
                "EU/t drained from the Tesla network during powered elytra/sonic flight.",
                "Speed and boost scale proportionally with this value.",
                "Default: 5000"
        })
        public long poweredFlightEUt = 5_000L;

        @Configurable
        @Configurable.Comment({
                "EU/t drained from the Tesla network during creative flight mode.",
                "Set to 0 for truly free creative flight.",
                "Fly speed scales proportionally with this value.",
                "Default: 1000"
        })
        public long creativeFlightEUt = 1_000L;

        @Configurable
        @Configurable.Comment({
                "Base boost scale for powered elytra flight at minimum speed setting.",
                "The actual boost = boostMin + (speedSlider * (boostMax - boostMin))",
                "Default: 0.01"
        })
        public double poweredBoostMin = 0.01;

        @Configurable
        @Configurable.Comment({
                "Max boost scale for powered elytra flight at maximum speed setting.",
                "Scales further with poweredFlightEUt so higher drain = faster top speed.",
                "Default: 0.09"
        })
        public double poweredBoostMax = 0.09;

        @Configurable
        @Configurable.Comment({
                "Min creative fly speed (at speed slider = 0).",
                "Default: 0.05"
        })
        public double creativeSpeedMin = 0.05;

        @Configurable
        @Configurable.Comment({
                "Max creative fly speed (at speed slider = 10).",
                "Scales further with creativeFlightEUt so higher drain = faster top speed.",
                "Default: 0.2"
        })
        public double creativeSpeedMax = 0.20;

        @Configurable
        @Configurable.Comment({
                "Min speed clamp for powered flight (at drift slider = 0, tightest handling).",
                "Default: 0.6"
        })
        public double poweredDriftMin = 0.6;

        @Configurable
        @Configurable.Comment({
                "Max speed clamp for powered flight (at drift slider = 10, loosest/floatiest).",
                "Default: 1.8"
        })
        public double poweredDriftMax = 1.8;
    }

    // --- CLEANROOM CONFIG ---

    public static class CleanroomConfig {

        @Configurable
        @Configurable.Comment({
                "Whether the cleanroom deals lethal damage to players when active and at max cleanliness." })
        public boolean lethal = true;

        @Configurable
        @Configurable.Comment({ "The maximum cleanliness level of the cleanroom." })
        public int maxCleanliness = 1000;

        @Configurable
        @Configurable.Comment({ "The amount of pollution each player adds per tick inside the cleanroom." })
        public int playerPollution = 5;

        @Configurable
        @Configurable.Comment({ "The amount of cleanliness regenerated per tick when no players are inside." })
        public int regenRate = 1;

        @Configurable
        @Configurable.Comment({ "The amount of sterilizing gas consumed per tick." })
        public int fluidConsumption = 1;
    }

    // --- FEATURE & COMPUTATION & COOLING CONFIG ---

    public static class FeatureConfigs {

        @Configurable.Comment({ "Whether the ME Tag Input Bus and Hatch are enabled" })
        public boolean tagInputsEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Creative Energy Multiblock is enabled" })
        public boolean creativeEnergyEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Blazing Maintenance Hatch is enabled" })
        public boolean blazingHatchEnabled = true;

        @Configurable
        @Configurable.Comment({
                "Whether the Blazing Cleanroom is enabled (This just disables the casings, you can have the hatch on with this off just fine)" })
        public boolean blazingCleanroomEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Custom HPCA components are enabled" })
        public boolean HPCAComponetsEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Custom Phoenix HPCA multiblock is enabled" })
        public boolean PHPCAEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether recipes for the machines are enabled" })
        public boolean recipesEnabled = true;

        // COMPUTATION
        @Configurable
        @Configurable.Comment({ "How powerful the normal Phoenix Computation Unit is (CWU/t)" })
        public int BasicPCUStrength = 32;

        @Configurable
        @Configurable.Comment({ "How powerful the Advanced Phoenix Computation Unit is (CWU/t)" })
        public int PCUStrength = 64;

        @Configurable
        @Configurable.Comment({ "How much coolant the basic Phoenix Computation Unit uses" })
        public int BasicPCUCoolantUsed = 4;

        @Configurable
        @Configurable.Comment({ "How much coolant the Advanced Phoenix Computation Unit uses" })
        public int PCUCoolantUsed = 8;

        @Configurable
        @Configurable.Comment({ "How powerful the normal Phoenix Computation Unit is (CWU/t) when damaged" })
        public int damagedBasicPCUStrength = 16;

        @Configurable
        @Configurable.Comment({ "How powerful the advanced Phoenix Computation Unit is (CWU/t) when damaged" })
        public int damagedPCUStrength = 32;

        @Configurable
        @Configurable.Comment({
                "How much EU the normal Phoenix Computation uses per tick while not providing CWU/t (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int basicPCUEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the normal Phoenix Computation can use at max (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int basicPCUMaxEUt = 10;

        @Configurable
        @Configurable.Comment({
                "How much EU the advanced Phoenix Computation uses per tick while not providing CWU/t (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int PCUEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the advanced Phoenix Computation can use at max (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int PCUMaxEUt = 10;

        // COOLING
        @Configurable
        @Configurable.Comment({ "How powerful the Phoenix Heat Sink is (Cooling Provided)" })
        public int HeatSinkStrength = 4;

        @Configurable
        @Configurable.Comment({ "How powerful the Phoenix Active Cooler is (Cooling Provided)" })
        public int ActiveCoolerStrength = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the Phoenix Heat Sink uses per tick (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int HeatSinkEutUpkeep = 0;

        @Configurable
        @Configurable.Comment({
                "How much EU the Active Phoenix Cooler uses per tick (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int ActiveCoolerEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({ "How much coolant the Active Phoenix Cooler can use at max in milibuckets" })
        public int ActiveCoolerCoolantUse = 10;

        @Configurable
        @Configurable.Comment({
                "What Base Coolant the Active Phoenix Cooler uses while in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolantBase = "copper";

        @Configurable
        @Configurable.Comment({
                "What Stronger Coolant the Active Phoenix Cooler uses while in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant1 = "pcb_coolant";

        @Configurable
        @Configurable.Comment({
                "What Strongest Coolant the Active Phoenix Cooler uses when in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant2 = "sodium_potassium";

        @Configurable
        @Configurable.Comment({ "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
        public double BaseCoolantBoost = 1.0;

        @Configurable
        @Configurable.Comment({ "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
        public double CoolantBoost1 = 1.1;

        @Configurable
        @Configurable.Comment({
                "What Strongest Coolant the Active Phoenix Cooler uses when in the PHPCA (Gt or GT Kubejs Material)" })
        public double CoolantBoost2 = 1.2;

        @Configurable
        @Configurable.Comment({
                "The connection mode for Tesla Towers.",
                "TEAM_AUTO: All towers under a team/player share the same cloud automatically.",
                "DATA_STICK: Towers must be manually linked to hatches using a Data Stick."
        })
        public TeslaConnectionMode teslaConnectionMode = TeslaConnectionMode.DATA_STICK;

        public enum TeslaConnectionMode {
            TEAM_AUTO,
            DATA_STICK
        }
    }

    // --- FISSION CONFIG ---

    public static class FissionConfigs {

        @Configurable
        @Configurable.Comment("Enable the nuke block.")
        public boolean nukeEnabled = true;

        @Configurable
        @Configurable.Comment("Cube radius in blocks. Total affected volume is (2r+1)^3.")
        public int nukeCubeRadius = 16;

        @Configurable
        @Configurable.Comment("Hard cap to prevent insane values.")
        public int nukeCubeRadiusCap = 48;

        @Configurable
        @Configurable.Comment("Fuse time in ticks (20 ticks = 1 second). TNT is 80.")
        public int nukeFuseTicks = 120;

        @Configurable
        @Configurable.Comment("How many blocks to process per tick during cube wipe.")
        public int nukeBatchPerTick = 4000;

        @Configurable
        @Configurable.Comment("Skip blocks that have a BlockEntity (machines/chests).")
        public boolean nukeSkipBlockEntities = true;

        @Configurable
        @Configurable.Comment("Skip blocks in unloaded chunks (prevents chunk-forcing).")
        public boolean nukeSkipUnloadedChunks = true;

        @Configurable
        @Configurable.Comment("If true, replace removed blocks with fire instead of air.")
        public boolean nukeReplaceWithFire = false;

        @Configurable
        @Configurable.Comment("Flat heat added per tick while running (before rods/moderators/parallels).")
        public double baseHeatPerTick = 0.0;

        @Configurable
        @Configurable.Comment("Max bonus percent from continuous running (power + breeder output). Example: 60 = up to +60%.")
        public double burnBonusMaxPercent = 30.0;

        @Configurable
        @Configurable.Comment("Seconds of continuous running required to reach the max burn bonus. Example: 1200 = 20 minutes.")
        public double burnBonusRampSeconds = 1200.0;

        @Configurable
        @Configurable.Comment("The maximum heat a reactor can hold before starting the meltdown timer.")
        public double maxSafeHeat = 100000.0;

        @Configurable
        @Configurable.Comment("Minimum heat clamp.")
        public double minHeat = 0.0;

        @Configurable
        @Configurable.Comment("Maximum heat clamp to prevent runaway numeric overflow (meltdown still happens).")
        public double maxHeatClamp = 250000.0;

        @Configurable
        @Configurable.Comment("Does the reactor naturally lose heat when not running?")
        public boolean passiveCooling = true;

        @Configurable
        @Configurable.Comment("Heat lost per tick when idling.")
        public double idleHeatLoss = 1.0;

        @Configurable
        @Configurable.Comment("Base parallels added per fuel rod (before heat-based parallels).")
        public int parallelsPerFuelRod = 1;

        @Configurable
        @Configurable.Comment("How much heat is required to add +1 to the recipe parallel multiplier.")
        public double heatPerParallel = 10000.0;

        @Configurable
        @Configurable.Comment("Hard cap for parallels.")
        public int maxParallels = 256;

        @Configurable
        @Configurable.Comment("How much EU/t is generated per unit of CURRENT heat (power scales with current heat).")
        public double euPerHeatUnit = 1.0;

        @Configurable
        @Configurable.Comment("Optional cap on generated EU/t. Set <= 0 for no cap.")
        public long maxGeneratedEUt = 0;

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with parallels (recommended).")
        public boolean fuelUsageScalesWithParallels = true;

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with rod count (usually true).")
        public boolean fuelUsageScalesWithRodCount = true;

        @Configurable
        @Configurable.Comment("If true, blanket usage/output is additive across ALL blankets. If false, only the primary blanket is processed.")
        public boolean blanketUsageAdditive = true;

        @Configurable
        @Configurable.Comment("Clamp for total fuel discount percent from moderators.")
        public int maxFuelDiscountPercent = 90;

        @Configurable
        @Configurable.Comment("Clamp for total EU boost percent from moderators.")
        public int maxEUBoostPercent = 100;

        @Configurable
        @Configurable.Comment("If true, cooling only applies when coolant is present.")
        public boolean coolingRequiresCoolant = true;

        @Configurable
        @Configurable.Comment("If true, coolant usage is additive across all coolers. If false, uses primary cooler only.")
        public boolean coolantUsageAdditive = false;

        @Configurable
        @Configurable.Comment("Minimum EU/t produced while running (prevents 0). Set 0 to allow 0.")
        public long minGeneratedEUt = 1024;

        @Configurable
        @Configurable.Comment("Exponent for heat->power curve. 1 = linear, >1 rewards high heat.")
        public double powerCurveExponent = 2.0;

        @Configurable
        @Configurable.Comment("Heat fraction of maxSafeHeat where generation begins. 0.0 = always, 0.1 = starts at 10% of maxSafeHeat.")
        public double powerStartFraction = 0.0;

        @Configurable
        public MeltdownConfigs meltdown = new MeltdownConfigs();

        @Configurable
        public ExplosionConfigs explosion = new ExplosionConfigs();

        @Configurable
        @Configurable.Comment("If true, all fuel rods in the multiblock must be the same tier.")
        public boolean restrictFuelRodTier = true;

        @Configurable
        @Configurable.Comment("If true, all coolers in the multiblock must be the same tier.")
        public boolean restrictCoolerTier = true;

        @Configurable
        @Configurable.Comment("If >= 0, all fuel rods must be exactly this tier. Set to -1 to disable.")
        public int requiredFuelRodTier = -1;

        @Configurable
        @Configurable.Comment("If >= 0, all coolers must be exactly this tier. Set to -1 to disable.")
        public int requiredCoolerTier = -1;
    }

    public static class MeltdownConfigs {

        @Configurable
        @Configurable.Comment("Base grace seconds when barely above safe heat.")
        public double baseGraceSeconds = 60.0;

        @Configurable
        @Configurable.Comment("Minimum grace seconds when extremely above safe heat.")
        public double minGraceSeconds = 15.0;

        @Configurable
        @Configurable.Comment("Severity multiplier: higher = faster meltdown when over safe heat.")
        public double excessHeatSeverity = 1.0;

        @Configurable
        @Configurable.Comment("If true, falling back under safe heat clears the timer.")
        public boolean clearTimerWhenSafe = true;
    }

    public static class FissionBlockStatsConfigs {

        @Configurable
        @Configurable.Comment("If true, falling back under safe heat clears the timer.")
        public boolean clearTimerWhenSafe = true;

        @Configurable
        @Configurable.Comment("Configuration for Fission Coolers")
        public CoolerConfigs coolers = new CoolerConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Fission Moderators")
        public ModeratorConfigs moderators = new ModeratorConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Fission Fuel Rods")
        public FuelRodConfigs fuelRods = new FuelRodConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Breeder Blankets")
        public BlanketConfigs blankets = new BlanketConfigs();

        public static class CoolerConfigs {

            @Configurable
            @Configurable.Comment("Cooling amount (Heat/t) for each cooler type.")
            public Map<String, Integer> coolingAmount = Map.of(
                    "cooler_basic", 40,
                    "cooler_ev", 160,
                    "cooler_iv", 640,
                    "cooler_luv", 2560);

            @Configurable
            @Configurable.Comment("Coolant usage (mB/t) for each cooler type.")
            public Map<String, Integer> coolantUsage = Map.of(
                    "cooler_basic", 100,
                    "cooler_ev", 200,
                    "cooler_iv", 400,
                    "cooler_luv", 800);
        }

        public static class ModeratorConfigs {

            @Configurable
            @Configurable.Comment("EU/t boost multiplier for moderators.")
            public Map<String, Integer> euBoost = Map.of(
                    "graphite_moderator", 2,
                    "beryllium_moderator", 5,
                    "heavy_water_moderator", 12,
                    "niobium_sic_moderator", 30);

            @Configurable
            @Configurable.Comment("Heat production discount (subtracted from fuel heat).")
            public Map<String, Integer> fuelDiscount = Map.of(
                    "graphite_moderator", 1,
                    "beryllium_moderator", 2,
                    "heavy_water_moderator", 5,
                    "niobium_sic_moderator", 10);
        }

        public static class FuelRodConfigs {

            @Configurable
            @Configurable.Comment("Base heat production for fuel rods.")
            public Map<String, Integer> baseHeat = Map.of(
                    "t1_fuel_rod", 5,
                    "t2_fuel_rod", 20,
                    "t3_fuel_rod", 80,
                    "t4_fuel_rod", 320,
                    "t5_fuel_rod", 1280);

            @Configurable
            @Configurable.Comment("Neutron bias (affects breeding results).")
            public Map<String, Integer> neutronBias = Map.of(
                    "t1_fuel_rod", 0,
                    "t2_fuel_rod", 1,
                    "t3_fuel_rod", 2,
                    "t4_fuel_rod", 5,
                    "t5_fuel_rod", 10);

            @Configurable
            @Configurable.Comment("Duration in ticks for a full cycle.")
            public Map<String, Integer> cycleDuration = Map.of(
                    "t1_fuel_rod", 2000,
                    "t2_fuel_rod", 4000,
                    "t3_fuel_rod", 8000,
                    "t4_fuel_rod", 16000,
                    "t5_fuel_rod", 32000);
        }

        public static class BlanketConfigs {

            @Configurable
            @Configurable.Comment("Duration in ticks for breeder blanket conversion.")
            public Map<String, Integer> duration = Map.of(
                    "thorium_blanket", 1200,
                    "uranium_blanket", 2400,
                    "neptunium_blanket", 4800,
                    "plutonium_blanket", 9600,
                    "americium_blanket", 19200);
        }
    }

    public static class ExplosionConfigs {

        @Configurable
        @Configurable.Comment("If true, replaces blocks with air/fire. If false, uses standard GT explosion (drops items).")
        public boolean destructiveExplosion = false;

        @Configurable
        @Configurable.Comment("Explosion power scales with fuel rod count: power = base + rods * multiplier.")
        public double explosionPowerPerFuelRod = 1.5;

        @Configurable
        @Configurable.Comment("The base power of the meltdown explosion.")
        public float baseExplosionPower = 2.0f;

        @Configurable
        @Configurable.Comment("Max radius used for destructive bypass block wiping.")
        public int maxDestructiveRadius = 6;
    }
}
