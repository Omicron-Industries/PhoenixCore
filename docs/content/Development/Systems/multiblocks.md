# Custom Multiblock Frameworks

PhoenixCore introduces several unique multiblock frameworks that extend GTCEu's base functionality to support specialized mechanics like threading and environmental shielding.

---

## 1. Threaded Machines (`BasicThreadedMachine`)
The `BasicThreadedMachine` is a specialized multiblock that can run multiple independent recipes simultaneously within a single machine instance.

### Key Features:
- **Parallel Processing**: Unlike standard GTCEu parallelization (which multiplies input/output of a *single* recipe), threading allows the machine to process different recipes at once.
- **Thread Management**: Default implementation supports 8 concurrent threads.
- **Recipe Matching**: The machine continuously searches for compatible recipes that aren't already being processed by another thread.
- **Persistence**: Thread progress and active recipes are saved to NBT, ensuring they resume correctly after world reloads.

### Implementation:
Machines extending this class must handle their own `recipeModifier` to define how threads behave (e.g., speed boosts or energy discounts).

---

## 2. Shielded Machines (`ShieldedMachine`)
A framework for multiblocks that require protection or specialized containment, often used for hazardous processes.

### Mechanics:
- **Shield Integrity**: Tracks a "Shield" value via `NotifiableShieldContainer`.
- **Damage Mitigation**: When the machine is active, it can absorb environmental damage or "overflow" stress from its own processes.
- **Visuals**: Supports dynamic shield rendering via `ShieldRenderProperty`.

---

## 3. Specialized Cleanrooms (`BlazingCleanroom`)
PhoenixCore extends the GTCEu Cleanroom system with new types that provide unique environmental conditions.

- **Blazing Cleanroom**: Designed for high-temperature manufacturing. Recipes tagged with `CleanroomType.BLAZING_CLEANROOM` can only be performed within this structure.
- **Logic**: Inherits from `CleanroomType`, allowing it to be easily integrated into standard GTCEu recipe definitions.

---

## 4. HPCA Components (`PhoenixHPCAMachine`)
Expands on the High-Performance Computing Array (HPCA) with custom parts and computation logic.

### Components:
- **Custom Modules**: New HPCA components that provide unique "Computation" or "Data" traits.
- **Advanced Networking**: Enhanced logic for how HPCA components interact with the controller, allowing for more complex data-driven recipes.
