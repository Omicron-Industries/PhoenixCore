# Tesla Network Architecture

The Tesla Network is a sophisticated, data-driven energy distribution system designed to provide wireless power across dimensions without the overhead of chunk loading.

---

## 1. Global Data Management (`TeslaTeamEnergyData`)

The core of the system is `TeslaTeamEnergyData`, which extends Minecraft's `SavedData`. It is attached to the Overworld's data storage to ensure global accessibility.

### Key Components:
- **`TeamEnergy` Class**: Stores the state for a single frequency (UUID).
    - `stored` & `capacity`: Uses `BigInteger` to support energy levels far beyond the limits of `Long` (OpV+ tiers).
    - `soulLinkedMachines`: A set of `BlockPos` for machines that consume/provide energy wirelessly.
    - `energyBuffered`: Tracks energy stored in physical Tesla Hatches.
    - `machineToTeam`: A global lookup map (`BlockPos` → `UUID`) for O(1) network identification.

### Synchronization:
- Data is saved to `phoenix_tesla_team_energy.dat`.
- The system uses a "Last Seen" mechanic (`markHatchActive`) to track which parts of the network are currently active and should be considered for flow calculations.

---

## 2. The Battery Interface (`ITeslaBattery`)

To allow various blocks to contribute to the network's capacity, PhoenixCore uses the `ITeslaBattery` interface.

### Requirements:
- `getCapacity()`: Returns the total storage provided.
- `getMaxInput()` / `getMaxOutput()`: Defines the throughput limits for this battery component.
- `getStored()` / `setStored()`: Manages the internal energy state.

The `TeslaTowerMachine` aggregates all blocks within its structure that implement this interface (like `TeslaBatteryBlock`) into a single logical pool.

---

## 3. Machine Integration

### `TeslaTowerMachine` (The Core)
The Tower acts as the "Brain" of a frequency.
- **Aggregation**: It scans its multiblock structure for `ITeslaBattery` components and sums their values.
- **Global Sync**: Every tick, it synchronizes its local `TeslaEnergyBank` with the global `TeslaTeamEnergyData`.
- **Flow Control**: It calculates the net energy change across the entire network and updates statistics for the UI.

### `TeslaEnergyHatchPartMachine`
Hatches are the physical IO points.
- **Dual Mode**:
    1. **Managed**: If part of a Tower, the Tower handles its energy logic.
    2. **Standalone**: If placed alone, it subscribes to server ticks and performs wireless IO directly against the `SavedData`.
- **Dynamic Linking**: Hatches automatically link to the owner's frequency unless overridden by a `TeslaBinderItem`.

---

## 4. The Tesla Binder (`TeslaBinderItem`)

The Binder is the primary interface for players and developers to interact with the network.

- **NBT Snapshotting**: To avoid high-frequency `SavedData` lookups, the Binder takes a "Snapshot" of the network state every 5 ticks.
- **Remote Management**: Allows players to toggle "Soul Links" on distant machines, bringing them into the network's logical flow without physical connections.

---

## 5. Wireless Charging (`TeslaWirelessChargerMachine`)

This machine extends the network's utility to player inventories.
- **Player Scanning**: Iterates through online players.
- **Team Verification**: Uses `TeamUtils` to ensure only players on the same frequency receive energy.
- **Injection**: Directly modifies the energy level of items in the player's main inventory and Curios slots.

---

## Performance Considerations
- **Chunk Loading**: The system *does not* require machines to be chunk-loaded to calculate energy flow. It only needs the `TeslaTowerMachine` or the individual hatches to be active.
- **BigInteger Math**: While more expensive than `Long`, `BigInteger` is only used for the aggregate network pool and individual battery storage, minimizing the performance impact.
- **Tick Subscriptions**: Only active "Standalone" hatches subscribe to server ticks, reducing the overhead for large networks.
