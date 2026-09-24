# CobblemonRider HD Optimized V6

Safe gameplay fixes over V5.

- Keeps V5 right-click behavior and per-entity form cache.
- Hybrid SWIM+FLY controllers no longer fight each other in water.
- Adds an air-control fallback for flying mounts whose horizontal motion stalls (notably aquatic flyers such as Kyogre).
- Enlarges interaction pick radius for rideable Pokemon, with extra reach for large HD aquatic/legendary models.
- Adds safe aerial dismount protection until the player reaches ground/water/lava or 30 seconds elapse.
- Protection is granted from Player.removeVehicle so it also covers the K dismount path.
- Optimizes swimming surface lookup by querying Heightmap only while descending.
- Keeps GZIP config sync, large-config support, form fallback and passenger bounds protections.
- Same modId: cobblemonrider. Replace V5; do not install both.
