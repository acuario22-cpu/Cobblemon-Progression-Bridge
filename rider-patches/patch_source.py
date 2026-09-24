from pathlib import Path
import sys

root = Path(sys.argv[1])

build = root / "build.gradle"
text = build.read_text(encoding="utf-8")
text = text.replace("version = '1.2.4'", "version = '1.2.4-HD-Optimized-V4'")
build.write_text(text, encoding="utf-8")

mods = root / "src/main/resources/META-INF/mods.toml"
text = mods.read_text(encoding="utf-8")
text = text.replace('displayName="cobblemonrider"', 'displayName="CobblemonRider HD Optimized V4"')
text = text.replace(
    "Makes possible to ride pokemons on cobblestone.",
    "Compatibility-first CobblemonRider build with V2 mount behavior, compressed large-config sync and Mega/GMAX/Dynamax/Primal support."
)
mods.write_text(text, encoding="utf-8")

mixin = root / "src/main/java/dev/zanckor/cobblemonrider/mixin/PokemonMixin.java"
text = mixin.read_text(encoding="utf-8")

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
        return dev.zanckor.cobblemonrider.FormRideCompat.resolve(
                getPokemon().getSpecies().getName(),
                getPokemon().getForm().getName()
        );
    }
"""

if old_method not in text:
    raise SystemExit("PokemonMixin getter anchor not found")
text = text.replace(old_method, new_method, 1)
mixin.write_text(text, encoding="utf-8")

print("Applied CobblemonRider HD Optimized V4 compatibility-first patch")
