package pl.grzegorz2047.hytale.hungergames.db;

import pl.grzegorz2047.hytale.hungergames.arena.HgArena;

import java.util.Map;

public interface ArenaRepository {
    default void initialize() throws Exception {}
    void save(HgArena arena) throws Exception;
    Map<String, HgArena> loadAll() throws Exception;

    /**
     * Załaduj wszystkie areny z podanym PlayerRepository
     */
    default Map<String, HgArena> loadAll(PlayerRepository playerRepository) throws Exception {
        return loadAll();
    }
}
