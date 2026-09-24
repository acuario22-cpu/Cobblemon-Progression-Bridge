package dev.zanckor.cobblemonrider.network;

import dev.zanckor.cobblemonrider.CobblemonRider;
import dev.zanckor.cobblemonrider.config.PokemonJsonObject;

public class ClientHandler {

    public static void saveConfigObject(PokemonJsonObject jsonObject) {
        CobblemonRider.pokemonJsonObject = jsonObject;
    }
}
