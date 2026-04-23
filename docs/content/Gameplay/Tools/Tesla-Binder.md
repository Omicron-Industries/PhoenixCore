# Tesla Binder

The **Tesla Binder** is the universal management and configuration tool for the Tesla Network. It allows you to bind machines, manage network settings, and monitor energy flow in real-time.

---

## 🔧 Core Mechanics

### 1. Frequency Binding
To interact with a network, the Binder must be "Bound" to a frequency (UUID).
- **Personal Binding**: `Shift + Right-Click` the air to bind the tool to your own player frequency.
- **Copying Frequency**: `Shift + Right-Click` a **Tesla Tower** or an already configured **Tesla Hatch** to copy its frequency into the Binder.
- **Applying Frequency**: `Right-Click` a machine or hatch with a bound Binder to set it to that frequency.

### 2. Soul Linking
Soul Linking is a way to wirelessly connect any GTM machine with an energy buffer directly to the Tesla Network without using physical hatches.
- **How to Link**: `Right-Click` a machine with a bound Tesla Binder.
- **Visual Feedback**: A purple "Soul Link" particle effect will appear around the machine.
- **Limitations**: You cannot Soul Link a Battery Buffer, as the internal energy logic of buffers is incompatible with wireless streaming.

---

## 🖥️ Management Interface
`Right-Click` the air with a bound Binder to open the **Network Dashboard**.

### Network Overview
- **Storage Bar**: Shows current stored energy vs. total capacity across all connected batteries.
- **Net Flow**: Displays the real-time EU/t entering and leaving the network.
- **Frequency Info**: Shows the team name or UUID associated with the network.

### Device List
The dashboard lists every machine currently connected to your frequency.
- **Uplinks (I)**: Machines pushing energy into the cloud.
- **Downlinks (O)**: Machines drawing energy from the cloud.
- **Soul Links (S)**: Wireless direct-to-buffer connections.
- **Wireless Chargers (C)**: Global player inventory chargers.

### Remote Diagnostics
- **Highlighting**: Click the "Eye" icon to render a world-space highlight box around a specific machine, even through walls.
- **Power Stats**: Hover over a device to see its individual contribution (EU/t) and its location (coordinates and dimension).
- **Remote Toggle**: You can remotely disable or disconnect machines from the list.

---

## 💡 Advanced Tips
- **Network Snapshots**: The Binder UI updates its data every 5 ticks to save performance. If you see a slight delay in energy readings, this is normal.
- **Multiple Networks**: You can carry multiple Binders, each bound to a different team frequency, to manage complex multi-team setups.
- **Global Range**: The Binder works across all dimensions. You can monitor your Overworld tower while exploring the End.
