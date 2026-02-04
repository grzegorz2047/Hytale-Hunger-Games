package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.hud.MinigameHud;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;
import pl.grzegorz2047.hytale.hungergames.teleport.LobbyTeleporter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;

public class HgArena {
    private final String worldName;
    private final List<Vector3d> playerSpawnPoints;
    private final Vector3d lobbySpawnLocation;
    private final int minimumStartArenaPlayersNumber;
    private boolean isArenaEnabled = false;
    private int deathmatchArenaSeconds = 30;
    // Odliczanie / stan oczekiwania
    private final int startingArenaSeconds = 10; // domyślne odliczanie
    private final int ingameArenaSeconds = 30; // domyślne odliczanie
    private int currentCountdown = startingArenaSeconds;
    private List<Vector3i> openedChests = new ArrayList<>();

    public boolean isBlockOpenedInArena(Vector3i position) {
        return this.openedChests.contains(position);
    }

    public void addBlockOpenedInArena(Vector3i position) {
        this.openedChests.add(position);
    }

    public enum GameState {WAITING, STARTING, INGAME_MAIN_PHASE, INGAME_DEATHMATCH_PHASE, RESTARTING}

    private GameState state = GameState.WAITING;

    // Zmienione: przechowujemy obiekty Player (lista oczekujących graczy)
    private final List<UUID> activePlayers = new ArrayList<>();
    private ScheduledFuture<?> scheduledTask;
    private final MainConfig config;
    private static final int KILLFEED_MAX_LINES = 5;
    private final Deque<String> recentKills = new ArrayDeque<>();


    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d lobbySpawnLocation, MainConfig config) {
        this(worldName, playerSpawnPoints, lobbySpawnLocation, config, true);
    }

    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d lobbySpawnLocation, MainConfig config, boolean startScheduler) {
        this.worldName = worldName;
        this.playerSpawnPoints = playerSpawnPoints;
        this.lobbySpawnLocation = lobbySpawnLocation;
        this.config = config;
        this.minimumStartArenaPlayersNumber = config.getMinimumPlayersToStartArena();
        if (startScheduler) {
            startClockScheduler();
        }
    }

    public void playerDied(Player playerComponent, Player attackerPlayer) {
        if (!this.activePlayers.contains(playerComponent.getUuid())) {
            return;
        }
        if (!this.isIngame()) {
            return;
        }
        this.activePlayers.remove(playerComponent.getUuid());
        String killEntry;
        if (attackerPlayer != null) {
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled("<color=#FF0000>" + playerComponent.getDisplayName() + " has been killed by " + attackerPlayer.getDisplayName() + " !</color>"));
            killEntry = attackerPlayer.getDisplayName() + " killed " + playerComponent.getDisplayName();
        } else {
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled("<color=#FF0000>" + playerComponent.getDisplayName() + " has died!</color>"));
            killEntry = playerComponent.getDisplayName() + " died";
        }
        addKillFeedEntry(killEntry);
        updateKillFeedForActivePlayers();
        PlayerRef playerRef = playerComponent.getPlayerRef();
        clearCustomHud(playerComponent, playerRef);
        LobbyTeleporter.teleportToLobby(playerRef);
    }

    public int getArenaSize() {
        return this.playerSpawnPoints.size();
    }

    public void playerLeft(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        if (!activePlayers.contains(playerRef.getUuid())) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        activePlayers.remove(uuid);
        String tpl = this.config.getTranslation("hungergames.arena.left");
        playerRef.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{worldName}", this.worldName)));
        synchronized (activePlayers) {
            String tpl2 = this.config.getTranslation("hungergames.arena.playerLeftBroadcast");
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled(tpl2 == null ? "" : tpl2.replace("{count}", String.valueOf(activePlayers.size()))));
        }
    }

    @NullableDecl
    private static Player getPlayer(PlayerRef playerRef) {
        Player player = null;
        Ref<EntityStore> reference = playerRef.getReference();
        if (reference != null) {
            player = findPlayerInPlayerComponentsBag(reference.getStore(), reference);
        }
        return player;
    }

    private void broadcastMessageToActivePlayers(Message message) {
        for (UUID playerUuid : activePlayers) {
            PlayerRef p = Universe.get().getPlayer(playerUuid);
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }

    public boolean isPlayerInArena(Player player) {
        return this.activePlayers.contains(player.getUuid());
    }

    public boolean forceStart() {
        // Wymuszenie startu: natychmiast ustawiamy stan ingame i teleportujemy graczy

        this.state = GameState.INGAME_MAIN_PHASE;
        this.currentCountdown = ingameArenaSeconds;
        teleportPlayersToTheSpawnPoints(playerSpawnPoints);
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
        int playersCount;
        synchronized (activePlayers) {
            playersCount = activePlayers.size();
        }
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();

        switch (state) {
            case INGAME_DEATHMATCH_PHASE -> {
                countdown();
                world.execute(() -> {
                    for (UUID activePlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(activePlayer);
                        if (p == null) {
                            continue;
                        }
                        Ref<EntityStore> reference = p.getReference();
                        String tpl = this.config.getTranslation("hungergames.arena.gameEndsIn");
                        Player player = findPlayerInPlayerComponentsBag(store, reference);
                        if (player == null) {
                            continue;
                        }
                        updateScoreboard(player.getHudManager().getCustomHud());
                        p.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{seconds}", String.valueOf(currentCountdown))));
                    }
                });

                if (currentCountdown <= 0) {
                    currentCountdown = deathmatchArenaSeconds;
                    resetArenaNoWinners(world);
                }
                synchronized (activePlayers) {
                    if (this.activePlayers.isEmpty()) {
                        this.reset();
                    }
                }
            }
            case INGAME_MAIN_PHASE -> {
                countdown();
                synchronized (activePlayers) {

                    world.execute(() -> {
                        for (PlayerRef p : world.getPlayerRefs()) {
                            if (p != null) {
                                String tpl = this.config.getTranslation("hungergames.arena.deathmatchIn");
                                Ref<EntityStore> reference = p.getReference();
                                Player player = findPlayerInPlayerComponentsBag(store, reference);
                                updateScoreboard(player.getHudManager().getCustomHud());
                                p.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{seconds}", String.valueOf(currentCountdown))));
                            }
                        }

                    });
                }

                if (currentCountdown <= 0) {
                    startIngamePhase(deathmatchArenaSeconds, GameState.INGAME_DEATHMATCH_PHASE, "hungergames.arena.deathmatchStart");
                }
                synchronized (activePlayers) {
                    if (this.activePlayers.isEmpty()) {
                        this.reset();
                    }
                }
            }
            case STARTING -> {
                countdown();
                if (currentCountdown <= 0) {
                    startIngamePhase(ingameArenaSeconds, GameState.INGAME_MAIN_PHASE, "hungergames.arena.arenaStarted");
                }
                if (isNotEnoughtPlayers(playersCount)) {
                    this.state = GameState.WAITING;
                    currentCountdown = startingArenaSeconds;
                    synchronized (activePlayers) {
                        for (UUID activePlayer : activePlayers) {
                            PlayerRef p = Universe.get().getPlayer(activePlayer);
                            if (p != null) {
                                String tpl = this.config.getTranslation("hungergames.arena.countingCancelled");
                                p.sendMessage(MessageColorUtil.rawStyled(tpl));
                            }
                        }
                    }
                    return;
                }
                world.execute(() -> {
                    for (UUID activePlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(activePlayer);
                        if (p != null) {
                            String tpl = this.config.getTranslation("hungergames.arena.startIn");
                            Ref<EntityStore> reference = p.getReference();
                            if (reference == null) continue;
                            Store<EntityStore> activeStore = reference.getStore();
                            if (activeStore == null) continue;
                            Player player = findPlayerInPlayerComponentsBag(activeStore, reference);
                            updateScoreboard(player.getHudManager().getCustomHud());
                            p.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{seconds}", String.valueOf(currentCountdown))));
                        }
                    }
                });
            }
            case null, default -> {
                if (playersCount >= minimumStartArenaPlayersNumber) {
                    this.state = GameState.STARTING;
                    currentCountdown = startingArenaSeconds;

                    synchronized (activePlayers) {
                        world.execute(() -> {
                            for (UUID activePlayer : activePlayers) {
                                PlayerRef p = Universe.get().getPlayer(activePlayer);
                                if (p != null) {
                                    String tpl = this.config.getTranslation("hungergames.arena.countingStarted");
                                    Ref<EntityStore> reference = p.getReference();
                                    if (reference == null) continue;
                                    Player player = findPlayerInPlayerComponentsBag(reference.getStore(), reference);
                                    updateScoreboard(player.getHudManager().getCustomHud());
                                    p.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{seconds}", String.valueOf(currentCountdown))));
                                }
                            }
                        });
                    }
                }
            }
        }

    }

    private void resetArenaNoWinners(World world) {
        world.execute(() -> {
            this.activePlayers.clear();
            for (Player p : world.getPlayers()) {
                if (p == null || p.getReference() == null) continue;
                Ref<EntityStore> refForTeleport = p.getReference();
                // walidacja referencji przed użyciem
                try {
                    World defaultWorld = Objects.requireNonNull(Universe.get().getDefaultWorld());
                    Vector3d position = getLobbyPosition(defaultWorld, refForTeleport);
                    try {
                        addTeleportTask(refForTeleport, defaultWorld, position);
                    } catch (Throwable t) {
                        HytaleLogger.getLogger().atWarning().withCause(t).log("Failed to schedule teleport during end-game for player %s: %s", p.getDisplayName(), t.getMessage());
                    }
                    String tpl = this.config.getTranslation("hungergames.arena.gameEndedReturn");
                    p.sendMessage(MessageColorUtil.rawStyled(tpl));
                } catch (Throwable t) {
                    HytaleLogger.getLogger().atWarning().withCause(t).log("Skipping teleport for possibly invalid reference for player %s: %s", p.getDisplayName(), t.getMessage());
                }
            }
        });
        reset();
    }

    @NonNullDecl
    private static Vector3d getLobbyPosition(World defaultWorld, Ref<EntityStore> refForTeleport) {
        ISpawnProvider spawnProvider = defaultWorld.getWorldConfig().getSpawnProvider();
        Store<EntityStore> storeLocal = refForTeleport.getStore();
        Transform spawnPoint = spawnProvider.getSpawnPoint(refForTeleport, storeLocal);

        Vector3d position = spawnPoint.getPosition();
        return position;
    }

    private void startIngamePhase(int ingameArenaSeconds, GameState gamePhase, String key) {
        currentCountdown = ingameArenaSeconds;
        state = gamePhase;
        synchronized (activePlayers) {
            for (UUID activePlayer : activePlayers) {
                PlayerRef p = Universe.get().getPlayer(activePlayer);
                if (p != null) {
                    String tpl = this.config.getTranslation(key);
                    p.sendMessage(MessageColorUtil.rawStyled(tpl));
                }
            }
        }
        teleportPlayersToTheSpawnPoints(playerSpawnPoints);
    }

    private boolean isNotEnoughtPlayers(int playersCount) {
        return playersCount < minimumStartArenaPlayersNumber;
    }

    private void updateScoreboard(CustomUIHud customHud) {
        if (customHud != null) {
            MinigameHud customHudMinigame = (MinigameHud) customHud;
            String timeLabel = getTranslationOrDefault("hungergames.hud.time", "Time");
            String playersLabel = getTranslationOrDefault("hungergames.hud.playersLeft", "Players left");
            String arenaLabel = getTranslationOrDefault("hungergames.hud.arena", "Arena");
            customHudMinigame.setTimeText(timeLabel + ": " + formatHHMMSS(currentCountdown));
            customHudMinigame.setNumOfActivePlayers(playersLabel + ": " + this.activePlayers.size());
            customHudMinigame.setArenaName(arenaLabel + ": " + this.worldName);
            customHudMinigame.setKillFeedText(buildKillFeedText());
        }
    }

    private String buildKillFeedText() {
        String title = getTranslationOrDefault("hungergames.hud.killFeed", "Kill Feed");
        String emptyValue = getTranslationOrDefault("hungergames.hud.killFeedEmpty", "-");
        StringBuilder sb = new StringBuilder(title + ":");
        synchronized (recentKills) {
            if (recentKills.isEmpty()) {
                return sb.append(" ").append(emptyValue).toString();
            }
            for (String line : recentKills) {
                sb.append("\n").append(line);
            }
        }
        return sb.toString();
    }

    private String getTranslationOrDefault(String key, String fallback) {
        String value = this.config.getTranslation(key);
        return value == null ? fallback : value;
    }

    public static String formatHHMMSS(int secs) {
        if (secs < 3600) {
            int minutes = secs / 60;
            int seconds = secs % 60;

            return (minutes < 10 ? "0" : "") + minutes + ":"
                    + (seconds < 10 ? "0" : "") + seconds;
        }
        int hours = secs / 3600;
        int divider = secs % 3600;
        int minutes = divider / 60;
        int seconds = divider % 60;

        return (hours < 10 ? "0" : "") + hours + ":"
                + (minutes < 10 ? "0" : "") + minutes + ":"
                + (seconds < 10 ? "0" : "") + seconds;
    }

    private void countdown() {
        currentCountdown--;
    }

    private void reset() {
        this.activePlayers.clear();
        this.recentKills.clear();
        this.openedChests.clear();
        this.state = GameState.WAITING;
        this.currentCountdown = startingArenaSeconds;
    }

    private void addKillFeedEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        synchronized (recentKills) {
            recentKills.addFirst(entry);
            while (recentKills.size() > KILLFEED_MAX_LINES) {
                recentKills.removeLast();
            }
        }
    }


    private void updateKillFeedForActivePlayers() {
        World world = Universe.get().getWorld(this.worldName);
        if (world == null) {
            return;
        }
        List<UUID> snapshot;
        synchronized (activePlayers) {
            snapshot = new ArrayList<>(activePlayers);
        }
        String killFeedText = buildKillFeedText();
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            for (UUID activePlayer : snapshot) {
                PlayerRef p = Universe.get().getPlayer(activePlayer);
                if (p == null || p.getReference() == null) {
                    continue;
                }
                Player player = findPlayerInPlayerComponentsBag(store, p.getReference());
                if (player == null) {
                    continue;
                }
                CustomUIHud customHud = player.getHudManager().getCustomHud();
                if (customHud instanceof MinigameHud minigameHud) {
                    minigameHud.setKillFeedText(killFeedText);
                }
            }
        });
    }

    protected void teleportPlayersToTheSpawnPoints(List<Vector3d> spawnPoints) {
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

    public List<Vector3d> getPlayerSpawnPoints() {
        return playerSpawnPoints;
    }

    public Vector3d getLobbySpawnLocation() {
        return lobbySpawnLocation;
    }

    public void join(World playerWorld, UUID uuid) {

        try {
            PlayerRef playerRefFromUUID = Universe.get().getPlayer(uuid);
            Player player = findPlayerInPlayerComponentsBag(playerRefFromUUID.getReference().getStore(), playerRefFromUUID.getReference());
            if (player == null) {
                return;
            }
            int capacity = (playerSpawnPoints == null || playerSpawnPoints.isEmpty()) ? 1 : playerSpawnPoints.size();

            if (activePlayers.contains(uuid)) {
                // gracz już dodany
                return;
            }
            if (activePlayers.size() >= capacity) {
                String tpl = this.config.getTranslation("hungergames.arena.full");
                player.sendMessage(MessageColorUtil.rawStyled(tpl));
                return;
            }
            activePlayers.add(uuid);
            PlayerRef playerRef = Universe.get().getPlayer(uuid);
            Ref<EntityStore> reference = playerRef.getReference();


            String tplJoined = this.config.getTranslation("hungergames.arena.joined");
            player.sendMessage(MessageColorUtil.rawStyled(tplJoined == null ? "" : tplJoined.replace("{worldName}", this.worldName)));
            Inventory inventory = player.getInventory();
            inventory.clear();
            inventory.markChanged();

            // przygotowanie HUD i teleport w bezpiecznym bloku try/catch
            try {
                player.getHudManager().setCustomHud(playerRef, new MinigameHud(playerRef, 24, 300, true));
                World arenaWorld = Universe.get().getWorld(this.worldName);
                addTeleportTask(reference, arenaWorld, this.lobbySpawnLocation);
            } catch (Throwable t) {
                HytaleLogger.getLogger().atWarning().withCause(t).log("Failed to prepare/teleport player %s to arena %s: %s", uuid, this.worldName, t.getMessage());
            }
        } catch (Throwable outer) {
            HytaleLogger.getLogger().atWarning().withCause(outer).log("Error while finishing join for player %s: %s", uuid, outer.getMessage());
        }


    }

//    public void preparePlayerJoin(PlayerRef playerRef) {
//        HytaleLogger logger = HytaleLogger.getLogger();
//        try {
//            World world = Universe.get().getWorld(playerRef.getWorldUuid());
//            Ref<EntityStore> ref = playerRef.getReference();
//
//            world.execute(() -> {
//                try {
//                    Store<EntityStore> store = ref.getStore();
//                    HudBuilder.detachedHud()
//                            .fromHtml(hudArenaInfo)
//                            .show(playerRef, store);
//                } catch (Throwable t) {
//                    logger.atWarning().withCause(t).log("Failed to show HUD: %s", t.getMessage());
//                }
//            });
//        } catch (Throwable t) {
//            logger.atWarning().withCause(t).log("Error preparing HUD: %s", t.getMessage());
//        }
//    }

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
        try {
            Store<EntityStore> store = playerRef.getStore();
            ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
            Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
            // dodanie komponentu może wyrzucić IllegalStateException jeśli ref jest nieważny — obsłużymy to
            store.addComponent(playerRef, componentType, teleport);
        } catch (IllegalStateException ise) {
            HytaleLogger.getLogger().atWarning().withCause(ise).log("Invalid entity reference when adding teleport: %s", ise.getMessage());
        } catch (Throwable t) {
            HytaleLogger.getLogger().atWarning().withCause(t).log("Failed to add teleport task: %s", t.getMessage());
        }
    }

    public boolean isIngame() {
        return this.state.equals(GameState.INGAME_DEATHMATCH_PHASE) || this.state.equals(GameState.INGAME_MAIN_PHASE);
    }

    private void clearCustomHud(Player player, PlayerRef playerRef) {
        if (playerRef == null || player == null) {
            return;
        }
        try {
            HudManager hudManager = player.getHudManager();
            hudManager.resetHud(playerRef);
        } catch (Throwable t) {
            HytaleLogger.getLogger().atWarning().withCause(t)
                    .log("Failed to clear custom HUD for player %s", playerRef.getUuid());
        }
    }

}
