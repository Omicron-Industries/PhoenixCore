# Automation and Logistics

PhoenixCore introduces advanced tools and machines to simplify the logistics of high-tier GregTech manufacturing, particularly when dealing with complex tags and large-scale networks.

---

## 1. ME Tag Input Bus & Hatch
These are specialized **Applied Energistics 2** components that revolutionize how you move items and fluids into your multiblocks.

### How they work:
Instead of manually placing items or fluids into configuration slots, these components use **Expression Filtering**. You type in what you want, and the bus/hatch pulls everything matching that description from your ME network.

### Features:
- **Expression Strings**: Support for item names, fluid names, and tags (e.g., `#forge:ores/iron`).
- **Wildcards**: Use `*` to match groups (e.g., `*ingot` pulls all ingots).
- **Blacklisting**: Prevent specific items from being pulled even if they match a tag.
- **Auto-Config**: The bus automatically scans your ME network every second and adds matching items to its internal buffer.
- **Large Capacity**: Designed to handle millions of items, perfect for late-game resource hungry machines.

---

## 2. Data Stick Management
You can copy the complex tag configurations from one ME Tag Bus/Hatch to another using a standard **GTM Data Stick**.
- **Copy**: `Shift + Right-Click` a configured Tag Bus with a Data Stick.
- **Paste**: `Right-Click` another Tag Bus to apply the settings.

---

## 3. Threaded Machines
The **Threaded Machine** framework allows a single multiblock to do the work of eight.

- **Independent Threads**: Unlike "Parallel" machines that run the *same* recipe multiple times, a Threaded machine can run **8 different recipes** at the exact same time.
- **Smart Scheduling**: If you give it multiple different inputs, it will automatically assign them to free threads.
- **Progress Tracking**: You can see the individual progress of all 8 threads by looking at the machine's HUD or using Jade.

---

## 4. Advanced Sensor Hatches
New sensor hatches provide more granular control over your automation.

- **Shield Stability Sensor**: Detects the integrity of a machine's containment shield. Essential for automating hazardous processes without causing meltdowns.
- **Fission Stability Sensor**: Specifically for Fission Reactors, providing a redstone signal proportional to the reactor's heat level or safety status.
