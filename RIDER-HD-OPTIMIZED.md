# CobblemonRider HD Optimized V3

Replacement build for Forge 1.20.1 / Cobblemon 1.5.x based on upstream CobblemonRider 1.2.4.

## Changes

- Replaces the 32,767-character UTF config packet with versioned GZIP-compressed binary sync.
- Safety limits: 8 MiB decompressed / 2 MiB compressed config payload.
- Bumps network protocol so stock and optimized client/server JARs cannot silently mix.
- Builds a normalized O(1) species/form config lookup cache.
- Clears the lookup cache when a new server config is received.
- Form-aware per-Pokemon cache recalculates only when species/form actually changes.
- Explicit form config wins; otherwise any unknown form falls back to the base species.
- Handles species names prefixed by Mega, GMAX, Gigantamax, Dynamax and Primal.
- Keeps the same modId: cobblemonrider. Replace the old JAR; do not install both.

For the 315-entry HD config used during development:
- compact JSON: 33,832 bytes/chars
- GZIP payload: about 4.5 KiB
