package com.example.entityinspector;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EntityInspector extends JavaPlugin {

    private static EntityInspector instance;
    private final Set<UUID> inspectors = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        // Register commands and events
        getCommand("ec").setExecutor(new InspectorCommand(this));
        getServer().getPluginManager().registerEvents(new EntityDataListener(this), this);
        getLogger().info("EntityInspector has been enabled!");
    }

    @Override
    public void onDisable() {
        inspectors.clear();
        getLogger().info("EntityInspector has been disabled!");
    }

    public static EntityInspector getInstance() {
        return instance;
    }

    public boolean isInspector(UUID playerUUID) {
        return inspectors.contains(playerUUID);
    }

    public void toggleInspector(UUID playerUUID) {
        if (inspectors.contains(playerUUID)) {
            inspectors.remove(playerUUID);
        } else {
            inspectors.add(playerUUID);
        }
    }
}
