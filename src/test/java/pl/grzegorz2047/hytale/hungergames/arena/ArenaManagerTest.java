package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArenaManagerTest {

    @Test
    void createExistsAndCanBreakAndEnableBehavior() {
        ArenaManager manager = new ArenaManager();

        // przed utworzeniem - nie istnieje
        assertFalse(manager.arenaExists("world1"));
        assertTrue(manager.canBreak("world1")); // brak areny => można łamać

        // utworzenie areny
        List<Vector3d> sp = List.of(new Vector3d(0, 0, 0));
        assertTrue(manager.createArena("world1", sp, new Vector3d(0, 1, 0)));
        assertTrue(manager.arenaExists("world1"));

        // domyślnie arena nieaktywna => można łamać
        assertTrue(manager.canBreak("world1"));

        // włączenie areny => nie można łamać
        manager.setEnableArena("world1", true);
        assertFalse(manager.canBreak("world1"));

        // wyłączenie => znowu można łamać
        manager.setEnableArena("world1", false);
        assertTrue(manager.canBreak("world1"));
    }

    @Test
    void forceStartBehavior() {
        ArenaManager manager = new ArenaManager();

        // brak areny => false
        assertFalse(manager.forceStartArena("nope", context.sender()));

        // po utworzeniu => forceStart zwraca true (implementacja HgArena)
        manager.createArena("a1", List.of(new Vector3d(1,2,3)), new Vector3d(0,0,0));
        assertTrue(manager.forceStartArena("a1", context.sender()));
    }
}
