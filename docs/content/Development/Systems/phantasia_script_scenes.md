# Phantasia Script Scenes: Multiblocks, Text, and Timing

This document outlines how to integrate multiblocks, manage text display, and control timing within Phantasia script scenes.

---

## 1. Adding Multiblocks to Scenes

Details on how to define and place multiblocks within a Phantasia scene script.

### Defining Multiblock Structures
To define a multiblock structure, you typically reference a pre-defined multiblock ID. These IDs are usually configured in a separate data file (e.g., JSON, XML) or registered within the game's core systems.

*   **Referencing Existing Multiblock Definitions:**
    Multiblocks are identified by a unique string ID. For example, `multiblock_id: "my_custom_machine"`.

*   **Syntax for Specifying Multiblock Components and Their Positions:**
    While the multiblock definition itself is external, within a scene script, you might specify its orientation or relative position.
    ```
    // Example of defining a multiblock in a scene script (pseudo-code)
    define_multiblock("my_machine", "multiblock_id:my_custom_machine", {
        "orientation": "NORTH",
        "offset_x": 0,
        "offset_y": 0,
        "offset_z": 0
    });
    ```

### Spawning Multiblocks
Once defined, multiblocks can be spawned at specific locations within the scene.

*   **Commands or Functions to Spawn a Multiblock:**
    Use a `spawn_multiblock` command, specifying the multiblock's ID (as defined in the script) and its world coordinates.
    ```
    // Spawn 'my_machine' at world coordinates (100, 64, 200)
    spawn_multiblock("my_machine", 100, 64, 200);
    ```
    You can also specify an initial state or rotation if the multiblock supports it.
    ```
    spawn_multiblock("my_machine", 100, 64, 200, "rotation:Y_AXIS_90");
    ```

*   **Handling Multiblock States (e.g., active, inactive):**
    Multiblocks can have various states that can be manipulated during a scene.
    ```
    // Activate a multiblock
    set_multiblock_state("my_machine", "active");

    // Deactivate a multiblock
    set_multiblock_state("my_machine", "inactive");

    // Trigger a specific action on a multiblock
    trigger_multiblock_action("my_machine", "start_animation");
    ```

---

## 2. Managing Text in Scenes

How to display and control text elements within Phantasia script scenes.

### Displaying Dialogue and Narration
Text is crucial for conveying story, dialogue, and instructions.

*   **Syntax for Adding Text Boxes or Overlays:**
    Use a `display_text` command, often with parameters for the speaker, content, and duration.
    ```
    // Display narration
    display_text("Narrator", "A strange device hums to life.", 3.0); // Speaker, Text, Duration in seconds

    // Display character dialogue
    display_text("Player", "What is this thing?", 2.5);
    ```
    For longer dialogues, you might use a blocking call that waits for player input to continue.
    ```
    display_dialogue("NPC_Scientist", "This is my latest invention! It will revolutionize energy production.", true); // 'true' means wait for player input
    ```

*   **Specifying Speaker, Text Content, and Display Duration:**
    As shown above, these are typically direct arguments to the text display function. The speaker can be an entity ID or a generic name.

### Text Formatting
Enhance readability and emphasize parts of the text using formatting.

*   **Basic Formatting Options (e.g., bold, italics, color):**
    Phantasia's text system often supports basic markup similar to Markdown or rich text tags.
    ```
    display_text("Narrator", "The air grew *cold* and a [color:red]shiver[/color] ran down your spine.", 4.0);
    display_text("Player", "I need to find the **key**.", 2.0);
    ```
    Specific tags or syntax will depend on the underlying text rendering engine.

*   **Using Variables or Dynamic Content in Text:**
    You can embed variables from the scene's state or player data directly into text strings.
    ```
    set_variable("player_name", get_player_name());
    display_text("NPC", "Welcome, {player_name}! We've been expecting you.", 3.0);

    set_variable("item_count", get_inventory_item_count("ancient_relic"));
    display_text("Narrator", "You have {item_count} ancient relics.", 2.0);
    ```

---

## 3. Controlling Scene Timing

How to manage the flow and timing of events within Phantasia script scenes.

### Pausing and Delays
Control the pace of the scene and allow players to absorb information or react.

*   **Commands for Introducing Pauses or Delays in the Script Execution:**
    The `wait` or `delay` command is commonly used.
    ```
    // Wait for 2.5 seconds
    wait(2.5);

    display_text("Narrator", "The door slowly creaks open...", 3.0);
    wait(3.0); // Wait for the text to finish displaying and for the door animation

    // Wait for a specific game tick duration (if applicable)
    wait_ticks(50); // 50 ticks = 2.5 seconds at 20 ticks/second
    ```

