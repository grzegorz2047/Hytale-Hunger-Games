package pl.grzegorz2047.hytale.hungergames.commands.hg.arena;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.HashMap;
import java.util.List;

public class ArenaManager {
    HashMap<String, HgArena> listOfArenas = new HashMap<>();

    public boolean createArena(String worldName, List<Vector3i> spawnPoints, Vector3i lobbySpawnLocation) {
        this.listOfArenas.put(worldName, new HgArena(worldName, spawnPoints, lobbySpawnLocation));
        return true;
    }

    public boolean forceStartArena(String arenaName) {
        if (!this.listOfArenas.containsKey(arenaName)) {
            return false;
        }
        HgArena hgArena = this.listOfArenas.get(arenaName);
        return hgArena.forceStart();
    }
}
