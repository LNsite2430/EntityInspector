package com.example.entityinspector;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityDataListener implements Listener {

    private final EntityInspector plugin;
    private final NamespacedKey spawnTimeKey;
    private final NamespacedKey spawnerKey;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Cache for Spawn Egg correlation: Thread/Tick -> Player Name
    // Using a simple tick-based variable is generally more robust than location
    // checking for simple interactions
    // because Interaction -> Spawn is synchronous in Bukkit.
    private String currentEggUser = null;

    // Cache for Command correlation: Thread/Tick -> Command Sender Name
    private String currentCommandRunner = null;

    // Inspection Cooldown: Player UUID -> Last Inspection Time (ms)
    private final Map<UUID, Long> inspectionCooldown = new HashMap<>();

    public EntityDataListener(EntityInspector plugin) {
        this.plugin = plugin;
        this.spawnTimeKey = new NamespacedKey(plugin, "spawn_time");
        this.spawnerKey = new NamespacedKey(plugin, "spawner");
    }

    // --- Command Tracking ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/summon") || msg.startsWith("/minecraft:summon")) {
            currentCommandRunner = event.getPlayer().getName();
            plugin.getServer().getScheduler().runTask(plugin, () -> currentCommandRunner = null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String msg = event.getCommand().toLowerCase();
        if (msg.startsWith("summon") || msg.startsWith("minecraft:summon")) {
            currentCommandRunner = "Console/Server";
            plugin.getServer().getScheduler().runTask(plugin, () -> currentCommandRunner = null);
        }
    }

    // --- Spawn Egg Tracking ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            Material type = event.getItem().getType();
            if (type.name().endsWith("_SPAWN_EGG")) {
                currentEggUser = event.getPlayer().getName();
                // Reset immediately after this tick handling
                plugin.getServer().getScheduler().runTask(plugin, () -> currentEggUser = null);
            }
        }
    }

    // --- Entity Placement (Boats, ArmorStand, etc - Manual) ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        Player player = event.getPlayer();

        pdc.set(spawnTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (player != null) {
            pdc.set(spawnerKey, PersistentDataType.STRING, "Placed by: " + player.getName());
        }
    }

    // --- Vehicle Creation (Boats/Minecarts - extra safety for summons) ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        Entity entity = event.getVehicle();
        processSpawnLogic(entity, null);
    }

    // --- General Spawn Logic ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        processSpawnLogic(event.getEntity(), event);
    }

    private void processSpawnLogic(Entity entity, EntitySpawnEvent event) {
        // Ignore noise
        if (entity instanceof org.bukkit.entity.Item
                || entity instanceof org.bukkit.entity.ExperienceOrb
                || entity instanceof org.bukkit.entity.FallingBlock) {
            return;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        if (!pdc.has(spawnTimeKey, PersistentDataType.LONG)) {
            pdc.set(spawnTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        }

        if (pdc.has(spawnerKey, PersistentDataType.STRING)) {
            return; // Already set
        }

        // PRIORITY 1: Command (Active Command Runner context)
        if (currentCommandRunner != null) {
            pdc.set(spawnerKey, PersistentDataType.STRING, "Command: " + currentCommandRunner);
            return;
        }

        // PRIORITY 1.5: Spawn Egg (Tick Context)
        if (currentEggUser != null) {
            pdc.set(spawnerKey, PersistentDataType.STRING, "Spawn Egg: " + currentEggUser);
            return;
        }

        // PRIORITY 2: CreatureSpawnEvent Reason check
        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent cse = (CreatureSpawnEvent) event;
            CreatureSpawnEvent.SpawnReason reason = cse.getSpawnReason();

            if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                // Should fall here if Priority 1.5 missed it, but usually standard egg use is
                // synchronous.
                // If we are here and currentEggUser is null, it might be a dispenser or other
                // plugin.
                pdc.set(spawnerKey, PersistentDataType.STRING, "Spawn Egg (Unknown/Dispenser)");
            } else if (reason == CreatureSpawnEvent.SpawnReason.COMMAND) {
                pdc.set(spawnerKey, PersistentDataType.STRING, "Command (Unknown Source)");
            } else if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
                pdc.set(spawnerKey, PersistentDataType.STRING, "Custom/Plugin");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player) {
            Player breeder = (Player) event.getBreeder();
            PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
            pdc.set(spawnerKey, PersistentDataType.STRING, "Breeder: " + breeder.getName());
        }
    }

    // --- Inspection Logic ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        performInspection(event.getPlayer(), event.getRightClicked(), event.getHand());
        if (plugin.isInspector(event.getPlayer().getUniqueId())
                && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        performInspection(event.getPlayer(), event.getRightClicked(), event.getHand());
        if (plugin.isInspector(event.getPlayer().getUniqueId())
                && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
            event.setCancelled(true);
        }
    }

    private void performInspection(Player player, Entity entity, org.bukkit.inventory.EquipmentSlot hand) {
        if (hand != org.bukkit.inventory.EquipmentSlot.HAND)
            return;

        if (plugin.isInspector(player.getUniqueId())) {

            if (entity instanceof ComplexEntityPart) {
                entity = ((ComplexEntityPart) entity).getParent();
            }

            long now = System.currentTimeMillis();
            if (inspectionCooldown.containsKey(player.getUniqueId())) {
                if (now - inspectionCooldown.get(player.getUniqueId()) < 500)
                    return;
            }
            inspectionCooldown.put(player.getUniqueId(), now);

            PersistentDataContainer pdc = entity.getPersistentDataContainer();

            player.sendMessage(ChatColor.GOLD + "--- Entity Inspection ---");
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + entity.getType());
            player.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + entity.getUniqueId());

            if (pdc.has(spawnTimeKey, PersistentDataType.LONG)) {
                long time = pdc.get(spawnTimeKey, PersistentDataType.LONG);
                player.sendMessage(
                        ChatColor.YELLOW + "Spawn Time: " + ChatColor.WHITE + dateFormat.format(new Date(time)));
            } else {
                player.sendMessage(ChatColor.YELLOW + "Spawn Time: " + ChatColor.GRAY + "Unknown/Pre-plugin");
            }

            if (pdc.has(spawnerKey, PersistentDataType.STRING)) {
                String spawner = pdc.get(spawnerKey, PersistentDataType.STRING);
                player.sendMessage(ChatColor.YELLOW + "Spawner: " + ChatColor.WHITE + spawner);
            } else {
                player.sendMessage(ChatColor.YELLOW + "Spawner: " + ChatColor.GRAY + "Natural / Unknown");
            }
            player.sendMessage(ChatColor.GOLD + "-------------------------");

            // Log to Database
            plugin.getDatabaseManager().logInspection(
                    player.getUniqueId().toString(),
                    player.getName(),
                    entity.getUniqueId().toString(),
                    entity.getType().toString(),
                    entity.getLocation().toString());
        }
    }
}
