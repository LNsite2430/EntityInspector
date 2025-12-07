package com.example.entityinspector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InspectorCommand implements CommandExecutor {

    private final EntityInspector plugin;

    public InspectorCommand(EntityInspector plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && (args[0].equalsIgnoreCase("inspect") || args[0].equalsIgnoreCase("i"))) {
            plugin.toggleInspector(player.getUniqueId());

            if (plugin.isInspector(player.getUniqueId())) {
                player.sendMessage(
                        ChatColor.GREEN + "Entity Inspector Mode ENABLED. Interact with an entity to check its data.");
            } else {
                player.sendMessage(ChatColor.RED + "Entity Inspector Mode DISABLED.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /ec <inspect|i>");
        }

        return true;
    }
}
