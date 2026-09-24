from pathlib import Path
import sys

root = Path(sys.argv[1])

build = root / "build.gradle"
text = build.read_text(encoding="utf-8")
text = text.replace("version = '1.2.4'", "version = '1.2.4-HD-Optimized-V6'")
build.write_text(text, encoding="utf-8")

mods = root / "src/main/resources/META-INF/mods.toml"
text = mods.read_text(encoding="utf-8")
text = text.replace('displayName="cobblemonrider"', 'displayName="CobblemonRider HD Optimized V6"')
text = text.replace(
    "Makes possible to ride pokemons on cobblestone.",
    "Compatibility-first CobblemonRider build with safe hybrid flight/swim controls, protected aerial dismounts, large-mount interaction reach and compressed config sync."
)
mods.write_text(text, encoding="utf-8")

mixin = root / "src/main/java/dev/zanckor/cobblemonrider/mixin/PokemonMixin.java"
text = mixin.read_text(encoding="utf-8")

field_anchor = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$passengerObject;
"""
field_replacement = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$passengerObject;
    @Unique
    private boolean cobblemonRiding$passengerObjectResolved;
    @Unique
    private String cobblemonRiding$cachedSpecies;
    @Unique
    private String cobblemonRiding$cachedForm;
    @Unique
    private PokemonJsonObject cobblemonRiding$cachedConfigSource;
"""
if field_anchor not in text:
    raise SystemExit("PokemonMixin field anchor not found")
text = text.replace(field_anchor, field_replacement, 1)

old_method = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$getPassengerObject() {
        if (cobblemonRiding$passengerObject == null) {
            cobblemonRiding$passengerObject = MCUtil.getPassengerObject(getPokemon().getSpecies().getName(), getPokemon().getForm().getName());
        }

        return cobblemonRiding$passengerObject;
    }
"""

new_method = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$getPassengerObject() {
        String species = getPokemon().getSpecies().getName();
        String form = getPokemon().getForm().getName();
        PokemonJsonObject configSource = CobblemonRider.pokemonJsonObject;

        if (!cobblemonRiding$passengerObjectResolved
                || cobblemonRiding$cachedConfigSource != configSource
                || !Objects.equals(cobblemonRiding$cachedSpecies, species)
                || !Objects.equals(cobblemonRiding$cachedForm, form)) {
            cobblemonRiding$passengerObject =
                    dev.zanckor.cobblemonrider.FormRideCompat.resolve(species, form);
            cobblemonRiding$cachedSpecies = species;
            cobblemonRiding$cachedForm = form;
            cobblemonRiding$cachedConfigSource = configSource;
            cobblemonRiding$passengerObjectResolved = true;
            cobblemonRiding$maxPassengers = -1;
        }

        return cobblemonRiding$passengerObject;
    }
"""
if old_method not in text:
    raise SystemExit("PokemonMixin getter anchor not found")
text = text.replace(old_method, new_method, 1)

# Robustness: avoid an out-of-range passenger offset from crashing the mount.
old_pos = """            ArrayList<Float> offSet = isControllingPassenger ? cobblemonRiding$getPassengerObject().getRidingOffSet() : cobblemonRiding$getPassengerObject().getPassengersOffSet().get(passengerIndex);

            setYBodyRot(getControllingPassenger().getYRot());
"""
new_pos = """            PokemonJsonObject.PokemonConfigData config = cobblemonRiding$getPassengerObject();
            ArrayList<Float> offSet;
            if (isControllingPassenger) {
                offSet = config.getRidingOffSet();
            } else {
                if (passengerIndex < 0 || passengerIndex >= config.getPassengersOffSet().size()) {
                    return;
                }
                offSet = config.getPassengersOffSet().get(passengerIndex);
            }

            if (offSet == null || offSet.size() < 3) {
                return;
            }

            setYBodyRot(getControllingPassenger().getYRot());
"""
if old_pos not in text:
    raise SystemExit("PokemonMixin position anchor not found")
text = text.replace(old_pos, new_pos, 1)


# Hybrid SWIM+FLY mounts: don't run swimming and flying vertical controllers at the same time.
old_movement = """    @Unique
    private void cobblemonRiding$movementHandler() {
        if (getControllingPassenger() instanceof Player passenger && cobblemonRiding$getPassengerObject() != null) {
            if (!cobblemonRiding$getPassengerObject().getMountTypes().contains(SWIM) && wasTouchingWater) return;

            cobblemonRiding$sprintHandler();
            cobblemonRiding$travelHandler();

            if (cobblemonRiding$getPassengerObject().getMountTypes().contains(SWIM)) {
                cobblemonRiding$swimmingHandler();
            }

            if (cobblemonRiding$getPassengerObject().getMountTypes().contains(LAVA_SWIM)) {
                cobblemonRiding$lavaSwimmingHandler();
            }

            if (cobblemonRiding$getPassengerObject().getMountTypes().contains(FLY)) {
                cobblemonRiding$flyingHandler();
            }

            cobblemonRiding$resetKeyData(passenger);
        }
    }
"""

