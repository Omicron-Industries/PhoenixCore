# Tesla Network

The **Tesla Network** is a revolutionary wireless energy distribution system that allows for the seamless transfer of power across dimensions, teams, and machines. It eliminates the need for complex cable management and long-distance wiring, providing a centralized "Cloud Energy" solution for late-game power needs.

---

## The Heart: Tesla Tower
The **Tesla Tower** is the primary multiblock required to establish and manage a Tesla Network. It acts as the physical storage and high-bandwidth gateway for your team's energy.

### Construction
- **Capacity**: The storage capacity of the network is determined by the **Tesla Batteries** installed within the tower.
- **Tiers**: The "Battery Tier" of the network (UHV to MAX) is determined by the highest tier of Tesla Battery installed. This tier dictates the maximum voltage/amperage the network can handle for certain operations.
- **Frequency**: Each tower is bound to a specific **Team UUID**. All members of that team share the same energy pool.

---

## The Tool: Tesla Binder
The **Tesla Binder** is the essential management tool for the network.

### Linking & Setup
1. **Personal Binding**: `Shift + Right-Click` the air to bind the tool to your current team frequency.
2. **Tower Sync**: `Shift + Right-Click` a Tesla Tower to copy its frequency, or `Right-Click` with a bound binder to set the Tower's frequency.
3. **Machine Sync**: `Right-Click` a bound Tesla Binder on any machine with an energy buffer to **Soul Link** it (see below).

### Management UI
`Right-Click` the air with a bound binder to open the **Tesla Network Management UI**.
- **Dashboard**: View total stored energy, capacity, and net EU/t flow (Input vs. Output).
- **Device List**: See every Uplink, Downlink, Soul-Linked machine, and Wireless Charger on the network.
- **Filtering**: Sort by Input (I), Output (O), Soul-Linked (S), or Chargers (C).
- **Remote Diagnostics**: Click the "Gear" icon or "Highlight" button to see exactly where a machine is located in the world (renders an X-ray box).
- **Flow Stats**: Monitor exactly how much EU/t each individual machine is drawing or providing.

---

## Power Transfer Methods

### 1. Tesla Hatches (Uplink & Downlink)
These are physical hatches you can add to any multiblock or use as single blocks.
- **Tesla Uplink (Input Hatch)**: Siphons energy from the machine/multiblock into the Tesla Cloud.
- **Tesla Downlink (Output Hatch)**: Broadcasts energy from the Tesla Cloud into the machine.
- **Wireless Capability**: These hatches do not require cables; they communicate directly with the team's data storage.

### 2. Soul Linking
A "Soul Link" is a direct metaphysical connection between a machine's internal buffer and the Tesla Network.
- **How to Link**: `Right-Click` a machine with a bound Tesla Binder.
- **Benefits**: No physical hatches required. The machine will automatically pull or push energy as needed.
- **Risks**: High-throughput machines can rapidly drain the network. **Battery Buffers** are blocked from Soul Linking due to the extreme risk of energy voiding.

### 3. Wireless Charging
The **Tesla Wireless Charger** is a single-block machine that provides global inventory charging.
- Once bound to a team, it will search for any online team members (regardless of dimension) and charge their electric items and armor.
- It provides a status notification ("Tesla Field Connected") when you are being actively charged.

---

## Armor Integration (Tesla Mode)
The **Phoenix Tech Suite** (Meta-Armor) features a dedicated **Tesla Mode**.
- **Flight**: Uses network energy to power creative or elytra flight, bypassing the need for internal armor power.
- **Auto-Charging**: Automatically keeps all items in your inventory charged using network energy.
- **Tesla Discharge**: A powerful defensive ability that consumes network energy to damage nearby enemies with lightning.
- **Status HUD**: Displays real-time network storage and your current drain on the HUD.
