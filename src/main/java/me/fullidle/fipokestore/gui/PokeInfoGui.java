package me.fullidle.fipokestore.gui;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.fullidle.ficore.ficore.common.api.ineventory.ListenerInvHolder;
import me.fullidle.fipokestore.enums.EnumInputType;
import me.fullidle.fipokestore.MyListener;
import me.fullidle.fipokestore.common.Pair;
import me.fullidle.fipokestore.common.PokeData;
import net.minecraft.entity.player.EntityPlayerMP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.fullidle.fipokestore.Main.*;
import static me.fullidle.fipokestore.MyListener.someData;

@Getter
public class PokeInfoGui extends ListenerInvHolder {
    private final Inventory inventory;
    private final Player player;
    private final Pokemon pokemon;
    private final ItemStack confirm;
    @Setter
    private Inventory upInv = null;
    public PokeInfoGui(Player player, EnumSpecies species){
        this(player,Pixelmon.pokemonFactory.create(species));
    }
    public PokeInfoGui(Player player, Pokemon pokemon){
        this.pokemon = pokemon;
        this.player = player;
        this.inventory = Bukkit.createInventory(this,
                36,
                papi(main.getConfig().getString("gui.title"),player,pokemon)
        );
        {
            //宝可梦图片
            this.inventory.setItem(13,getPhotoItem(pokemon,player));
        }
        {
            //确认购买
            confirm = new ItemStack(Material.STAINED_GLASS_PANE,1,(short)13);
            ItemMeta meta = confirm.getItemMeta();
            meta.setDisplayName(papi(main.getConfig().getString("gui.confirm.name"),player,pokemon));
            List<String> lore = main.getConfig().getStringList("gui.confirm.lore").stream().map(it->papi(it,player,pokemon)).
                    collect(Collectors.toList());
            meta.setLore(lore);
            confirm.setItemMeta(meta);
            this.inventory.setItem(31,confirm);
        }
        {
            if (player.isOp()){
                PokeData.PokeInfo pokeInfo = pokeData.getPokemonPokeInfo(pokemon);
                {
                    ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (pokeInfo.isCanBuy() ? 5 : 14));
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§3isCanBuy§7: "+(pokeInfo.isCanBuy()?"§a"+ true :"§c"+ false));
                    item.setItemMeta(meta);
                    inventory.setItem(29,item);
                }
                {
                    ItemStack item = new ItemStack(Material.BOOK_AND_QUILL);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§eEdit");
                    item.setItemMeta(meta);
                    inventory.setItem(33,item);
                }
            }
        }
        //事件处理
        onClose(e->{
            if (upInv == null){
                return;
            }
            Bukkit.getScheduler().runTask(main,()->player.openInventory(upInv));
        });

        onClick(e->{
            ItemStack item = e.getCurrentItem();
            if (item == null)return;
            e.setCancelled(true);
            UUID uuid = player.getUniqueId();
            PlayerPartyStorage pps = Pixelmon.storageManager.getParty(uuid);
            PokeData.PokeInfo pokeInfo = pokeData.getPokemonPokeInfo(pokemon);
            double v = pokeInfo.getValue();
            String payType = pokeInfo.getPayType();
            if (e.getSlot() == 29&&player.isOp()){
                pokeInfo.setCanBuy(!pokeInfo.isCanBuy());
                pokeData.setPokemonPokeInfo(pokeInfo.getSpecies(),pokeInfo);
                e.getCurrentItem().setDurability((short) (pokeInfo.isCanBuy() ? 5 : 14));
                return;
            }
            if (e.getSlot() == 33&&player.isOp()){
                player.closeInventory();
                MyListener.inputTypeMap.put(player, EnumInputType.PAYTYPE);
                someData.put(player,new Pair<PokeData.PokeInfo,String>(pokeInfo,null));
                Bukkit.getScheduler().runTask(main,()->{
                    payTypeInput.sendTo((EntityPlayerMP) ((Object) ((CraftPlayer) player).getHandle()));
                });
                return;
            }
            //点击购买的时候
            if (!item.equals(confirm)){
                return;
            }
            //如果是op直接给就好了
            if (!player.isOp()) {
                //判断是否未设置
                if (payType.equalsIgnoreCase("none")){
                    player.closeInventory();
                    //添加申请名单

                    if (!pokeInfo.getPlayers().contains(player)) {
                        pokeInfo.getPlayers().add(player);
                        pokeData.setPokemonPokeInfo(pokemon,pokeInfo);
                    }

                    //提示
                    player.sendMessage(getConfigColorMsg("Msg.noPriceSet"));
                    return;
                }
                {
                    //判断支付格式是否正确
                    if (!getAllMoneyTypeList().contains(payType)) {
                        player.closeInventory();
                        player.sendMessage(getConfigColorMsg("Msg.currencyNotExist"));
                        return;
                    }
                }

                //消耗
                double look = getBalance(payType, player);
                if (look < v){
                    player.closeInventory();
                    player.sendMessage(getConfigColorMsg("Msg.notEnoughMoney"));
                    return;
                }
                withdraw(payType,player,v);
            }
            //给与
            player.sendMessage(getConfigColorMsg("Msg.purchaseSuccessful"));
            pps.add(pokemon);
            player.closeInventory();
        });
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static String papi(String str,OfflinePlayer player,Pokemon pokemon){
        PokeData.PokeInfo pokeInfo = pokeData.getPokemonPokeInfo(pokemon);
        String payType = main.getConfig().getString("payTypeFormat." + pokeInfo.getPayType());
        String v = pokeInfo.getValue() == -1? main.getConfig().getString("payTypeFormat.none")
                : pokeInfo.getValue()+"";
        if (payType == null) payType = pokeInfo.getPayType();
        return getColorMsg(PlaceholderAPI.setPlaceholders(player,str).replace("{pokemon}",pokemon.getLocalizedName()).
                replace("{value}", v).
                replace("{paytype}",payType));
    }

    public static double getBalance(String payType,Player player){
        switch (payType){
            case "points":{
                return pointsApi.look(player.getUniqueId());
            }
            case "vault":{
                return vaultApi.getBalance(player);
            }
            default:{
                return nyeApi.getBalance(payType, player.getName());
            }
        }
    }
    public static void withdraw(String payType,Player player,double value){
        switch (payType){
            case "points":{
                pointsApi.take(player.getUniqueId(), (int) value);
                return;
            }
            case "vault":{
                vaultApi.withdrawPlayer(player,value);
                return;
            }
            default:{
                nyeApi.withdraw(payType,player.getName(), (int) value);
            }
        }
    }

    public static ItemStack getPhotoItem(Pokemon pokemon, OfflinePlayer player){
        ItemStack item = CraftItemStack.asBukkitCopy(((net.minecraft.server.v1_12_R1.ItemStack) (Object) ItemPixelmonSprite.getPhoto(pokemon)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(papi(main.getConfig().getString("gui.pokeInfo.name"),player,pokemon));
        List<String> lore = main.getConfig().getStringList("gui.pokeInfo.lore").stream().map(it->papi(it,player,pokemon)).collect(Collectors.toList());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
