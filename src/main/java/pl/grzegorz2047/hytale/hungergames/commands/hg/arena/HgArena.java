package pl.grzegorz2047.hytale.hungergames.commands.hg.arena;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.List;

public class HgArena {
    private final String worldName;
    private final List<Vector3i> spawnPoints;
    private final Vector3i lobbySpawnLocation;
    private int minimumStartArenaPlayersNumber = 2;
    private boolean isArenaActive = false;

    public HgArena(String worldName, List<Vector3i> spawnPoints, Vector3i lobbySpawnLocation) {
        this.worldName = worldName;
        this.spawnPoints = spawnPoints;
        this.lobbySpawnLocation = lobbySpawnLocation;
    }

    public boolean forceStart() {
        this.teleportPlayersToTheSpawnPoints();

        return true;
    }

    private void teleportPlayersToTheSpawnPoints() {

    }
}
