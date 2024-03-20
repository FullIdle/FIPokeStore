package me.fullidle.fipokestore.common;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import lombok.Getter;
import lombok.Setter;
import me.fullidle.ficore.ficore.common.api.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class PokeData {
    private final FileUtil pokeData;
    public PokeData(FileUtil pokeData){
        this.pokeData = pokeData;
    }
    public PokeInfo getPokemonPokeInfo(Pokemon pokemon){
        return getPokemonPokeInfo(pokemon.getSpecies());
    }
    public PokeInfo getPokemonPokeInfo(EnumSpecies species){
        String name = species.name;
        String payType = pokeData.getConfiguration().getString(name + ".payType");
        double v = pokeData.getConfiguration().getDouble(name + ".value");
        boolean c = pokeData.getConfiguration().getBoolean(name + ".canBuy",true);
        ArrayList<OfflinePlayer> applyPlayers = pokeData.getConfiguration().getStringList(name + ".applyPlayers").stream().
                map(s -> Bukkit.getOfflinePlayer(UUID.fromString(s))).collect(Collectors.toCollection(ArrayList::new));
        return payType == null?new PokeInfo(species,"none",-1,c,applyPlayers):new PokeInfo(species,payType,v,c,applyPlayers);
    }
    /**
     * 修改payValue
     * 返回的是旧的payValue
     * @return 返回的是旧的payValue
     */
    public PokeInfo setPokemonPokeInfo(Pokemon pokemon, PokeInfo pokeInfo){
        return setPokemonPokeInfo(pokemon.getSpecies(), pokeInfo);
    }
    public PokeInfo setPokemonPokeInfo(EnumSpecies species, PokeInfo pokeInfo){
        String name = species.name;
        PokeInfo oldValue = getPokemonPokeInfo(species);
        pokeData.getConfiguration().set(name+".payType", pokeInfo.payType);
        pokeData.getConfiguration().set(name+".value", pokeInfo.value);
        pokeData.getConfiguration().set(name+".canBuy", pokeInfo.canBuy);
        pokeData.getConfiguration().set(name+".applyPlayers",pokeInfo.players.stream().map(p->p.getUniqueId().toString()).collect(Collectors.toList()));
        pokeData.save();
        return oldValue;
    }

    @Getter
    @Setter
    public static class PokeInfo {
        private final EnumSpecies species;
        private String payType;
        private double value;
        private boolean canBuy;
        private ArrayList<OfflinePlayer> players;
        private PokeInfo(EnumSpecies species,String payType, double value, boolean canBuy,ArrayList<OfflinePlayer> players){
            this.payType = payType;
            this.value = value;
            this.canBuy = canBuy;
            this.players = players;
            this.species = species;
        }
    }
}
