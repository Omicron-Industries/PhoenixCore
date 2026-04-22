# Fission Reactor Systems

The **Fission Reactor System** is a complex, high-output energy generation framework involving multiblock reactors, thermal management, and fuel breeding.

---

## The Reactors

### 1. Pressurized Fission Reactor (PFR)
The standard entry-point for nuclear power. It consumes nuclear fuel rods to generate heat, which must be managed via coolant fluids.
- **Cooling**: Requires a steady supply of coolant (like Water or Sodium) to prevent core damage.
- **Power Output**: Determined by the fuel type and the number of active rods.

### 2. High-Performance Breeder Reactor
An advanced multiblock designed to convert "blanket" materials into useful isotopes while generating power.
- **Breeding Logic**: In addition to standard fuel consumption, this reactor uses **Blanket Rods** (Thorium, Uranium, etc.) to produce new materials.
- **Spectrum Bias**: The type of isotopes produced can be influenced by the reactor's configuration.

---

## Reactor Components

### 1. Fuel Rods
The heart of the reactor. Different tiers of rods (T1 to T5) provide varying levels of heat and energy.
- **Neutron Bias**: High-tier rods provide a "Neutron Flux" that can amplify the efficiency of surrounding rods.
- **Depletion**: Once a fuel cycle is complete, the rod is converted into **Spent Fuel**, which can be reprocessed.

### 2. Coolers
Blocks placed within the reactor structure to dissipate heat.
- **Cooling Power**: Each cooler has a specific HU/t (Heat Unit) capacity.
- **Coolant Requirements**: Some coolers require specific fluids to be pumped into the reactor's input hatches to remain active.

### 3. Moderators
Moderators like Graphite or Beryllium are used to control the reaction.
- **EU Boost**: Increases the energy output of the reactor.
- **Fuel Discount**: Reduces the speed at which fuel rods are consumed.
- **Parallel Bonus**: Allows the reactor to process multiple "virtual" recipes at once, increasing throughput.

### 4. Safety Systems: SCRAM Hatches
SCRAM (Safety Control Rod Axe Man) hatches are critical for preventing meltdowns.
- **Manual SCRAM**: Can be triggered via the UI to immediately shut down the reaction.
- **Automated SCRAM**: Advanced sensors can be configured to automatically trigger a SCRAM if core temperature exceeds a safe threshold.

---

## Thermal Management & Meltdowns
If a reactor generates more heat than its coolers can handle, the **Core Temperature** will rise.
- **Warning Phase**: The UI and Jade/Waila tooltips will show "CORE HEATING UP."
- **Meltdown Timer**: Once the temperature hits the critical limit, a countdown begins. 
- **The End**: If the temperature is not lowered (via SCRAM or increased cooling) before the timer hits zero, the reactor will explode, leaving behind radioactive waste and destroying the structure.
