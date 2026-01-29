package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HgArenaTest {

    @Test
    void activeFlagAndForceStart() {
        HgArena arena = new HgArena("w1", List.of(), new Vector3d(0,0,0));

        // domyślnie nieaktywna
        assertFalse(arena.isActive());

        arena.setActive(true);
        assertTrue(arena.isActive());

        // forceStart zwraca true zgodnie z implementacją
        assertTrue(arena.forceStart());

        // ustawienie z powrotem na false
        arena.setActive(false);
        assertFalse(arena.isActive());
    }
}
