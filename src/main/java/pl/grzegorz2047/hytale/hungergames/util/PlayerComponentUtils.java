package pl.grzegorz2047.hytale.hungergames.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public final class PlayerComponentUtils {
    private PlayerComponentUtils() {
    }

    @NullableDecl
    public static Player findPlayerInPlayerComponentsBag(Store<EntityStore> store, Ref<EntityStore> refEntityStore) {
        if (store == null || refEntityStore == null) {
            return null;
        }
        return store.getComponent(refEntityStore, Player.getComponentType());
    }

    @NullableDecl
    public static PlayerRef findPlayerRefInPlayerRefComponentsBag(Store<EntityStore> store, Ref<EntityStore> refEntityStore) {
        if (store == null || refEntityStore == null) {
            return null;
        }
        return store.getComponent(refEntityStore, PlayerRef.getComponentType());
    }
}
