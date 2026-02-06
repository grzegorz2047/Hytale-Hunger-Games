package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgPlayer;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacja PlayerRepository dla SQLite
 */
public class SqlitePlayerRepository implements PlayerRepository {
    private final String jdbcUrl;

    public SqlitePlayerRepository(String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public void initialize() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "player_name TEXT NOT NULL," +
                        "kills INTEGER NOT NULL DEFAULT 0," +
                        "global_kills INTEGER NOT NULL DEFAULT 0," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");");
            }
        }
    }

    @Override
    public void save(HgPlayer player) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("HgPlayer nie może być null");
        }

        // Sprawdzenie czy gracz już istnieje
        if (exists(player.getUuid())) {
            update(player);
            return;
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "INSERT INTO players (uuid, player_name, kills, global_kills) VALUES (?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUuid().toString());
                ps.setString(2, player.getPlayerName());
                ps.setInt(3, player.getKills());
                ps.setInt(4, player.getGlobalKills());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void update(HgPlayer player) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("HgPlayer nie może być null");
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "UPDATE players SET player_name = ?, kills = ?, global_kills = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getPlayerName());
                ps.setInt(2, player.getKills());
                ps.setInt(3, player.getGlobalKills());
                ps.setString(4, player.getUuid().toString());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public Optional<HgPlayer> findByUuid(UUID uuid) throws Exception {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID nie może być null");
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "SELECT uuid, player_name, kills, global_kills FROM players WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        String playerName = rs.getString("player_name");
                        int kills = rs.getInt("kills");
                        int globalKills = rs.getInt("global_kills");
                        return Optional.of(new HgPlayer(UUID.fromString(uuidStr), playerName, kills, globalKills));
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean exists(UUID uuid) throws Exception {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID nie może być null");
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID nie może być null");
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "DELETE FROM players WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        }
    }
}
