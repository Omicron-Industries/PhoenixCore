# Chameleon Spray Can

The **Chameleon Spray Can** is a high-utility painting tool that combines the functionality of all colored spray cans into a single, rechargeable item. It allows for rapid re-coloring of pipes, cables, and various blocks without inventory clutter.

---

## Features

### 1. Dynamic Color Selection
Unlike standard spray cans, the Chameleon version can be set to any of the 16 Minecraft dye colors.
- **Scroll Selection**: While holding the spray can, use your mouse wheel (or the configured keybind) to cycle through available colors.
- **Solvent Mode**: The can also features a "Solvent" mode (represented by a purple icon/text), which strips paint from surfaces, returning them to their default unpainted state.

### 2. Multi-Block Painting
Hold `Shift` while using the tool to trigger a **Chain Paint** effect.
- This will automatically search for and re-color all connected blocks of the same type (e.g., a whole line of pipes or a wall of concrete).
- The maximum chain length is determined by your server's configuration.

### 3. Integrated Support
The Chameleon Spray Can has native support for:
- **GregTech**: Pipes, Cables, and any machine/hatch that implements `IPaintable`.
- **Vanilla Blocks**: Glass, Terracotta, Wool, Concrete, and Shulker Boxes.
- **Applied Energistics 2**: Supports re-coloring of AE2 cables and machines without requiring additional tools.

---

## Technical Details
- **HUD Overlay**: While held, a status overlay appears on your screen indicating the currently selected color or if the tool is in Solvent mode.
- **Sound Feedback**: Features a distinct pneumatic spray sound during use.
