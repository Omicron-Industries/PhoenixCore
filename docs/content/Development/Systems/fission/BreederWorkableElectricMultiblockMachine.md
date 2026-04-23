# BreederWorkableElectricMultiblockMachine

The `BreederWorkableElectricMultiblockMachine` extends the fission framework to support isotope breeding. It manages a dual-cycle reaction: power generation and material transformation.

---

## 1. Breeding Mechanics
The core of the breeder is the **Neutron Spectrum**. 

### Blanket Rods:
The machine tracks `activeBlankets` (IFissionBlanketType). Each blanket has:
- **`inputKey`**: The material consumed (e.g., `thorium`).
- **`amountPerCycle`**: Quantity required for one breeding cycle.
- **`outputs`**: A list of `BreedingOutput` objects containing potential isotopes.

### Output Distribution:
Unlike standard recipes, breeding uses a **Weighted Distribution**. When a cycle completes, the machine rolls for outputs based on:
1.  **Base Weight**: The likelihood of an isotope forming.
2.  **Instability**: A factor that increases the "Heat Cost" of breeding that specific isotope.

---

## 2. Resource Management
The Breeder handles blanket consumption via `tickBlanketConsumption()`.

- **Consumption**: Similar to fuel rods, blankets are consumed gradually over a `durationTicks` period.
- **Resource Recovery**: Depleted blankets are pushed to output hatches.
- **Spectrum Control**: The machine uses `getPrimaryBlanket()` to determine which "Breeding Mode" the reactor is currently in, which influences the output isotope pool.

---

## 3. UI and Jade Integration
The Breeder provides additional data to `FissionMachineProvider`:
- **Current Blanket Input**: The item/material currently being irradiated.
- **Potential Outputs**: A list of possible isotopes with their relative weights.
- **Breeding Progress**: Visual feedback on the isotope formation cycle.

---

## 4. Stability Hooks
The breeder is more sensitive to heat than the standard reactor.
- **Instability Scaling**: High-tier breeding outputs increase the reactor's `lastRequiredCooling` exponentially.
- **Thermal Runaway**: If heat management fails during a breeding cycle, the instability can cause a "Fission Surge," rapidly accelerating the meltdown timer.
