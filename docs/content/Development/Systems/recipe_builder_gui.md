# Visual Recipe Builder GUI

The **Visual Recipe Builder** is a development tool included in PhoenixCore that provides a graphical interface for creating GregTech recipes. It streamlines the process of balanced recipe creation by allowing you to "draw" your recipes and export them directly to **KubeJS** or **Java Datagen** code.

---

## 🚀 Getting Started

### Opening the GUI
The Recipe Builder can be opened via a custom command (typically `/tesla ui` or similar, depending on your configuration) or by using the specialized developer item in the creative inventory.

---

## 🛠️ Main Features

### 1. General Settings
- **Recipe Type**: A dropdown containing all registered GTCEu recipe types (Assembler, Chemical Reactor, etc.).
- **Recipe ID**: The internal resource name for your recipe.
- **Duration & Energy**: Input boxes for the time (in ticks) and energy consumption (EU/t).
- **Source IO**: Specialized fields for PhoenixCore's Source energy requirements.

### 2. Item & Fluid Management
- **Visual Slots**: Drag and drop items from your inventory into the input or output grids.
- **Amount Editor**: `Right-click` a slot to open the amount editor, supporting compact numbers (e.g., `1M`, `16k`).
- **Fluid Support**: A dedicated tab for defining fluid inputs/outputs with volume and expression support.

### 3. Conditions Screen
A sub-menu for adding specialized logic to your recipes, such as:
- Biome restrictions.
- Soul resonance requirements.
- Tool/Machine tier checks.

---

## 💾 Exporting Your Work

The Builder provides several ways to move your recipe from the game into your codebase:

### Copy KJS
Generates a **KubeJS** compatible script snippet. 
- Format: `event.recipes.gtceu.assembler("my_recipe").itemInputs("1x iron_ingot")...`
- Copies directly to your clipboard for instant pasting into your `.js` files.

### Copy Java
Generates a **Java Datagen** snippet for use in GTM addon development.
- Includes standard `recipeBuilder` syntax and material registration.

### Send to Server
Directly sends the recipe data to the server, which can be configured to auto-generate a `.json` or `.java` file in the world directory for batch processing.

---

## 💡 Developer Tips
- **Persistence**: The GUI saves its state when closed. If you crash or relog, your work-in-progress recipe will be restored.
- **Not Consumable**: `Shift-click` items in the input panel to mark them as "Not Consumable" (e.g., molds or programmed circuits).
- **Automation**: Use the "Clear" button to quickly reset all fields before starting a new recipe in a batch.
