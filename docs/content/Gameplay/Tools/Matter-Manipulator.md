# Matter Manipulator

The **Phoenix Matter Manipulator** is an advanced construction tool designed specifically for large-scale automation and infrastructure building. It allows for the rapid placement and connection of pipes, cables, and wires over large distances and volumes.

---

## Operations

### 1. Selection Points
The tool operates based on a two-point selection system:
- **Point 1 (R-Click Block)**: Sets the primary anchor point.
- **Point 2 (Shift + R-Click Block)**: Sets the secondary anchor point.
- **Action (R-Click Air)**: Executes the manipulation based on the current mode and selection.

> [!WARNING]
> **Placement Rule**: You must always set **Point 1 HIGHER** than **Point 2**. Vertical building and volume filling only function downwards from the first anchor point.

### 2. Modes of Operation
Toggle between modes using the **Matter Manipulator Menu** (configured keybind):

| Mode | Description |
| :--- | :--- |
| **Line** | Connects Point 1 and Point 2 in a straight axis. Ideal for long cable runs. |
| **Wall** | Creates a 2D plane of connections between the two points. |
| **Grid** | Fills the entire 3D volume (cuboid) defined by the points. |
| **Connect** | Scans the area and forces connections between existing nodes without placing new blocks. |
| **Disconnect** | Severs all existing node connections within the selection. |

### 3. Tool Versatility
The Matter Manipulator functions as multiple tools in one:
- **Wrench**: Can be used to rotate and configure GregTech machines.
- **Wire Cutter**: Can be used to snip cable connections.

---

## Feedback Systems
- **Ghost Rendering**: While you have points selected, the tool will render a holographic "Ghost" box in the world showing you exactly what area will be affected.
- **Action Bar**: Displays real-time updates on your selection status and the total block count of the current operation.
- **Hotbar Warnings**: Will notify you if your point selection is invalid (e.g., trying to build upwards).
