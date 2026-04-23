# DynamicFissionReactorMachine

The `DynamicFissionReactorMachine` is the high-tier implementation of the fission framework. It extends the base `FissionWorkableElectricMultiblockMachine` with support for dynamic components and advanced scaling.

---

## 1. Dynamic Component Scanning
Unlike the static reactor, the Dynamic Reactor performs a comprehensive "Component Scan" every time the structure is formed.

### Scanned Elements:
- **`activeCoolers`**: Collects `IFissionCoolerType` from all internal blocks.
- **`activeModerators`**: Collects `IFissionModeratorType`.
- **`activeFuelRods`**: Collects `IFissionFuelRodType`.
- **`activeBlankets`**: Collects `IFissionBlanketType`.

The machine uses the `getMultiblockState().getMatchContext()` to retrieve these lists, which are populated by the `FactoryBlockPattern` predicates during structure validation.

---

## 2. Advanced Scaling Logic
The Dynamic Reactor utilizes several scaling factors from `PhoenixConfigs` to determine its performance:

- **Heat Scaling**: `cfg().fuelUsageScalesWithRodCount` — if true, heat production is multiplied by the total number of fuel rods.
- **Parallel Scaling**: `computeParallels()` — calculates the maximum number of simultaneous recipes based on the moderator tier and count.
- **Efficiency Boosts**: Combines `EUBoost` from moderators with the `BaseCoolantBoost` from the current coolant fluid to determine final EU/t.

---

## 3. Tick Synchronization
The reactor uses `reactorTickHandler` to ensure that its internal simulation (heat, fuel, output) stays perfectly in sync with GTM's machine tick.

```java
protected void handleReactorLogic(boolean running) {
    if (running && !activeFuelRods.isEmpty()) {
        double heatProduced = computeHeatProducedPerTick(lastParallels);
        heat += heatProduced;
        tickFuelConsumptionMachineDriven(lastParallels);
        tickMachineOutputs(lastParallels);
    }
    // ... handling cooling and stability
}
```

---

## 4. Stability and Safety
The Dynamic Reactor includes hooks for `IFissionStabilityHatchType`.
- **Automated SCRAM**: If a stability hatch is present, the reactor can trigger a `setMachineActiveSafe(false)` when heat exceeds the stability threshold.
- **Data Export**: Stability data is pushed to the `SensorHatchPartMachine` for redstone integration.
