# Fission System Architecture

The Fission system is a modular multiblock framework that calculates energy output and heat management based on the physical blocks present within the multiblock structure.

---

## Core Machine: `FissionWorkableElectricMultiblockMachine`
This is the base class for all fission reactors. It handles the "Component Resolution" phase when the structure is formed.

### Component Resolution
When the multiblock is formed, the machine scans its internal blocks and categorizes them into lists:
- `activeCoolers`: List of `IFissionCoolerType`
- `activeModerators`: List of `IFissionModeratorType`
- `activeFuelRods`: List of `IFissionFuelRodType`
- `activeBlankets`: List of `IFissionBlanketType` (Breeder only)

The machine then identifies a "Primary" component for each category (the type with the highest count) to determine the base operating parameters (e.g., what fluid it consumes).

---

## The Simulation Loop: `reactorTick()`
Unlike standard GTCEu machines that rely solely on recipes, Fission reactors run a custom tick loop.

### 1. Prerequisite Check
- Validates the presence of at least one Fuel Rod and one Cooler.
- Checks `IEnergyContainer` for available input fluids (coolant) and input items (fuel).

### 2. Heat Calculation
- **Base Heat**: Sum of `baseHeatProduction` from all active Fuel Rods.
- **Moderator Multiplier**: The base heat is multiplied by the `heatMultiplier` of active moderators.
- **Cooling Power**: Sum of `coolingCapacity` from all active Coolers.
- **Net Heat**: `(Generated Heat - Cooling Power)`.

### 3. Energy Generation
- Output EU/t is calculated based on the total heat produced, further amplified by the `EUBoost` values of moderators.
- Efficiency scales with the "Neutron Bias" of the fuel rods used.

### 4. Consumption and Output
- Fuel and Coolant are consumed from input hatches.
- **Depleted Fuel** and **Hot Coolant** are pushed to output hatches.
- Note: The reactor is designed to continue running even if output hatches are full (simulating a "waste-heavy" environment), though this is a configurable behavior.

---

## Safety and Meltdowns
The machine tracks `currentHeat` against a `maxSafeHeat` threshold defined in `PhoenixConfigs.FissionConfigs`.

- **Meltdown Logic**: If `currentHeat > maxSafeHeat`, a meltdown timer begins.
- **Meltdown Sequence**: 
    - The timer is visible via Jade/HUD.
    - If the timer reaches zero, `performExplosion()` is called.
    - Explosion power scales with the tier of fuel rods and moderators present.
    - Breaking the controller or structure while the timer is active triggers an immediate explosion.

---

## Breeder Logic: `BreederWorkableElectricMultiblockMachine`
Extends the base fission logic with a "Neutron Spectrum" simulation.
- **Breeding Cycle**: Consumes `Blanket Rods` and produces isotopes.
- **Distribution**: Uses a `WeightedKey` distribution system to decide which isotopes are produced based on the "Spectrum Bias" (influenced by the type of fuel and moderators used).
