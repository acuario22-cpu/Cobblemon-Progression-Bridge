from pathlib import Path
import sys

root = Path(sys.argv[1])

# Version the replacement JAR distinctly while keeping modId=cobblemonrider.
build = root / "build.gradle"
text = build.read_text(encoding="utf-8")
text = text.replace("version = '1.2.4'", "version = '1.2.4-HD-Optimized-V3'")
build.write_text(text, encoding="utf-8")

# Make the mod list clearly identify the custom replacement.
mods = root / "src/main/resources/META-INF/mods.toml"
text = mods.read_text(encoding="utf-8")
text = text.replace('displayName="cobblemonrider"', 'displayName="CobblemonRider HD Optimized"')
text = text.replace(
    "Makes possible to ride pokemons on cobblestone.",
    "CobblemonRider 1.20.1 custom compatibility build with compressed large-config sync, form-aware caching and HD mount support."
)
mods.write_text(text, encoding="utf-8")

# Patch PokemonMixin without replacing the whole upstream source file.
mixin = root / "src/main/java/dev/zanckor/cobblemonrider/mixin/PokemonMixin.java"
text = mixin.read_text(encoding="utf-8")

field_anchor = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$passengerObject;
"""
field_replacement = """    @Unique
    private PokemonJsonObject.PokemonConfigData cobblemonRiding$passengerObject;
    @Unique
    private String cobblemonRiding$cachedSpecies;
    @Unique
    private String cobblemonRiding$cachedForm;
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

        // Re-resolve only when the Pokemon actually changes species/form.
        // This preserves Mega/GMAX/Dynamax/Primal transitions without doing
        // repeated full config lookups every tick.
        if (cobblemonRiding$passengerObject == null
                || !Objects.equals(cobblemonRiding$cachedSpecies, species)
                || !Objects.equals(cobblemonRiding$cachedForm, form)) {
            cobblemonRiding$passengerObject =
                    dev.zanckor.cobblemonrider.FormRideCompat.resolve(species, form);
            cobblemonRiding$cachedSpecies = species;
            cobblemonRiding$cachedForm = form;
            cobblemonRiding$maxPassengers = -1;
        }

        return cobblemonRiding$passengerObject;
    }
"""
if old_method not in text:
    raise SystemExit("PokemonMixin getter anchor not found")
text = text.replace(old_method, new_method, 1)

mixin.write_text(text, encoding="utf-8")

print("Applied CobblemonRider HD Optimized V3 patch")
