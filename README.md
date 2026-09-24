# Cobblemon TAN Realism

Forge 1.20.1 addon for Cobblemon 1.5.2 and Tough As Nails 9.2.0.171.

## Default behavior
- Fire and Ice Pokemon affect player temperature.
- Influence scales with Pokemon level.
- Influence radius scales from about 1.5 blocks to 5 blocks.
- Thermal strength falls off smoothly with real distance.
- Wild Pokemon can cause HOT/ICY extremes.
- Owned Pokemon can warm/cool the player but do not cause HOT/ICY by default.
- Fire and Ice Pokemon cancel each other naturally.
- Shoulder Pokemon use zero distance when owned by the observing player.
- If Cobblemon Integrations 1.0.7 is installed, only its original Pokemon temperature modifier is disabled; hydration and other integrations are preserved.

## Mild world preset
The built-in data downgrades vanilla Overworld HOT biomes to WARM and ICY biomes to COLD. Runtime defaults make ambient changes slow while player/Pokemon temperature changes remain responsive.

Config file: config/cobblemon-tan-realism.toml
