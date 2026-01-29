package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgArena;

import java.util.Map;

public interface ArenaRepository {
    default void initialize() throws Exception {}
    void save(HgArena arena) throws Exception;
    Map<String, HgArena> loadAll() throws Exception;
}
