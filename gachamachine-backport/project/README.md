# GachaMachine Forge 1.20.1 Backport

Target: Minecraft 1.20.1, Forge 47.4.22, Java 17.

This build keeps the original namespace and item/block IDs, downloads the official NeoForge 2.0.2 JAR during CI, extracts its assets/data, and migrates only the resource-format differences required by Minecraft 1.20.1.

Preserved in this compatibility build:
- 10 Gacha Machine IDs
- 10 Gacha Coin IDs
- 100 Capsule IDs
- supplied textures and models
- original loot tables and weighted odds
- 5-coin default activation threshold
- player interaction and capsule opening
- hopper input/output container behavior
- machine progress saved in NBT

The original geo/animation JSON files are copied into the built JAR as well. The first build validates the Forge 1.20.1 gameplay/core compatibility before re-enabling GeckoLib block-entity animation hooks.
