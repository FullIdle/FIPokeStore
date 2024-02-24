package me.fullidle.fipokestore;

import com.google.common.collect.Lists;
import me.fullidle.fipokestore.enums.EnumInputType;
import me.fullidle.fipokestore.gui.ApplyListGui;
import net.minecraft.entity.player.EntityPlayerMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CMD implements CommandExecutor, TabCompleter {
    public static final ArrayList<String> subCmd = Lists.newArrayList(
            "reload", "help", "open", "applylist"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            String arg = args[0];
            if (subCmd.contains(arg.toLowerCase())) {
                switch (arg) {
                    case "help": {
                        break;
                    }
                    case "reload": {
                        Main.main.reloadConfig();
                        sender.sendMessage("§aConfiguration has been reloaded!");
                        return false;
                    }
                    case "open": {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage("§cYou are not a player and cannot use this command!");
                            break;
                        }
                        Player player = (Player) sender;
                        {
                            //打开聊天框
                            if (Main.inputCache.containsKey(player)) {
                                Main.searchInput.setDefaultText(Main.inputCache.get(player));
                            }
                            MyListener.inputTypeMap.put(player, EnumInputType.SEARCH);
                            Main.searchInput.sendTo((EntityPlayerMP) ((Object) ((CraftPlayer) player).getHandle()));
                        }
                        return false;
                    }
                    case "applylist": {
                        if (!(sender instanceof Player) || !sender.isOp()) {
                            sender.sendMessage("§cYou are not OP!");
                            return false;
                        }
                        Player player = (Player) sender;
                        player.openInventory(new ApplyListGui().getInventory());
                        return false;
                    }
                }
            }
        }
        ArrayList<String> clone = (ArrayList<String>) subCmd.clone();
        clone.add("字面意思Help我懒得写");
        sender.sendMessage(clone.toArray(new String[0]));
        return false;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return subCmd;
        if (args.length == 1) return subCmd.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        return null;
    }
}
