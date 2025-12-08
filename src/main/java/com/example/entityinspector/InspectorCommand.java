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

                // Helper record to store chunk data
                class ChunkData {
                    org.bukkit.Chunk chunk;
                    int nonItemEntityCount;
                    int itemCount;
                    double totalY;

                    ChunkData(org.bukkit.Chunk chunk, int nonItemEntityCount, int itemCount, double totalY) {
                        this.chunk = chunk;
                        this.nonItemEntityCount = nonItemEntityCount;
                        this.itemCount = itemCount;
                        this.totalY = totalY;
                    }
                }

                java.util.List<ChunkData> chunkDataList = new java.util.ArrayList<>();

                for (org.bukkit.Chunk chunk : player.getWorld().getLoadedChunks()) {
                    int nonItemCount = 0;
                    int items = 0;
                    double tY = 0;

                    if (chunk.getEntities().length > 0) {
                        for (org.bukkit.entity.Entity e : chunk.getEntities()) {
                            tY += e.getLocation().getY();
                            if (e instanceof org.bukkit.entity.Item) {
                                items++;
                            } else {
                                nonItemCount++;
                            }
                        }
                        chunkDataList.add(new ChunkData(chunk, nonItemCount, items, tY));
                    }
                }

                // 1. Sort and display by Non-Item Entities
                chunkDataList.sort((c1, c2) -> Integer.compare(c2.nonItemEntityCount, c1.nonItemEntityCount));

                player.sendMessage(ChatColor.GOLD + "=== Top Entity Density Chunks (Non-Items) ===");
                int count = 0;
                for (ChunkData data : chunkDataList) {
                    if (count >= 10)
                        break;
                    if (data.nonItemEntityCount > 0) {
                        org.bukkit.Chunk c = data.chunk;
                        int blockX = c.getX() * 16;
                        int blockZ = c.getZ() * 16;
                        int avgY = (int) (data.totalY / (data.nonItemEntityCount + data.itemCount)); // Avg Y of all
                                                                                                     // entities for
                                                                                                     // rough location

                        player.sendMessage(ChatColor.AQUA + "Chunk[" + c.getX() + ", " + c.getZ() + "]"
                                + ChatColor.GRAY + " (x: " + blockX + ", y: " + avgY + ", z: " + blockZ + "): "
                                + ChatColor.WHITE + data.nonItemEntityCount + " entities");
                        count++;
                    }
                }
                if (count == 0) {
                    player.sendMessage(ChatColor.GRAY + "No loaded chunks with non-item entities found.");
                }

                // 2. Sort and display by Item Drops
                chunkDataList.sort((c1, c2) -> Integer.compare(c2.itemCount, c1.itemCount));

                player.sendMessage(ChatColor.GOLD + "=== Top Item Drop Density Chunks ===");
                count = 0;
                for (ChunkData data : chunkDataList) {
                    if (count >= 10)
                        break;
                    if (data.itemCount > 0) {
                        org.bukkit.Chunk c = data.chunk;
                        int blockX = c.getX() * 16;
                        int blockZ = c.getZ() * 16;
                        int avgY = (int) (data.totalY / (data.nonItemEntityCount + data.itemCount));

                        player.sendMessage(ChatColor.AQUA + "Chunk[" + c.getX() + ", " + c.getZ() + "]"
                                + ChatColor.GRAY + " (x: " + blockX + ", y: " + avgY + ", z: " + blockZ + "): "
                                + ChatColor.YELLOW + data.itemCount + " items");
                        count++;
                    }
                }
                if (count == 0) {
                    player.sendMessage(ChatColor.GRAY + "No loaded chunks with item drops found.");
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
