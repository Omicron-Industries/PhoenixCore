package net.phoenix.core;

import com.gregtechceu.gtceu.api.data.chemical.material.IMaterialRegistryManager;

import net.phoenix.core.integration.phoenix_fission.api.block.*;
import net.phoenix.core.api.machine.trait.ITeslaBattery;
import net.phoenix.core.common.block.*;

import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionBlanketBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionCoolerBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission.FissionScramHatchPart;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission.FissionStabilitySensorPart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PhoenixAPI {

    public static IMaterialRegistryManager materialManager;

    public static PhoenixAPI instance;

    public static final Logger LOGGER = LogManager.getLogger();

    public static final Map<ITeslaBattery, Supplier<TeslaBatteryBlock>> TESLA_BATTERIES = new HashMap<>();
    public static final Map<IFissionCoolerType, Supplier<FissionCoolerBlock>> FISSION_COOLERS = new HashMap<>();
    public static final Map<IFissionModeratorType, Supplier<FissionModeratorBlock>> FISSION_MODERATORS = new HashMap<>();
    public static final Map<IFissionFuelRodType, Supplier<FissionFuelRodBlock>> FISSION_FUEL_RODS = new HashMap<>();
    public static final Map<IFissionBlanketType, Supplier<FissionBlanketBlock>> FISSION_BLANKETS = new HashMap<>();
    public static final Map<IFissionStabilityHatchType, Supplier<FissionScramHatchPart>> FISSION_STABILITY_HATCHES = new HashMap<>();
    public static final Map<IFissionSensorHatchType, Supplier<FissionStabilitySensorPart>> FISSION_SENSOR_HATCHES = new HashMap<>();
}
