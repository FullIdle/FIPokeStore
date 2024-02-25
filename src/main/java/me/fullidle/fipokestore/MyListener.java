package me.fullidle.fipokestore;

import com.pixelmonmod.pixelmon.api.events.dialogue.DialogueInputEvent;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import me.fullidle.ficore.ficore.common.api.event.ForgeEvent;
import me.fullidle.fipokestore.common.Pair;
import me.fullidle.fipokestore.common.PokeData;
import me.fullidle.fipokestore.enums.EnumInputType;
import me.fullidle.fipokestore.gui.ApplyListGui;
import me.fullidle.fipokestore.gui.PokeInfoGui;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.fullidle.fipokestore.Main.*;

public class MyListener implements Listener {
    public static Map<Player, EnumInputType> inputTypeMap = new HashMap<>();
    public static Map<Player, Object> someData = new HashMap<>();

    //确保缓存清理
    @EventHandler
    public void quit(PlayerQuitEvent e) {
        inputTypeMap.remove(e.getPlayer());
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        //给op提示申请,对权限op可没用
        Player player = e.getPlayer();
        if (!player.isOp()) {
            return;
        }
        //获取申请列表内第一个空位,如果是0那么就是不存在申请
        ApplyListGui gui = new ApplyListGui();
        if (gui.getInventory().firstEmpty() == 0) {
            return;
        }
        BaseComponent[] button = new ComponentBuilder("§a[Open ApplyListGui]").
                event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fipstore applylist")).
                event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new ComponentBuilder("§aClick to open!").create())).
                create();
        BaseComponent[] msg = new ComponentBuilder("§3You have a Pokémon store configuration request!").
                append(button).create();
        player.spigot().sendMessage(msg);
    }

    @EventHandler
    public void forge(ForgeEvent event) {
        if (event.getForgeEvent() instanceof DialogueInputEvent.Submitted) {
            DialogueInputEvent.Submitted e = (DialogueInputEvent.Submitted) event.getForgeEvent();
            Player player = Bukkit.getPlayer(e.getPlayer().func_110124_au());
            String inputText = e.getInput();
            //判断是否是对应的输入框|清楚记录(这个事件触发关闭事件就不会主动触发了!)
            if (!inputTypeMap.containsKey(player)) {
                return;
            }
            EnumInputType inputType = inputTypeMap.get(player);
            inputTypeMap.remove(player);
            switch (inputType) {
                case SEARCH: {
                    //搜索
                    //获取精灵Species
                    EnumSpecies species = EnumSpecies.getFromNameAnyCase(inputText);
                    if (species == null) {
                        player.sendMessage(getConfigColorMsg("Msg.incorrectName"));
                        return;
                    }
                    //检查是否能买 OP跳过可强行打开
                    if (!player.isOp()) {
                        PokeData.PokeInfo value = pokeData.getPokemonPokeInfo(species);
                        if (!value.isCanBuy()) {
                            //提示不可购买!
                            player.sendMessage("§cPoke " + species.getLocalizedName() + " Not available for purchase!");
                            return;
                        }
                    }
                    //打开GUI
                    PokeInfoGui gui = new PokeInfoGui(player, species);
                    Bukkit.getScheduler().runTask(main, () -> {
                        Main.inputCache.put(player, inputText);
                        player.openInventory(gui.getInventory());
                    });
                    return;
                }
                case PAYTYPE: {
                    //输入支付格式
                    if (!getAllMoneyTypeList().contains(inputText)) {
                        player.sendMessage(getConfigColorMsg("Msg.currencyNotExist"));
                        inputTypeMap.put(player, EnumInputType.PAYTYPE);
                        Bukkit.getScheduler().runTask(main, () -> {
                            payTypeInput.sendTo(e.getPlayer());
                        });
                        return;
                    }
                    ((Pair<PokeData.PokeInfo, String>) someData.get(player)).setValue(inputText);
                    inputTypeMap.put(player, EnumInputType.PAYVALUE);
                    Bukkit.getScheduler().runTask(main, () -> {
                        payValueInput.sendTo(e.getPlayer());
                    });
                    return;
                }
                case PAYVALUE: {
                    //输入支付的量
                    try {
                        double value = Double.parseDouble(inputText);
                        Pair<PokeData.PokeInfo, String> pair = (Pair<PokeData.PokeInfo, String>) someData.get(player);
                        PokeData.PokeInfo key = pair.getKey();
                        String type = pair.getValue();
                        key.setPayType(type);
                        key.setValue(value);
                        pokeData.setPokemonPokeInfo(key.getSpecies(), key);
                        player.sendMessage(getConfigColorMsg("Msg.editedSuccessfully"));
                        Inventory inv = new PokeInfoGui(player, key.getSpecies()).getInventory();
                        Bukkit.getScheduler().runTask(main, () -> {
                            player.openInventory(inv);
                        });
                    } catch (NumberFormatException ex) {
                        player.sendMessage(getConfigColorMsg("Msg.enterNonNumber"));
                        e.setCanceled(true);
                    }
                    return;
                }
            }
        }
        //清楚记录
        if (event.getForgeEvent() instanceof DialogueInputEvent.ClosedScreen) {
            DialogueInputEvent.ClosedScreen e = (DialogueInputEvent.ClosedScreen) event.getForgeEvent();
            UUID uuid = e.getPlayer().func_110124_au();
            Player player = Bukkit.getPlayer(uuid);
            {
                Object o = someData.get(player);
                if (o instanceof Pair) {
                    PokeData.PokeInfo info = (PokeData.PokeInfo) ((Pair<?, ?>) o).getKey();
                    Inventory inv = new PokeInfoGui(player, info.getSpecies()).getInventory();
                    Bukkit.getScheduler().runTask(main,()->player.openInventory(inv));
                }
            }
            inputTypeMap.remove(player);
            someData.remove(player);
        }
    }
}
