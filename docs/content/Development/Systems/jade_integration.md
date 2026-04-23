# Jade Integration (WAILA)

PhoenixCore utilizes the **Jade** API (formerly WAILA/HWYLA) to provide real-time, server-authoritative information about its custom machines and systems directly in the world HUD.

---

## 1. Overview
Jade providers are split into two parts:
1.  **Server Data Provider (`IServerDataProvider`)**: Runs on the server and collects the data to be synchronized to the client.
2.  **Client Component Provider (`IBlockComponentProvider`)**: Runs on the client and handles the rendering of the tooltip based on the synchronized data.

PhoenixCore implements these in combined classes under `net.phoenix.core.integration.jade.provider`.

---

## 2. Server-Authoritative Data
To ensure accuracy, PhoenixCore providers push data from the server's machine instance into a `CompoundTag`. This avoids client-side desync where the machine's internal state might not match what the client thinks.

### Example: Fission Data Collection
In `FissionMachineProvider.java`, the server collects real-time heat, coolant status, and breeding info:

```java
@Override
public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
    if (accessor.getMetaMachine() instanceof FissionWorkableElectricMultiblockMachine machine) {
        tag.putDouble("pf_heat", machine.getHeat());
        tag.putBoolean("pf_is_scrammed", machine.isScramActive());
        tag.putInt("pf_meltdown_seconds", machine.getMeltdownSecondsRemaining());
        // ... more data
    }
}
```

---

## 3. Dynamic Tooltip Rendering
The client receives the `CompoundTag` and uses it to build the HUD display. PhoenixCore uses color coding and localization to make the information clear.

### Key Visual Cues:
- **Red/Bold "SCRAMMED"**: Indicates a safety shutdown is active.
- **Orange Meltdown Timer**: Only appears when the reactor is in a critical state.
- **Cyan Cooling Power**: Displays the current HU/t being removed.
- **Dynamic Item/Fluid Icons**: Providers like `SourceMachineProvider` use `resolveKeyToDisplayName` to show the names of materials being processed.

---

## 4. Specific Providers

### `TeslaNetworkProvider`
- Displays the current energy stored in the player's frequency.
- Shows total network capacity and net flow (EU/t).
- Lists how many "Live" hatches are currently contributing to the network.

### `FissionMachineProvider`
- Provides detailed reactor stats: Fuel Rod count, Moderator efficiency, and Cooler capacity.
- For **Breeder Reactors**, it lists the current input blanket and the potential output isotopes with their respective weights and instability.

### `ThreadedRecipeOutputProvider`
- A unique provider for `BasicThreadedMachine`.
- Instead of showing just one recipe, it iterates through all active "Threads" and displays the progress and output of each independent recipe running in parallel.

---

## 5. Development Tips
- **UIDs**: Every provider must have a unique `ResourceLocation` (e.g., `PhoenixCore.id("fission_machine_info")`) for users to be able to toggle them in the Jade config menu.
- **NBT Efficiency**: Only send data that has changed or is essential for the UI to minimize network traffic.
- **Helper Methods**: Use `resolveKeyToDisplayName` to handle GTM Materials, Vanilla Items, and Fluids consistently in the HUD.