new_movement = """    @Unique
    private void cobblemonRiding$movementHandler() {
        if (getControllingPassenger() instanceof Player passenger && cobblemonRiding$getPassengerObject() != null) {
            PokemonJsonObject.PokemonConfigData config = cobblemonRiding$getPassengerObject();
            ArrayList<PokemonJsonObject.MountType> mountTypes = config.getMountTypes();
            boolean inWater = isInWater() || wasTouchingWater;

            if (!mountTypes.contains(SWIM) && inWater) return;

            cobblemonRiding$sprintHandler();
            cobblemonRiding$travelHandler();

            if (mountTypes.contains(SWIM) && inWater) {
                cobblemonRiding$swimmingHandler();
            } else if (mountTypes.contains(FLY) && !onGround()) {
                cobblemonRiding$airControlAssist(passenger);
                cobblemonRiding$flyingHandler();
            }

            if (mountTypes.contains(LAVA_SWIM)) {
                cobblemonRiding$lavaSwimmingHandler();
            }

            cobblemonRiding$resetKeyData(passenger);
        }
    }
"""
if old_movement not in text:
    raise SystemExit("PokemonMixin movementHandler anchor not found")
text = text.replace(old_movement, new_movement, 1)

# Only query Heightmap while actually descending near the surface.
text = text.replace(
    "if (cobblemonRiding$getDistanceToSurface(this) <= 0.5 && cobblemonRiding$isShiftPressed()) {",
    "if (cobblemonRiding$isShiftPressed() && cobblemonRiding$getDistanceToSurface(this) <= 0.5) {",
    1
)

# Add an input-based fallback when a flying Pokemon's horizontal velocity stalls.
fly_anchor = """    @Unique
    private void cobblemonRiding$flyingHandler() {
"""
air_assist = """    @Unique
    private void cobblemonRiding$airControlAssist(Player passenger) {
        Vec3 current = getDeltaMovement();
        double horizontalSpeedSqr = current.x * current.x + current.z * current.z;

        // Preserve Rider's original movement whenever it is already working.
        if (horizontalSpeedSqr > 0.0004D) {
            return;
        }

        float forward = passenger.zza;
        float strafe = passenger.xxa;
        if (Math.abs(forward) < 0.01F && Math.abs(strafe) < 0.01F) {
            return;
        }

        Vec3 input = new Vec3(strafe * 0.5D, 0.0D, forward);
        if (input.lengthSqr() > 1.0D) {
            input = input.normalize();
        }

        input = input.yRot(-passenger.getYRot() * 0.017453292F);

        double speed = 0.18D
                * cobblemonRiding$getPassengerObject().getSpeedModifier()
                * Math.max(1.0F, cobblemonRiding$speedMultiplier);

        Vec3 assisted = new Vec3(input.x * speed, current.y, input.z * speed);
        setDeltaMovement(assisted);
        move(MoverType.SELF, new Vec3(assisted.x, 0.0D, assisted.z));
        cobblemonRiding$prevMovementInput = assisted;
    }

"""
if fly_anchor not in text:
    raise SystemExit("PokemonMixin flyingHandler anchor not found")
text = text.replace(fly_anchor, air_assist + fly_anchor, 1)

# Expand interaction/pick radius for rideable Pokemon, especially large aquatic HD models.
ground_anchor = """    @Override
    public boolean onGround() {
        return super.onGround();
    }
"""
pick_radius = ground_anchor + """
    @Override
    public float getPickRadius() {
        float base = super.getPickRadius();
        PokemonJsonObject.PokemonConfigData config = cobblemonRiding$getPassengerObject();
        if (config == null) {
            return base;
        }

        String species = getPokemon().getSpecies().getName();
        if (species != null && (
                species.equalsIgnoreCase("Kyogre")
                        || species.equalsIgnoreCase("Wailord")
                        || species.equalsIgnoreCase("Dondozo")
                        || species.equalsIgnoreCase("Gyarados")
                        || species.equalsIgnoreCase("Rayquaza")
                        || species.equalsIgnoreCase("Lugia")
        )) {
            return Math.max(base, 3.0F);
        }

        if (config.getMountTypes().contains(SWIM)) {
            return Math.max(base, 2.0F);
        }

        return Math.max(base, 1.0F);
    }
"""
if ground_anchor not in text:
    raise SystemExit("PokemonMixin onGround anchor not found")
text = text.replace(ground_anchor, pick_radius, 1)

mixin.write_text(text, encoding="utf-8")

print("Applied CobblemonRider HD Optimized V6 movement/safety patch")

