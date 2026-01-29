package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HgArena {
    private final String worldName;
    private final List<Vector3d> spawnPoints;
    private final Vector3d lobbySpawnLocation;
    private int minimumStartArenaPlayersNumber = 2;
    private boolean isArenaActive = false;
    private List<UUID> activePlayers = new ArrayList<>();

    public HgArena(String worldName, List<Vector3d> spawnPoints, Vector3d lobbySpawnLocation) {
        this.worldName = worldName;
        this.spawnPoints = spawnPoints;
        this.lobbySpawnLocation = lobbySpawnLocation;

    }


    public boolean forceStart() {
        this.teleportPlayersToTheSpawnPoints();

        // Pobierz world przez Universe.get().getWorld(worldName) i w world.execute wykonaj poniższy fragment
        //
        // Przykładowe umiejscowienie (pseudokod — dostosuj nazwy referencji graczy oraz tracking):
        //
        // World world = Universe.get().getWorld(worldName);
        // if (world != null) {
        //     // dla każdego gracza w arenie (lista playerRef powinna być dostępna w obiekcie Areny)
        //     for (PlayerTracking tracking : this.playerTrackings) {
        //         Ref<EntityStore> playerRef = tracking.playerRef;
        //         world.execute(() -> {
        //             try {
        //                 // Get entity store to check position
        //                 Store<EntityStore> store = world.getEntityStore().getStore();
        //                 if (store == null) return;
        //
        //                 Ref<EntityStore> entityRef = playerRef.getReference();
        //                 if (entityRef == null || !entityRef.isValid()) return;
        //
        //                 TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        //                 if (transform == null) return;
        //
        //                 Vector3d position = transform.getPosition();
        //                 int blockX = (int) Math.floor(position.getX());
        //                 int blockY = (int) Math.floor(position.getY());
        //                 int blockZ = (int) Math.floor(position.getZ());
        //
        //                 // Check if player moved to a new block
        //                 boolean hasMoved = blockX != tracking.lastBlockX ||
        //                                   blockY != tracking.lastBlockY ||
        //                                   blockZ != tracking.lastBlockZ;
        //
        //                 if (hasMoved && !this.isArenaActive) {
        //                     // Zablokuj ruch: np. teleportuj gracza z powrotem na ostatnią pozycję
        //                     // lub ustaw transform na poprzednie koordynaty — implementacja zależy od API
        //                 } else {
        //                     // zaktualizuj tracking.lastBlockX/Y/Z
        //                 }
        //
        //             } catch (Throwable t) {
        //                 // loguj błąd
        //             }
        //         });
        //     }
        // }
        //
        // Uwaga: powyższy fragment musi być dostosowany do tego, jak przechowujesz referencje graczy (playerRef)
        // i jak chcesz ich "zamrażać" (teleportacja do ostatniej pozycji, blokada kontroli itp.).
        // --- KONIEC MIEJSCA NA world.execute(...) ---
        //
        // Przykładowe umiejscowienie (pseudokod — dostosuj nazwy referencji graczy oraz tracking):
        //
        // World world = Universe.get().getWorld(worldName);
        // if (world != null) {
        //     // dla każdego gracza w arenie (lista playerRef powinna być dostępna w obiekcie Areny)
        //     for (PlayerTracking tracking : this.playerTrackings) {
        //         Ref<EntityStore> playerRef = tracking.playerRef;
        //         world.execute(() -> {
        //             try {
        //                 // Get entity store to check position
        //                 Store<EntityStore> store = world.getEntityStore().getStore();
        //                 if (store == null) return;
        //
        //                 Ref<EntityStore> entityRef = playerRef.getReference();
        //                 if (entityRef == null || !entityRef.isValid()) return;
        //
        //                 TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        //                 if (transform == null) return;
        //
        //                 Vector3d position = transform.getPosition();
        //                 int blockX = (int) Math.floor(position.getX());
        //                 int blockY = (int) Math.floor(position.getY());
        //                 int blockZ = (int) Math.floor(position.getZ());
        //
        //                 // Check if player moved to a new block
        //                 boolean hasMoved = blockX != tracking.lastBlockX ||
        //                                   blockY != tracking.lastBlockY ||
        //                                   blockZ != tracking.lastBlockZ;
        //
        //                 if (hasMoved && !this.isArenaActive) {
        //                     // Zablokuj ruch: np. teleportuj gracza z powrotem na ostatnią pozycję
        //                     // lub ustaw transform na poprzednie koordynaty — implementacja zależy od API
        //                 } else {
        //                     // zaktualizuj tracking.lastBlockX/Y/Z
        //                 }
        //
        //             } catch (Throwable t) {
        //                 // loguj błąd
        //             }
        //         });
        //     }
        // }
        //
        // Uwaga: powyższy fragment musi być dostosowany do tego, jak przechowujesz referencje graczy (playerRef)
        // i jak chcesz ich "zamrażać" (teleportacja do ostatniej pozycji, blokada kontroli itp.).
        // --- KONIEC MIEJSCA NA world.execute(...) ---

        return true;
    }

    private void teleportPlayersToTheSpawnPoints() {

    }

    public boolean isActive() {
        return isArenaActive;
    }

    public void setActive(boolean value) {
        this.isArenaActive = value;
    }

    // --- DODANE GETTERY DLA PERSYSTENCJI ---
    public String getWorldName() {
        return worldName;
    }

    public List<Vector3d> getSpawnPoints() {
        return spawnPoints;
    }

    public Vector3d getLobbySpawnLocation() {
        return lobbySpawnLocation;
    }

    public void join(Player player) {
        assert player.getWorld() != null;
        player.getWorld().execute(() -> {
            assert player.getReference() != null;
            World world = Universe.get().getWorld(this.worldName);
            addTeleportTask(
                    player.getReference(),
                    world,
                    this.lobbySpawnLocation
            );
        });
    }

    private void addTeleportTask(
            Ref<EntityStore> playerRef,
            World world,
            Vector3d spawnCoordPos
    ) {
        Store<EntityStore> store = playerRef.getStore();
        ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
        Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
        store.addComponent(playerRef, componentType, teleport);
    }
}