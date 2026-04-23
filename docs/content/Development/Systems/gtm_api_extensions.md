# GregTech Modern API Extensions in PhoenixCore

PhoenixCore extensively utilizes and extends the GregTech Modern (GTM) API to introduce new machines, systems, and gameplay mechanics. This document outlines key areas where PhoenixCore interacts with and builds upon the GTM framework.

---

## 1. Machine and Multiblock Registration

All machines and multiblocks in PhoenixCore are registered using GTM's `GTRegistrate` system, ensuring seamless integration with the GTCEu ecosystem.

### `GTRegistrate` Usage:
The `PhoenixRegistration.REGISTRATE` instance is the central point for registering all custom content.

```java
public class PhoenixRegistration {
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(PhoenixCore.MOD_ID);
}
```

### Tiered Machine Registration:
PhoenixCore provides a helper method `registerTieredMachines` to simplify the creation of tiered versions of machines (e.g., hatches).

```java
public static MachineDefinition[] registerTieredMachines(String name,
                                                         BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory,
                                                         BiFunction<Integer, MachineBuilder<MachineDefinition, ?>, MachineDefinition> builder,
                                                         int... tiers) {
    MachineDefinition[] definitions = new MachineDefinition[GTValues.TIER_COUNT];
    for (int tier : tiers) {
        var register = REGISTRATE
                .machine(GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_" + name,
                        holder -> factory.apply(holder, tier))
                .tier(tier);
        definitions[tier] = builder.apply(tier, register);
    }
    return definitions;
}
```
This method abstracts away the boilerplate of creating a `MachineDefinition` for each tier, allowing developers to focus on the machine's specific logic and appearance.

---

## 2. Custom Part Abilities

PhoenixCore introduces custom `PartAbility` definitions to enable specialized interactions with multiblocks, such as Source and Plasma handling.

### Defining Custom Abilities:
Custom abilities are defined in `PhoenixPartAbility.java` and used in `FactoryBlockPattern` predicates.

```java
public class PhoenixPartAbility {
    public static final PartAbility SOURCE_INPUT = new PartAbility("source_input");
    public static final PartAbility SOURCE_OUTPUT = new PartAbility("source_output");
    public static final PartAbility PLASMA_INPUT = new PartAbility("plasma_input");
    // ... other custom abilities
}
```

### Usage in Multiblock Patterns:
These abilities are then referenced in `FactoryBlockPattern` to define where specific hatches or components can be placed.

```java
.where('C', blocks(SOURCE_FIBER_MACHINE_CASING.get())
        .or(abilities(PhoenixPartAbility.SOURCE_INPUT).setPreviewCount(1))
        .or(abilities(PhoenixPartAbility.SOURCE_OUTPUT).setPreviewCount(1))
        // ...
)
```

---

## 3. Custom Recipe Types and Modifiers

PhoenixCore defines its own `RecipeTypes` and `RecipeModifiers` to support unique processing mechanics.

### Custom Recipe Types:
Examples include `PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES`, `PhoenixRecipeTypes.PHOENIXWARE_FUSION_MK1`, etc. These are registered and then assigned to specific machines.

```java
public static final MultiblockMachineDefinition ALCHEMICAL_IMBUER = REGISTRATE
        .multiblock("alchemical_imbuer", AlchemicalImbuerMachine::new)
        .recipeTypes(PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES, PhoenixRecipeTypes.SOURCE_IMBUEMENT_RECIPES)
        // ...
```

### Custom Recipe Modifiers:
Machines can apply custom modifiers to recipes, altering their duration, energy consumption, or output based on machine-specific logic.

```java
.recipeModifiers(AlchemicalImbuerMachine::recipeModifier, OC_NON_PERFECT_SUBTICK)
```
The `AlchemicalImbuerMachine::recipeModifier` is a static method that implements `ModifierFunction` to modify recipes dynamically.

---

## 4. Multiblock Pattern Definition

PhoenixCore heavily utilizes GTM's `FactoryBlockPattern` and `Predicates` to define complex multiblock structures.

### `FactoryBlockPattern` Structure:
Patterns are defined using `aisle` for layers and `where` for block predicates.

```java
.pattern(definition -> FactoryBlockPattern.start()
        .aisle("CCC", "CCC", "CCC")
        .aisle("CCC", "C#C", "CCC")
        .aisle("CCC", "CSC", "CCC")
        .where('S', controller(blocks(definition.get())))
        .where('C', blocks(casing.get())
                .or(abilities(PhoenixPartAbility.SOURCE_INPUT).setExactLimit(1))
                // ...
        )
        .where('#', air())
        .build())
```

### Custom Predicates:
PhoenixCore can also define custom `Predicates` to simplify pattern definitions or introduce new matching logic. For example, `PhoenixPredicates.lampsByColor` could be used to match specific colored lamps.

```java
.where("H", PhoenixPredicates.lampsByColor(DyeColor.BLUE))
```

---

## 5. Custom Models and Dynamic Renderers

PhoenixCore extends GTM's rendering capabilities to provide unique visual feedback for machines.

### `workableCasingModel` and `addDynamicRenderer`:
Machines can specify custom models and dynamic renderers for their casings and internal components.

```java
.model(createWorkableCasingMachineModel(
        PhoenixCore.id("block/phoenix_enriched_tritanium_casing"),
        GTCEu.id("block/multiblock/fusion_reactor"))
        .andThen(d -> d
                .addDynamicRenderer(
                        PhoenixDynamicRenderHelpers::getPlasmaArcFurnaceRenderer)))
```
This allows for complex visual effects, such as the plasma arc in the Phoenix Infuser or the rotating gears in the Bio Aetheric Engine.

---

## 6. Material-Based Machines (Drums and Crates)

PhoenixCore leverages GTM's material system to create tiered storage solutions like drums and crates for custom materials.

### `registerDrum` and `registerCrate`:
These helper methods streamline the creation of storage blocks that scale with material properties.

```java
public static MachineDefinition AURUM_STEEL_DRUM = registerDrum(AURUM_STEEL,
        (80 * FluidType.BUCKET_VOLUME),
        "Aurum Steel Drum");

public static MachineDefinition AURUM_STEEL_CRATE = registerCrate(AURUM_STEEL, 90,
        "Aurum Steel Crate");
```
These methods automatically handle model generation, tooltips, and capacity scaling based on the provided material and capacity.

---

## Conclusion

By extending the GTM API in these key areas, PhoenixCore is able to introduce a rich set of new machines and systems that feel native to the GregTech experience while offering unique gameplay and development opportunities. Understanding these extension points is crucial for contributing to PhoenixCore's codebase.
