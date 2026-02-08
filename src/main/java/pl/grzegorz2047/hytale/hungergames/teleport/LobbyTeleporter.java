package pl.grzegorz2047.hytale.hungergames.teleport;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Objects;

public final class LobbyTeleporter {
    private LobbyTeleporter() {
    }

    public static void teleportToLobby(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> reference;
        try {
            reference = playerRef.getReference();
            if (reference == null) return;
            World defaultWorld = Objects.requireNonNull(Universe.get().getDefaultWorld());
            Transform spawnPoint = getTransform(defaultWorld, reference);
            if (spawnPoint == null) return;
            Vector3d position = spawnPoint.getPosition();
            addTeleportTask(reference, defaultWorld, position);
        } catch (IllegalStateException ise) {
            HytaleLogger.getLogger().atWarning().withCause(ise)
                    .log( "Invalid entity reference in teleportToLobby: %s"  , ise.getMessage());
        } catch (Throwable t) {
            HytaleLogger.getLogger().atWarning().withCause(t)
                    .log("Error while teleporting to lobby: %s", t.getMessage());
        }
    }

    public static Transform getTransform(World defaultWorld, Ref<EntityStore> reference) {
        ISpawnProvider spawnProvider = defaultWorld.getWorldConfig().getSpawnProvider();
        Store<EntityStore> store = reference.getStore();
        Transform spawnPoint = spawnProvider.getSpawnPoint(reference, store);
        return spawnPoint;
    }

    private static void addTeleportTask(Ref<EntityStore> playerRef, World world, Vector3d spawnCoordPos) {
        try {
            Store<EntityStore> store = playerRef.getStore();
            ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
            Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
            store.addComponent(playerRef, componentType, teleport);
        } catch (IllegalStateException ise) {
            HytaleLogger.getLogger().atWarning().withCause(ise)
                    .log("Invalid entity reference when adding teleport: %s", ise.getMessage());
        } catch (Throwable t) {
            HytaleLogger.getLogger().atWarning().withCause(t)
                    .log("Failed to add teleport task: %s", t.getMessage());
        }
    }
}
