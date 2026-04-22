# Soul and Source Architecture

The Soul system is a chunk-based data layer that tracks metaphysical density across the world, implemented via `SoulSavedData`.

---

## Backend: `SoulSavedData`
The soul field is managed as a `Map<ChunkPos, SoulChunkEntry>` attached to the `ServerLevel`.

### Data Fields
- **Current Soul**: The current resonance value, which fluctuates based on usage and regeneration.
- **Max Capacity**: The maximum resonance a chunk can hold. This can be permanently increased by certain high-tier "Soul Growth" recipes.
- **Natural Regeneration**: A tick-based system that slowly restores `Current Soul` towards `Max Capacity`.

### Biome Integration
Upon first generation or access, a chunk's base soul density is calculated using the biome's attributes:
- `calculateBiomeBase()` uses a combination of biome category lookups (e.g., `forest`, `magical`, `wasteland`) and a noise function based on the chunk coordinates to create a varied landscape of soul density.

---

## Mechanics

### 1. Soul Growth Hook (`SoulGrowthHook`)
Certain recipes in the `Alchemical Imbuer` or other machines can trigger soul growth.
- **Permanent Growth**: Increases the `Max Capacity` of the chunk.
- **Temporary Growth**: Refills the `Current Soul` value, potentially beyond the max capacity for a short time.
- Implementation: Uses a `RecipeCondition` and a `Mixin` (`WorkableMultiblockMachineMixin`) to trigger the growth effect upon successful recipe completion.

### 2. Soul Condition (`SoulCondition`)
A custom GTCEu `RecipeCondition` that prevents a machine from starting a recipe unless the local `SoulSavedData` reports a value within the required range.
- Supports both "Minimum Density" and "Reverse" (Maximum Density) requirements.

### 3. Source Multipliers
Machines like the `Alchemical Imbuer` and `BioAethericEngineMachine` calculate their performance multipliers by querying `SoulSavedData.get(level).getMultiplier(pos)`.
- This calculation is often combined with searches for blocks tagged as `#phoenix:soul_flowers` to determine the final operational resonance.

---

## Client-Side Visualization: `SoulMapWidget`
The **Soul Lens** uses a custom UI widget to render the soul field.
- **Data Sync**: The `inventoryTick` of the `SoulLensItem` synchronizes a 17x17 grid of chunk density data from the server to the item's NBT.
- **Rendering**: The `SoulMapWidget` iterates through this data, drawing colored squares to represent density. 
- **Soul Vision Shader**: The system includes a post-processing shader (`soul_vision.json`) that can be triggered to provide an in-world "heat map" of soul density, though this is currently reserved for specific high-tier gear effects.
