package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HgArena {
    private final String worldName;
    private final List<Vector3d> spawnPoints;
    private final Vector3d lobbySpawnLocation;
    private int minimumStartArenaPlayersNumber = 2;
    private boolean isArenaEnabled = false;
    private int deathmatchArenaSeconds = 30;
    // Odliczanie / stan oczekiwania
    private final int startingArenaSeconds = 10; // domyślne odliczanie
    private final int ingameArenaSeconds = 30; // domyślne odliczanie
    private int currentCountdown = startingArenaSeconds;

    public int getArenaSize() {
        return this.spawnPoints.size();
    }

    public enum GameState {WAITING, STARTING, INGAME_MAIN_PHASE, INGAME_DEATHMATCH_PHASE, RESTARTING}

    private GameState state = GameState.WAITING;

    // Zmienione: przechowujemy obiekty Player (lista oczekujących graczy)
    private final List<UUID> activePlayers = new ArrayList<>();
    private ScheduledFuture<?> scheduledTask;


    public HgArena(String worldName, List<Vector3d> spawnPoints, Vector3d lobbySpawnLocation) {
        this.worldName = worldName;
        this.spawnPoints = spawnPoints;
        this.lobbySpawnLocation = lobbySpawnLocation;
        startClockScheduler();
    }


    public boolean forceStart() {
        // Wymuszenie startu: natychmiast ustawiamy stan ingame i teleportujemy graczy

        this.state = GameState.INGAME_MAIN_PHASE;
        this.currentCountdown = ingameArenaSeconds;
        teleportPlayersToTheSpawnPoints();
        return true;
    }

    public void startClockScheduler() {
        if (this.scheduledTask != null) {
            return;
        }
        scheduledTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::updateArenaState, 0L, 1L, TimeUnit.SECONDS);
    }

    public void stopClockScheduler() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
    }

    private void updateArenaState() {
        // co sekundę: sprawdzamy liczbę graczy i sterujemy odliczaniem/uruchomieniem gry
        int playersCount;
        playersCount = activePlayers.size();

        if (state == GameState.INGAME_DEATHMATCH_PHASE) {
            currentCountdown--;
            for (UUID activePlayer : activePlayers) {
                PlayerRef p = Universe.get().getPlayer(activePlayer);
                p.sendMessage(Message.raw("game ends in " + currentCountdown + "s"));
            }

            if (currentCountdown <= 0) {
                currentCountdown = deathmatchArenaSeconds;
                state = GameState.WAITING;
                World world = Universe.get().getWorld(worldName);
                world.execute(() -> {
                    for (UUID active : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(active);
                        Ref<EntityStore> reference = p.getReference();
                        assert reference != null;
                        World defaultWorld = Objects.requireNonNull(Universe.get().getDefaultWorld());
                        ISpawnProvider spawnProvider = defaultWorld.getWorldConfig().getSpawnProvider();
                        Store<EntityStore> store = reference.getStore();
                        Transform spawnPoint = spawnProvider.getSpawnPoint(reference, store);

                        Vector3d position = spawnPoint.getPosition();
                        addTeleportTask(reference, Universe.get().getDefaultWorld(), position);
                        p.sendMessage(Message.raw("Game ended! Returning to lobby."));

                    }
                });

                reset();
            }
            if (this.activePlayers.isEmpty()) {
                this.reset();
            }
            // tu można dodać logikę dla trwającej gry (np. kończenie) - na razie brak
        } else if (state == GameState.INGAME_MAIN_PHASE) {
            currentCountdown--;
            for (UUID activePlayer : activePlayers) {

                PlayerRef p = Universe.get().getPlayer(activePlayer);
                p.sendMessage(Message.raw("deathmatch in " + currentCountdown + "s"));

            }

            if (currentCountdown <= 0) {
                currentCountdown = deathmatchArenaSeconds;
                state = GameState.INGAME_DEATHMATCH_PHASE;
                for (UUID activePlayer : activePlayers) {
                    PlayerRef p = Universe.get().getPlayer(activePlayer);
                    p.sendMessage(Message.raw("Deatchmatch start!"));

                }
                teleportPlayersToTheSpawnPoints();
            }
            if (this.activePlayers.isEmpty()) {
                this.reset();
            }
            // tu można dodać logikę dla trwającej gry (np. kończenie) - na razie brak
        } else {
            if (this.state.equals(GameState.STARTING)) {
                // jeśli ktoś odpadł poniżej minimalnej liczby, anuluj odliczanie
                if (playersCount < minimumStartArenaPlayersNumber) {
                    this.state = GameState.WAITING;
                    currentCountdown = startingArenaSeconds;
                    for (UUID activePlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(activePlayer);
                        p.sendMessage(Message.raw("counting cancelled: not enough players"));

                    }
                    return;
                }

                // kontynuuj odliczanie
                currentCountdown--;
                for (UUID activePlayer : activePlayers) {
                    PlayerRef p = Universe.get().getPlayer(activePlayer);
                    p.sendMessage(Message.raw("Start in " + currentCountdown + "s"));
                }

                if (currentCountdown <= 0) {
                    // start gry
                    currentCountdown = ingameArenaSeconds;
                    state = GameState.INGAME_MAIN_PHASE;
                    for (UUID activePlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(activePlayer);
                        p.sendMessage(Message.raw("Arena started!"));
                    }
                    teleportPlayersToTheSpawnPoints();
                }
            } else {
                // zacznij odliczanie gdy jest wystarczająco graczy
                if (playersCount >= minimumStartArenaPlayersNumber) {
                    this.state = GameState.STARTING;
                    currentCountdown = startingArenaSeconds;
                    // powiadom graczy o starcie odliczania
                    for (UUID activePlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(activePlayer);
                        p.sendMessage(Message.raw("Counting started: game starts in " + currentCountdown + "s"));
                    }
                }
            }
        }

    }

    private void reset() {
        this.state = GameState.WAITING;
        this.currentCountdown = startingArenaSeconds;
    }

    private void teleportPlayersToTheSpawnPoints() {
        // teleportujemy oczekujących graczy do punktów startowych (równomiernie)
        World world = Universe.get().getWorld(this.worldName);
        if (world == null) {
            return;
        }

        world.execute(() -> {
            int idx = 0;
            for (UUID activePlayer : activePlayers) {
                PlayerRef p = Universe.get().getPlayer(activePlayer);
                try {
                    if (p == null || p.getReference() == null) continue;
                    // wybieramy punkt startowy (round-robin)
                    Vector3d spawn;
                    if (spawnPoints == null || spawnPoints.isEmpty()) {
                        spawn = lobbySpawnLocation;
                    } else {
                        spawn = spawnPoints.get(idx % spawnPoints.size());
                    }
                    addTeleportTask(p.getReference(), world, spawn);
                } catch (Throwable ignored) {
                }
                idx++;
            }
        });
    }

    public boolean isActive() {
        return isArenaEnabled;
    }

    public void setActive(boolean value) {
        this.isArenaEnabled = value;
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
        int capacity = (spawnPoints == null || spawnPoints.isEmpty()) ? 1 : spawnPoints.size();

        UUID uuid = player.getUuid();
        if (activePlayers.contains(uuid)) {
            // gracz już dodany
            return;
        }
        if (activePlayers.size() >= capacity) {
            player.sendMessage(Message.raw("Arena is full"));
            return;
        }
        player.sendMessage(Message.raw("You joined the arena: " + this.worldName));
        player.getInventory().clear();
        player.getInventory().markChanged();
        activePlayers.add(uuid);


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

    // pomocnicze API do odczytu liczby aktywnych graczy
    public int getActivePlayerCount() {
        synchronized (activePlayers) {
            return activePlayers.size();
        }
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

    public boolean isIngame() {
        return this.state.equals(GameState.INGAME_DEATHMATCH_PHASE) || this.state.equals(GameState.INGAME_MAIN_PHASE);
    }
}
