# Applied Energistics 2 Integration

PhoenixCore provides advanced AE2 integration components that simplify item and fluid management for complex GTM multiblocks.

---

## 1. ME Tag Input Bus (`METagInputBusPartMachine`)

The **ME Tag Input Bus** is a powerful alternative to standard AE2 export buses. Instead of manually specifying items, it uses string expressions to dynamically filter and pull items from the network.

### Key Features:
- **Expression-Based Filtering**: Supports whitelist and blacklist expressions (e.g., `#forge:ores/iron`, `*ingot`).
- **Dynamic Configuration**: Automatically scans the connected ME network and populates its internal "Config" slots with matching items.
- **Large Stack Support**: Handles `Long` based quantities, allowing for massive item buffers.
- **Data Stick Integration**: Configuration can be copied and pasted between buses using a GTM Data Stick.

---

## 2. ME Tag Input Hatch (`METagInputHatchPartMachine`)

Similar to the Input Bus, the **ME Tag Input Hatch** provides expression-based filtering for fluids.

### Key Features:
- **Fluid Tag Support**: Pulls all fluids matching a specific tag from the ME network.
- **High Capacity**: Designed for high-tier multiblocks that require large volumes of diverse fluids (like the Phoenix Infuser).

---

## 3. Machine Implementation

These components extend GTM's base AE2 integration (`MEBusPartMachine`) but add a custom UI and logic layer for expression parsing.

### Tag Matching Logic (`TagMatcher`):
The core logic for expression parsing resides in the `TagMatcher` utility. It allows for:
- **Item/Fluid Name Matches**: Direct string matching (e.g., `iron_ingot`).
- **Tag Matches**: Prefixing with `#` (e.g., `#forge:dusts`).
- **Wildcards**: Using `*` for partial matches (e.g., `*uranium*`).

---

## 4. UI Widgets

PhoenixCore includes custom AE2-styled UI widgets:
- **`MultilineTextFieldWidget`**: Used to input long expression strings in the bus/hatch interfaces.
- **`LargeAmountPreviewWidget`**: A specialized widget that can render `Long` quantities as compact strings (e.g., `1.2M`), which standard AE2 widgets might struggle with.

---

## 5. Development Tips
- **Grid Nodes**: These parts are active AE2 Grid Nodes. They require a network connection and consume AE2 energy (AE).
- **Network Scanning**: The `updateConfigurationFromTags()` method is called periodically (every 20 ticks) to ensure the bus is always tracking newly added items in the network that match its filter.
- **Auto-IO**: These parts handle their own IO logic via `autoIO()`, which pulls from the grid and pushes into the machine's input buffers.
