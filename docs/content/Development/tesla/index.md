# Tesla System Architecture

The Tesla Network is a data-driven energy distribution system implemented via `SavedData` to ensure persistence and cross-dimensional functionality without requiring chunk loading for energy calculations.

---

## Backend: `TeslaTeamEnergyData`
The core of the network resides in `TeslaTeamEnergyData.java`, which extends Minecraft's `SavedData`.

### Data Structure
- **Network Map**: A `Map<UUID, TeamEnergy>` where the UUID represents the team or player frequency.
- **TeamEnergy Class**: Contains the `stored` and `capacity` (using `java.math.BigInteger`), active endpoints (hatches, soul links, chargers), and flow statistics.
- **Global Lookup**: Maps `BlockPos` to `UUID` to allow machines to quickly find which network they belong to.

### persistence
All data is stored in `phoenix_tesla_team_energy.dat` in the world's `data` folder. Since it's attached to the Overworld's data storage, it's globally accessible from any dimension.

---

## Machine Logic

### `TeslaTowerMachine` (The Controller)
- **Energy Banking**: Uses a custom `TeslaEnergyBank` trait that aggregates `ITeslaBattery` capacities into a single `BigInteger` pool.
- **Syncing**: In its server tick, it synchronizes its local `energyBank` with the global `TeslaTeamEnergyData`.
- **Flow Distribution**:
    - **Pulling**: Aggregates energy from Input Hatches and Soul-Linked Generators.
    - **Pushing**: Distributes energy to Soul-Linked Consumers.
    - **Statistics**: Calculates `lastNetInput` and `lastNetOutput` every 20 ticks for UI display.

### `TeslaEnergyHatchPartMachine`
- **Wireless Ticking**: If not part of a `TeslaTowerMachine`, the hatch subscribes to server ticks.
- **IO Handling**: 
    - `IO.OUT` (Uplink): Drains internal machine buffer → `teamData.fill()`.
    - `IO.IN` (Downlink): `teamData.drain()` → fills internal machine buffer.
- **Dynamic Linking**: Supports automatic team linking based on owner UUID or manual linking via `TeslaBinderItem`.

### `TeslaWirelessChargerMachine`
- **Global Search**: Iterates through online players on the server.
- **Team Validation**: Checks `TeamUtils.isPlayerOnTeam` to ensure only authorized players receive energy.
- **Inventory Injection**: Uses `GTCapabilityHelper.getElectricItem` to charge items in the player's main inventory and Curios slots.

---

## Inter-System Communication

### The Tesla Binder (`TeslaBinderItem`)
The Binder acts as the bridge between the player and the `SavedData`.
- **NBT Sync**: During `inventoryTick`, the binder fetches a snapshot of the `TeamEnergy` data (including list of all machines and their current EU/t) and stores it in its own NBT.
- **UI Rendering**: The `ModularUI` reads this NBT snapshot to display the management interface. This avoids constant `SavedData` lookups during UI render ticks.

### Event Handling
- **TeslaLinkEventHandler**: Listens for machine placement/removal to ensure the `SavedData` lookup maps stay updated.
- **Armor Events**: `PhoenixArmorEvents` triggers checks for `teslaMode` to handle flight and discharge logic, drawing directly from the `TeslaTeamEnergyData` global instance.

---

## Performance Considerations
- **BigInteger**: Used for all energy storage to prevent overflows at OpV+ tiers.
- **Tick Subscriptions**: Hatches only tick when they are actively wireless and not part of a Tower (which handles its own internal hatches), reducing unnecessary tick overhead.
- **Snapshotting**: The Binder UI uses 5-tick snapshots rather than per-tick updates to minimize server-to-client NBT synchronization traffic.
