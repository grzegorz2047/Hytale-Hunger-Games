package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgArena;

import java.util.HashMap;
import java.util.Map;

// Proste in-memory repo (fallback i ułatwienie testów)
public class InMemoryRepository implements ArenaRepository {
    private final Map<String, HgArena> map = new HashMap<>();

    @Override
    public void save(HgArena arena) {
        map.put(arena.getWorldName(), arena);
    }

    @Override
    public Map<String, HgArena> loadAll() {
        // Zwracamy kopię, aby uniknąć modyfikacji zewnętrznych
        return new HashMap<>(map);
    }
}