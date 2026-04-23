# Core Mixins and Hooks

PhoenixCore utilizes Mixins to inject custom logic into Minecraft and GregTech Modern, enabling features like recipe blacklisting, soul resonance growth, and enhanced UI rendering.

---

## 1. Minecraft Mixins

### `RecipeManagerMixin`
Injects at the very beginning of recipe loading (`apply` method).
- **Function**: Automatically filters out recipes based on the `RecipeBlacklist` config.
- **Why**: Allows for hard-removing recipes from any mod (including vanilla and GTM) before they are even registered, ensuring a clean and controlled progression path.

### `GuiGraphicsCountSuffixMixin`
Overrides the default item count rendering in GUIs.
- **Function**: Adds support for compact number suffixes (e.g., `1.2M`, `16k`) on item stacks.
- **Why**: Necessary for high-tier GregTech gameplay where item quantities frequently exceed the 64-bit integer limit or standard UI space.

---

## 2. GregTech Modern Mixins

### `WorkableMultiblockMachineMixin`
Hooks into `afterWorking` for all multiblock machines.
- **Function**: Triggers the `SoulGrowthHook` if a recipe contains `soul_growth_perm` or `soul_growth_temp` data.
- **Why**: Integrates the magical Soul system directly into GTM's industrial machines, allowing alchemical recipes to "fertilize" the local environment.

### `ColorSprayBehaviourMixin`
Extends GTM's item behavior logic.
- **Function**: Implements custom logic for the **Chameleon Spraycan**, allowing it to interact with GTM's color-aware blocks and items.

### `NotifiableEnergyContainerMixin`
Modifies how GTM handles energy updates.
- **Function**: Provides hooks for the **Tesla Network** to monitor energy changes in physical hatches, enabling real-time wireless synchronization and statistics tracking.

---

## 3. Mod Integrations

### `RelayTileMixin` (Ars Nouveau)
Hooks into Ars Nouveau's Source Relay logic.
- **Function**: Allows Source Relays to interact with PhoenixCore's `SourceHatchPartMachine`, bridging the gap between GTM multiblocks and Ars Nouveau's Source network.

### `AE2 Mixins`
Various mixins into Applied Energistics 2 to support:
- **Expression Parsing**: Integrating `TagMatcher` logic into AE2 bus and hatch UI screens.
- **Large Stack Rendering**: Ensuring AE2 screens can correctly display the compact quantity suffixes introduced by PhoenixCore.

---

## 4. Developer Best Practices
- **Priority**: Use appropriate priorities (e.g., `priority = 100` for `RecipeManagerMixin`) to ensure compatibility with other coremods.
- **Remapping**: Ensure `remap = false` is used when mixining into non-obfuscated APIs like GTCEu or AE2.
- **Cleanup**: Always check for `null` or `isRemote()` before executing logic that affects server data or world state.
