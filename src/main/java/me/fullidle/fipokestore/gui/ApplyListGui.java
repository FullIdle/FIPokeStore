package me.fullidle.fipokestore.gui;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;
import lombok.Getter;
import me.fullidle.ficore.ficore.common.api.ineventory.ListenerInvHolder;
import me.fullidle.fipokestore.common.PokeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.fullidle.fipokestore.Main.main;
import static me.fullidle.fipokestore.Main.pokeData;

@Getter
public class ApplyListGui extends ListenerInvHolder {
    private final Inventory inventory;
    private final Map<ItemStack, PokeData.PokeInfo> itemStackPokeInfoMap = new HashMap<>();
    private final Inventory[] pageInv;
    private Integer nowPage = 0;

    public ApplyListGui() {
        this.inventory = Bukkit.createInventory(this, 54, "ApplyList");
        {
            //物品
            ArrayList<ItemStack> photoList = new ArrayList<>();
            for (String pokeName : pokeData.getPokeData().getConfiguration().getKeys(false)) {
                EnumSpecies species = EnumSpecies.getFromNameAnyCase(pokeName);
                PokeData.PokeInfo info = pokeData.getPokemonPokeInfo(species);
                if (!info.getPlayers().isEmpty() && info.isCanBuy() && info.getPayType().equalsIgnoreCase("none")) {
                    ItemStack item = getPokemonPhotoItem(species, info);
                    itemStackPokeInfoMap.put(item, info);
                    photoList.add(item);
                }
            }
            //分页
            pageInv = new Inventory[photoList.size() > 54 ?
                    ((photoList.size() / 45) + (photoList.size() % 45 > 0 ? 1 : 0)) : 1];
            if (pageInv.length > 1){
                int x = 0;
                for (int i = 0; i < pageInv.length; i++) {
                    Inventory inv = Bukkit.createInventory(null, 45);
                    for (int i1 = 0; i1 < 45; i1++) {
                        inv.addItem(photoList.get(x));
                        x++;
                    }
                    pageInv[i] = inv;
                }
                {
                    //翻页控制台 下页 上页
                    ItemStack item = new ItemStack(Material.ARROW);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§3PreviousPage");
                    item.setItemMeta(meta);
                    inventory.setItem(47,item);
                    meta.setDisplayName("§3NextPage");
                    item.setItemMeta(meta);
                    inventory.setItem(51,item);
                }
            }else{
                Inventory inv = Bukkit.createInventory(null, 54);
                inv.addItem(photoList.toArray(new ItemStack[0]));
                pageInv[0] = inv;
            }
            //修改页面
            changePage(0);
        }
        //事件处理
        onClick(e -> {
            e.setCancelled(true);
            ItemStack currentItem = e.getCurrentItem();
            if (!(e.getWhoClicked() instanceof Player)) {
                return;
            }
            if (currentItem == null || currentItem.getType().equals(Material.AIR)) {
                return;
            }
            //判断是否是上下页的按钮
            if (currentItem.getType().equals(Material.ARROW)){
                int page = 0;
                if (e.getSlot() == 47){
                    if (nowPage == 0){
                        return;
                    }
                    page = nowPage-1;
                }
                if (e.getSlot() == 51){
                    if (nowPage == pageInv.length - 1){
                        return;
                    }
                    page = nowPage+1;
                }
                changePage(page);
                return;
            }

            PokeData.PokeInfo pokeInfo = itemStackPokeInfoMap.get(currentItem);
            if (pokeInfo == null) {
                return;
            }
            Player player = (Player) e.getWhoClicked();
            PokeInfoGui gui = new PokeInfoGui(player, pokeInfo.getSpecies());
            gui.setUpInv(this.inventory);
            Inventory inv = gui.getInventory();
            Bukkit.getScheduler().runTask(main, () -> {
                player.closeInventory();
                player.openInventory(inv);
            });
        });
    }

    @NotNull
    private static ItemStack getPokemonPhotoItem(EnumSpecies species, PokeData.PokeInfo info) {
        ItemStack item = CraftItemStack.asBukkitCopy(((net.minecraft.server.v1_12_R1.ItemStack) ((Object) ItemPixelmonSprite.getPhoto(Pixelmon.pokemonFactory.create(species)))));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§3" + species.getLocalizedName());
        ArrayList<String> lore = new ArrayList<>();
        lore.add("§3Apply Players:");
        for (int i = 0; i < Math.min(info.getPlayers().size(), 5); i++) {
            lore.add("§7 - " + info.getPlayers().get(i).getName());
        }
        if (info.getPlayers().size() > 5) {
            lore.add("§7   +(" + (info.getPlayers().size() - 5) + ")");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void changePage(Integer page) {
        this.nowPage = page;
        Inventory vv = pageInv[page];
        for (int i = 0; i < vv.getSize(); i++) {
            this.inventory.setItem(i,vv.getItem(i));
        }
    }
}
