from pathlib import Path
import sys

root = Path(sys.argv[1])

build = root / "build.gradle"
text = build.read_text(encoding="utf-8")
text = text.replace("version = '1.2.4'", "version = '1.2.4-HD-Optimized-V5'")
build.write_text(text, encoding="utf-8")

mods = root / "src/main/resources/META-INF/mods.toml"
text = mods.read_text(encoding="utf-8")
text = text.replace('displayName="cobblemonrider"', 'displayName="CobblemonRider HD Optimized V5"')
text = text.replace(
    "Makes possible to ride pokemons on cobblestone.",
    "Compatibility-first CobblemonRider build with compressed large-config sync and safe per-entity form cache."
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

mixin.write_text(text, encoding="utf-8")

print("Applied CobblemonRider HD Optimized V5 safe-cache patch")
