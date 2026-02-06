package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory implementacja PlayerRepository dla testów i fallback'u
 */
public class InMemoryPlayerRepository implements PlayerRepository {
    private final Map<UUID, HgPlayer> players = new HashMap<>();

    @Override
    public void save(HgPlayer player) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("HgPlayer nie może być null");
        }
        players.put(player.getUuid(), player);
    }

    @Override
    public void update(HgPlayer player) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("HgPlayer nie może być null");
        }
        if (!players.containsKey(player.getUuid())) {
            throw new IllegalArgumentException("Gracz nie istnieje: " + player.getUuid());
        }
        players.put(player.getUuid(), player);
    }

    @Override
    public Optional<HgPlayer> findByUuid(UUID uuid) throws Exception {
        return Optional.ofNullable(players.get(uuid));
    }

    @Override
    public boolean exists(UUID uuid) throws Exception {
        return players.containsKey(uuid);
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        players.remove(uuid);
    }
}
