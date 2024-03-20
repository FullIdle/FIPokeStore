package me.fullidle.fipokestore.gui;

import me.fullidle.ficore.ficore.common.api.ineventory.ListenerInvHolder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;

public abstract class MultiPageInv extends ListenerInvHolder {
    public Inventory[] pagination(Integer pageSize, Collection<ItemStack> itemStacks){
        ArrayList<Inventory> inventories = new ArrayList<>();
        int cycle = 0;
        Inventory pageInv = Bukkit.createInventory(null,pageSize);
        for (ItemStack itemStack : itemStacks) {
            pageInv.addItem(itemStack);
            cycle++;
            if (cycle == pageSize) {
                inventories.add(pageInv);
                pageInv = Bukkit.createInventory(null, pageSize);
                cycle = 0;
            }
        }
        if (inventories.isEmpty()||inventories.get(inventories.size()-1) != pageInv){
            inventories.add(pageInv);
        }
        return inventories.toArray(new Inventory[0]);
    }
}
