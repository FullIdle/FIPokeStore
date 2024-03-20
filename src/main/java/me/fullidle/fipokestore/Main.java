package me.fullidle.fipokestore;

import com.google.common.base.Enums;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mc9y.nyeconomy.api.NyEconomyAPI;
import com.pixelmonmod.pixelmon.api.dialogue.DialogueInputScreen;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.util.ITranslatable;
import lombok.Getter;
import me.fullidle.ficore.ficore.common.api.util.FileUtil;
import me.fullidle.fipokestore.common.PokeData;
import me.towdium.pinin.Keyboard;
import me.towdium.pinin.PinIn;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.util.text.translation.I18n;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {
    public static Main main;
    public static PokeData pokeData;
    public static PlayerPointsAPI pointsApi;
    public static NyEconomyAPI nyeApi;
    public static com.mc9y.nyeconomy.Main nye;
    public static Economy vaultApi;
    public static Map<Player,String> inputCache = new HashMap<>();
    public static DialogueInputScreen.Builder searchInput;
    public static DialogueInputScreen.Builder payTypeInput;
    public static DialogueInputScreen.Builder payValueInput;
    public static final PinIn pinIn;
    @Getter
    private static final ArrayList<String> allIndexSpec = new ArrayList<>();
    static {
        pinIn = new PinIn();
        pinIn.config().fCh2C(true).commit();
        pinIn.config().fZh2Z(true).commit();
        pinIn.config().fSh2S(true).commit();
        pinIn.config().fAng2An(true).commit();
        pinIn.config().fEng2En(true).commit();
        pinIn.config().fIng2In(true).commit();
        pinIn.config().accelerate(true).commit();
        pinIn.config().keyboard(Keyboard.QUANPIN).commit();

        for (EnumSpecies value : EnumSpecies.values()) {
            allIndexSpec.add(value.getLocalizedName());
            allIndexSpec.add(value.name.toLowerCase());
        }
    }

    @Override
    public void onEnable() {
        main = this;
        reloadConfig();
        {
            pointsApi = ((PlayerPoints) getServer().getPluginManager().getPlugin("PlayerPoints")).getAPI();
            nyeApi = NyEconomyAPI.getInstance();
            nye = (com.mc9y.nyeconomy.Main) getServer().getPluginManager().getPlugin("NyEconomy");
            vaultApi = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        }
        getLogger().info("§aPlugin loaded!");
        {
            //register
            getCommand("fipokestore").setExecutor(new CMD());
            getServer().getPluginManager().registerEvents(new MyListener(),this);
        }
        {
            //自动任务
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,()->{
                pokeData.getPokeData().getConfiguration().getKeys(false).forEach(pokeName -> {
                    EnumSpecies species = EnumSpecies.getFromNameAnyCase(pokeName);
                    PokeData.PokeInfo info = pokeData.getPokemonPokeInfo(species);
                    ArrayList<OfflinePlayer> players = info.getPlayers();

                    List<OfflinePlayer> meet = players.stream().
                            filter(player -> player.isOnline() && info.isCanBuy() && !info.getPayType().equalsIgnoreCase("none")).
                            collect(Collectors.toList());
                    meet.forEach(player -> player.getPlayer().sendMessage("§3§l" + pokeName + "§a§l is now available for purchase!"));
                    // 更新PokeInfo
                    info.getPlayers().removeAll(meet);
                    pokeData.setPokemonPokeInfo(info.getSpecies(), info);
                });
            },0,6000);
        }
    }

    @Override
    public void reloadConfig() {
        saveDefaultConfig();
        super.reloadConfig();
        {
            //宝可梦数据文件
            File file = new File(this.getDataFolder(),"pokeData.yml");
            pokeData = new PokeData(FileUtil.getInstance(file,true));
        }
        {
            //load InputScreen
            searchInput = DialogueInputScreen.builder().
                    setTitle(getConfigColorMsg("screen.search.title")).
                    setText(getConfigColorMsg("screen.search.text"));
            payTypeInput = DialogueInputScreen.builder().
                    setTitle(getConfigColorMsg("screen.payType.title")).
                    setText(getConfigColorMsg("screen.payType.text"));
            payValueInput = DialogueInputScreen.builder().
                    setTitle(getConfigColorMsg("screen.payValue.title")).
                    setText(getConfigColorMsg("screen.payValue.text"));
        }
    }

    public static List<String> getAllMoneyTypeList(){
        ArrayList<String> moneyList = new ArrayList<>(nye.vaults);
        moneyList.add("points");
        moneyList.add("vault");
        return moneyList;
    }

    public static String getColorMsg(String msg){
        return msg.replace("&","§");
    }
    public static String getConfigColorMsg(String key){
        String s = main.getConfig().getString(key);
        if (s == null) return null;
        return getColorMsg(s);
    }
}