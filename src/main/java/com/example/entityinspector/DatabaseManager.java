package com.example.entityinspector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private final EntityInspector plugin;
    private Connection connection;

    public DatabaseManager(EntityInspector plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            // Ensure the plugin folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Connection string for SQLite
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
            connection = DriverManager.getConnection(url);

            // Create table
            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS inspections (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "inspector_uuid VARCHAR(36) NOT NULL," +
                        "inspector_name VARCHAR(16) NOT NULL," +
                        "entity_uuid VARCHAR(36) NOT NULL," +
                        "entity_type VARCHAR(32) NOT NULL," +
                        "entity_location VARCHAR(64) NOT NULL," +
                        "timestamp LONG NOT NULL" +
                        ");";
                statement.execute(sql);
            }

            plugin.getLogger().info("Database connected successfully.");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database!", e);
        }
    }

    public void logInspection(String inspectorUUID, String inspectorName, String entityUUID, String entityType,
            String location) {
        if (connection == null)
            return;

        String sql = "INSERT INTO inspections(inspector_uuid, inspector_name, entity_uuid, entity_type, entity_location, timestamp) VALUES(?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, inspectorUUID);
            pstmt.setString(2, inspectorName);
            pstmt.setString(3, entityUUID);
            pstmt.setString(4, entityType);
            pstmt.setString(5, location);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Could not log inspection!", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close database connection!", e);
        }
    }
}