*   **Waiting for User Input or Specific Events:**
    Beyond simple time delays, you can pause script execution until a player action or an in-game event occurs.
    ```
    // Wait until the player interacts with a specific object
    wait_for_interaction("lever_01");

    // Wait until a multiblock reaches a certain state
    wait_for_multiblock_state("my_machine", "operational");

    // Wait for player to press a 'continue' button after dialogue
    display_dialogue("Guide", "Follow me to the next area.", true); // 'true' indicates waiting for input
    ```

### Event Triggers
Define actions that occur based on conditions or time.

*   **How to Trigger Events Based on Time, Player Actions, or Multiblock States:**
    Scene scripts often support conditional logic and event listeners.
    ```
    // Trigger an event after a delay
    schedule_event("open_door_sequence", 5.0); // Event 'open_door_sequence' will run in 5 seconds

    // Example of an event listener (pseudo-code)
    on_player_enter_area("area_01", function() {
        display_text("Narrator", "You have entered a new zone.", 2.0);
        play_sound("zone_discovery");
    });

    on_multiblock_state_change("my_machine", "broken", function() {
        display_text("Narrator", "The machine has broken down!", 3.0);
        spawn_enemies(3);
    });
    ```

*   **Looping and Conditional Logic for Scene Progression:**
    Use standard programming constructs like `if/else` statements and `while` loops to control scene flow.
    ```
    // Conditional logic
    if (get_player_has_item("key_card")) {
        display_text("Door", "Access granted.", 1.5);
        open_door("main_door");
    } else {
        display_text("Door", "Access denied. Key card required.", 2.0);
    }

    // Looping example (wait until a condition is met)
    while (!is_multiblock_active("generator_core")) {
        display_text("Hint", "The generator needs to be activated.", 2.0);
        wait(5.0); // Wait and re-check
    }
    display_text("Narrator", "The generator hums to life!", 2.0);
    ```

---

## Example Scene Script

A comprehensive example demonstrating the use of multiblocks, text, and timing in a Phantasia script scene.

```
// Scene: Ancient Machine Activation

// 1. Initial Setup
display_text("Narrator", "You stand before an ancient, dormant machine.", 3.0);
wait(3.0);

// Define and spawn a multiblock
define_multiblock("ancient_generator", "multiblock_id:ancient_generator_structure", {
    "orientation": "SOUTH"
});
spawn_multiblock("ancient_generator", 150, 70, 250);
wait(1.0);

display_text("Player", "This must be the generator they spoke of.", 2.5);
wait(2.5);

// 2. Player Interaction and Conditional Logic
display_dialogue("Guide", "To activate it, you need to find three power crystals and place them in the conduits.", true);

// Loop until crystals are placed
while (get_multiblock_state("ancient_generator") != "powered") {
    display_text("Hint", "Look for glowing crystals in the surrounding ruins.", 3.0);
    wait(5.0); // Give player time to explore

    if (get_player_has_item("power_crystal_red") &&
        get_player_has_item("power_crystal_blue") &&
        get_player_has_item("power_crystal_green")) {

        display_text("Narrator", "You have all three power crystals!", 2.0);
        // Assume a function to place items into the multiblock
        place_items_in_multiblock("ancient_generator", ["power_crystal_red", "power_crystal_blue", "power_crystal_green"]);
        set_multiblock_state("ancient_generator", "powered"); // Update state
        display_text("Narrator", "The crystals slot perfectly into the conduits.", 3.0);
        play_sound("crystal_insert");
        wait(3.0);
    } else {
        display_text("Player", "I still need more crystals...", 2.0);
        wait(2.0);
    }
}

// 3. Activation Sequence
display_text("Narrator", "With the crystals in place, the machine begins to hum loudly.", 3.5);
play_sound("machine_startup_hum");
wait(3.5);

set_multiblock_state("ancient_generator", "activating");
trigger_multiblock_action("ancient_generator", "start_activation_animation");
display_text("Narrator", "Energy surges through its ancient circuits. Lights flicker to life!", 4.0);
play_sound("machine_activation_surge");
wait(4.0);

set_multiblock_state("ancient_generator", "active");
display_text("Narrator", "The ancient generator is now fully operational.", 3.0);
play_sound("machine_operational_loop");
wait(3.0);

// 4. Scene Conclusion
display_dialogue("Guide", "Excellent work! Now we can proceed to the next phase.", true);
fade_to_black(2.0); // Fade out over 2 seconds
end_scene();
```