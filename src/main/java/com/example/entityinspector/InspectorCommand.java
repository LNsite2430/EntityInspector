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
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("inspect") || subCommand.equals("i")) {
                plugin.toggleInspector(player.getUniqueId());

                if (plugin.isInspector(player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN
                            + "Entity Inspector Mode ENABLED. Interact with an entity to check its data.");
                } else {
                    player.sendMessage(ChatColor.RED + "Entity Inspector Mode DISABLED.");
                }
            } else if (subCommand.equals("search") || subCommand.equals("s")) {
                player.sendMessage(ChatColor.YELLOW + "Searching for high density chunks...");

                java.util.List<org.bukkit.Chunk> chunks = new java.util.ArrayList<>();
                for (org.bukkit.Chunk chunk : player.getWorld().getLoadedChunks()) {
                    chunks.add(chunk);
                }

                chunks.sort((c1, c2) -> Integer.compare(
                        c2.getEntities().length,
                        c1.getEntities().length));

                int count = 0;
                player.sendMessage(ChatColor.GOLD + "=== Top Entity Density Chunks ===");
                for (org.bukkit.Chunk chunk : chunks) {
                    if (count >= 10)
                        break;
                    int entityCount = chunk.getEntities().length;
                    if (entityCount > 0) {
                        int blockX = chunk.getX() * 16;
                        int blockZ = chunk.getZ() * 16;

                        double totalY = 0;
                        int itemCount = 0;
                        for (org.bukkit.entity.Entity e : chunk.getEntities()) {
                            totalY += e.getLocation().getY();
                            if (e instanceof org.bukkit.entity.Item) {
                                itemCount++;
                            }
                        }
                        int avgY = (int) (totalY / entityCount);

                        player.sendMessage(ChatColor.AQUA + "Chunk[" + chunk.getX() + ", " + chunk.getZ() + "]"
                                + ChatColor.GRAY + " (x: " + blockX + ", y: " + avgY + ", z: " + blockZ + "): "
                                + ChatColor.WHITE + entityCount + " entities "
                                + ChatColor.YELLOW + "(Items: " + itemCount + ")");
                        count++;
                    }
                }
                if (count == 0) {
                    player.sendMessage(ChatColor.GRAY + "No loaded chunks with entities found.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /ec <inspect|i|search|s>");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /ec <inspect|i|search|s>");
        }

        return true;
    }
}
