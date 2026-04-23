---
title: Developer Documentation
---

# Developing PhoenixCore

If you want to contribute to the development of Phoenixcore, please feel free to submit a pull request with your changes. Or poke Phoenixvine, he's always up for a chat.

The following pages describe a few important concepts that you will likely run into when working with our codebase.

!!! link "GregTech Modern Docs"
    [:material-github: GregTech Modern :material-arrow-right: Wiki](https://gregtechceu.github.io/GregTech-Modern/1.20.1/)

    This mod is an addon to GregTech Modern. Most systems here are ran through the gtm api.   
    Please refer to its documentation as well.

---

## 🏗️ System Architecture

Technical deep-dives into PhoenixCore's core frameworks.

- [**Tesla Network**](Systems/tesla/architecture.md) - Global SavedData, BigInteger energy, and wireless IO logic.
- [**Source and Soul**](Systems/source/index.md) - Chunk-based data layers and biome integration.
- [**Fission Systems**](Systems/fission/index.md) - Modular multiblock simulation and reactor tick logic.
- [**Custom Multiblocks**](Systems/multiblocks.md) - Threaded machines, shielded machines, and HPCA extensions.

---

## 🛠️ API and Integrations

How PhoenixCore interacts with external mods and the GTM API.

- [**GTM API Extensions**](Systems/gtm_api_extensions.md) - Custom PartAbilities, RecipeTypes, and registration helpers.
- [**AE2 Integration**](Systems/ae2_integration.md) - Tag-based input buses/hatches and AE2 grid interactions.
- [**Jade (WAILA) Integration**](Systems/jade_integration.md) - Server-authoritative HUD providers and NBT syncing.
- [**RecipeHelper Utility**](Systems/recipe_helper.md) - Manual recipe IO, matching, and ticking for custom machines.
