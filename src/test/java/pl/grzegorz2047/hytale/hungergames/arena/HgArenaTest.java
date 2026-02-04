package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HgArenaTest {

    private static final class TestArena extends HgArena {
        private boolean teleportCalled;

        private TestArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d lobbySpawnLocation, MainConfig config) {
            super(worldName, playerSpawnPoints, lobbySpawnLocation, config, false);
        }

        @Override
        protected void startGame() {
            // no-op for tests to avoid touching Hytale runtime
        }

        @Override
        protected void teleportPlayersToTheSpawnPoints(List<Vector3d> spawnPoints) {
            teleportCalled = true;
        }
    }

    @Test
    void activeFlagAndForceStart() {
        TestArena arena = new TestArena("w1", List.of(), new Vector3d(0,0,0), new MainConfig());

        // domyślnie nieaktywna
        assertFalse(arena.isActive());

        arena.setActive(true);
        assertTrue(arena.isActive());

        // forceStart zwraca true zgodnie z implementacją
        assertTrue(arena.forceStart());
        assertTrue(arena.teleportCalled);

        // ustawienie z powrotem na false
        arena.setActive(false);
        assertFalse(arena.isActive());
    }
}
