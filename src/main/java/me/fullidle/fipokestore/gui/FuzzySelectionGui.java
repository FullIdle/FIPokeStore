package me.fullidle.fipokestore.gui;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import lombok.Getter;
import me.fullidle.ficore.ficore.common.api.ineventory.ListenerInvHolder;
import me.fullidle.fipokestore.MyListener;
import me.fullidle.fipokestore.common.PokeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static me.fullidle.fipokestore.Main.getConfigColorMsg;
import static me.fullidle.fipokestore.Main.pokeData;

@Getter
public class FuzzySelectionGui extends MultiPageInv {
    private final Inventory inv;
    private final Player player;
    private final Map<ItemStack,EnumSpecies> map = new HashMap<>();
    private final Inventory[] pageInv;
    private Integer nowPage;

    public FuzzySelectionGui(Player player, EnumSpecies... enumSpecies){
        int i = enumSpecies.length / 9 + (enumSpecies.length % 9 > 0 ? 1 : 0);
        boolean b = i > 6;
        int size = b?54:i*9;
        this.player = player;
        this.inv = Bukkit.createInventory(this,size,"§3§lSearchResults");

        ArrayList<ItemStack> itemList = new ArrayList<>();
        for (EnumSpecies sp : enumSpecies) {
            Pokemon pokemon = Pixelmon.pokemonFactory.create(sp);
            ItemStack photoItem = PokeInfoGui.getPhotoItem(pokemon, player);
            map.put(photoItem,sp);
            itemList.add(photoItem);
        }
        pageInv = pagination(b?size-9:size,itemList);
        if (pageInv.length > 1) {
            //翻页控制台 下页 上页
            ItemStack item = new ItemStack(Material.ARROW);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§3PreviousPage");
            item.setItemMeta(meta);
            inv.setItem(47,item);
            meta.setDisplayName("§3NextPage");
            item.setItemMeta(meta);
            inv.setItem(51,item);
        }
        changePage(0);

        onClick(e-> {
            e.setCancelled(true);
            if (!e.getClickedInventory().equals(this.inv)){
                return;
            }
            ItemStack currentItem = e.getCurrentItem();
            System.out.println(currentItem);
            if (currentItem == null||currentItem.getType().equals(Material.AIR)) {
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

            EnumSpecies spe = map.get(currentItem);

            //判断可否购买
            if (!player.isOp()) {
                PokeData.PokeInfo value = pokeData.getPokemonPokeInfo(spe);
                if (!value.isCanBuy()) {
                    //提示不可购买!
                    player.sendMessage(getConfigColorMsg("Msg.pokeCanTBuy"));
                    return;
                }
            }
            PokeInfoGui gui = new PokeInfoGui(this.player, spe);
            this.player.closeInventory();
            this.player.openInventory(gui.getInventory());
        });
        onDrag(e->e.setCancelled(true));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void changePage(Integer page) {
        this.nowPage = page;
        Inventory vv = pageInv[page];
        for (int i = 0; i < vv.getSize(); i++) {
            this.inv.setItem(i,vv.getItem(i));
        }
    }
}
