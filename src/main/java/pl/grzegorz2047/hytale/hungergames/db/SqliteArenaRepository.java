package pl.grzegorz2047.hytale.hungergames.db;

import com.hypixel.hytale.math.vector.Vector3d;
import pl.grzegorz2047.hytale.hungergames.arena.HgArena;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SQLite repozytorium (plik DB)
public class SqliteArenaRepository implements ArenaRepository {
    private final String dbPath;
    private final String jdbcUrl;

    public SqliteArenaRepository(String dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public void initialize() throws Exception {
        // Upewnij się, że katalog istnieje
        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS arenas (" +
                        "world TEXT PRIMARY KEY," +
                        "active INTEGER," +
                        "lobbyX INTEGER," +
                        "lobbyY INTEGER," +
                        "lobbyZ INTEGER," +
                        "spawnpoints TEXT" +
                        ");");
            }
        }
    }

    @Override
    public void save(HgArena arena) throws Exception {
        String sp = serializeSpawnPoints(arena.getSpawnPoints());
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "REPLACE INTO arenas (world, active, lobbyX, lobbyY, lobbyZ, spawnpoints) VALUES (?,?,?,?,?,?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, arena.getWorldName());
                ps.setInt(2, arena.isActive() ? 1 : 0);
                Vector3d lobby = arena.getLobbySpawnLocation();
                ps.setDouble(3, lobby != null ? lobby.getX() : 0);
                ps.setDouble(4, lobby != null ? lobby.getY() : 0);
                ps.setDouble(5, lobby != null ? lobby.getZ() : 0);
                ps.setString(6, sp);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public Map<String, HgArena> loadAll() throws Exception {
        Map<String, HgArena> result = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String sql = "SELECT world, active, lobbyX, lobbyY, lobbyZ, spawnpoints FROM arenas;";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int active = rs.getInt("active");
                    int lx = rs.getInt("lobbyX");
                    int ly = rs.getInt("lobbyY");
                    int lz = rs.getInt("lobbyZ");
                    String sp = rs.getString("spawnpoints");
                    List<Vector3d> spawnPoints = deserializeSpawnPoints(sp);
                    HgArena arena = new HgArena(world, spawnPoints, new Vector3d(lx, ly, lz));
                    arena.setActive(active == 1);
                    result.put(world, arena);
                }
            }
        }
        return result;
    }

    // Prosty format serializacji spawn points: "x,y,z;x,y,z;..."
    private static String serializeSpawnPoints(List<Vector3d> spawnPoints) {
        if (spawnPoints == null || spawnPoints.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Vector3d p : spawnPoints) {
            if (sb.length() > 0) sb.append(';');
            sb.append(p.getX()).append(',').append(p.getY()).append(',').append(p.getZ());
        }
        return sb.toString();
    }

    private static List<Vector3d> deserializeSpawnPoints(String s) {
        List<Vector3d> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        String[] parts = s.split(";");
        for (String part : parts) {
            String[] nums = part.split(",");
            if (nums.length != 3) continue;
            try {
                int x = Integer.parseInt(nums[0]);
                int y = Integer.parseInt(nums[1]);
                int z = Integer.parseInt(nums[2]);
                out.add(new Vector3d(x, y, z));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }
}