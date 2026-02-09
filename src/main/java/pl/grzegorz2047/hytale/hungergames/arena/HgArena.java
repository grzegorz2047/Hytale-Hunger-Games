package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.db.PlayerRepository;
import pl.grzegorz2047.hytale.hungergames.hud.MinigameHud;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;
import pl.grzegorz2047.hytale.hungergames.teleport.LobbyTeleporter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.hypixel.hytale.logger.HytaleLogger.getLogger;
import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;

public class HgArena {
    private final String worldName;
    private final List<Vector3d> playerSpawnPoints;
    private Vector3d arenaLobbySpawnLocation;
    private final int minimumStartArenaPlayersNumber;
    private boolean isArenaEnabled = false;
    private final int deathmatchArenaSeconds;
    // Odliczanie / stan oczekiwania
    private final int startingArenaSeconds;
    private final int ingameArenaSeconds;
    private int currentCountdown;
    private List<Vector3i> openedChests = new ArrayList<>();

    public boolean isBlockOpenedInArena(Vector3i position) {
        return this.openedChests.contains(position);
    }

    public void addBlockOpenedInArena(Vector3i position) {
        this.openedChests.add(position);
    }

    public void playerDisconnected(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        HgPlayer hgPlayer = findHgPlayerByUuid(uuid);
        if (hgPlayer == null) {
            return;
        }
        activePlayers.remove(hgPlayer);
    }

    public String getArenaPlayersStat() {
        String playersStatLabel = getTranslationOrDefault("hungergames.hud.playersLeft", "arena players");
        return playersStatLabel.replace("{activePlayers}", String.valueOf(this.activePlayers.size()))
                .replace("{maxPlayers}", String.valueOf(this.playerSpawnPoints.size()));
    }

    public enum GameState {WAITING, STARTING, INGAME_MAIN_PHASE, INGAME_DEATHMATCH_PHASE, RESTARTING}

    private GameState state = GameState.WAITING;

    // Przechowujemy obiekty HgPlayer (lista aktywnych graczy z ich statystykami)
    private final List<HgPlayer> activePlayers = new ArrayList<>();
    private final PlayerRepository playerRepository;
    private ScheduledFuture<?> scheduledTask;
    private final MainConfig config;
    private static final int KILLFEED_MAX_LINES = 5;
    private final Deque<String> recentKills = new ArrayDeque<>();


    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d arenaLobbySpawnLocation, MainConfig config, PlayerRepository playerRepository) {
        this(worldName, playerSpawnPoints, arenaLobbySpawnLocation, config, playerRepository, true);
    }

    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d arenaLobbySpawnLocation, MainConfig config) {
        this(worldName, playerSpawnPoints, arenaLobbySpawnLocation, config, null, true);
    }

    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d arenaLobbySpawnLocation, MainConfig config, boolean startScheduler) {
        this(worldName, playerSpawnPoints, arenaLobbySpawnLocation, config, null, startScheduler);
    }

    public HgArena(String worldName, List<Vector3d> playerSpawnPoints, Vector3d arenaLobbySpawnLocation, MainConfig config, PlayerRepository playerRepository, boolean startScheduler) {
        this.worldName = worldName;
        // Inicjalizujemy playerSpawnPoints jako ArrayList aby umożliwić dodawanie/usuwanie spawn points
        this.playerSpawnPoints = playerSpawnPoints != null ? new ArrayList<>(playerSpawnPoints) : new ArrayList<>();
        this.arenaLobbySpawnLocation = arenaLobbySpawnLocation;
        this.config = config;
        this.playerRepository = playerRepository;

        this.minimumStartArenaPlayersNumber = config.getMinimumPlayersToStartArena();
        this.deathmatchArenaSeconds = config.getDeathmatchArenaSeconds();
        this.startingArenaSeconds = config.getStartingArenaSeconds();
        this.ingameArenaSeconds = config.getIngameArenaSeconds();
        this.currentCountdown = startingArenaSeconds;
        if (startScheduler) {
            startClockScheduler();
        }
    }

    public void playerDied(Player diedPlayer, Player attackerPlayer) {
        UUID deadPlayerUuid = diedPlayer.getUuid();
        if (!this.isIngame()) {
            return;
        }
        HgPlayer hgPlayerByUuid = findHgPlayerByUuid(deadPlayerUuid);
        this.activePlayers.remove(hgPlayerByUuid);

        if (attackerPlayer != null) {
            UUID attackerUuid = attackerPlayer.getUuid();
            HgPlayer attackerHgPlayer = findHgPlayerByUuid(attackerUuid);

            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled("<color=#FF0000>" + diedPlayer.getDisplayName() + " has been killed by " + attackerPlayer.getDisplayName() + " !</color>"));

            if (attackerHgPlayer != null) {
                attackerHgPlayer.addKill(); // Zabójstwo w bieżącej grze
                attackerHgPlayer.addGlobalKill(); // Incjrement globalnych zabójstw
                savePlayerToDatabase(attackerHgPlayer);
            }
        } else {
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled("<color=#FF0000>" + diedPlayer.getDisplayName() + " has died!</color>"));
        }
        Store<EntityStore> store = diedPlayer.getPlayerRef().getReference().getStore();

        diedPlayer.getWorld().execute(() -> {
            healPlayer(store, diedPlayer.getReference());
            LobbyTeleporter.teleportToLobby(diedPlayer.getPlayerRef());
        });

        if (this.activePlayers.size() == 1) {
            String tpl = this.config.getTranslation("hungergames.arena.gameEndedWinner");
            broadcastMessageToAllPlayers(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{player}", attackerPlayer.getDisplayName())));
            healPlayer(attackerPlayer.getPlayerRef().getReference().getStore(), attackerPlayer.getReference());
            LobbyTeleporter.teleportToLobby(attackerPlayer.getPlayerRef());
            reset();
        }
    }

    private void broadcastMessageToAllPlayers(Message message) {
        Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(message));
    }

    public int getArenaSize() {
        return this.playerSpawnPoints.size();
    }

    public void playerLeft(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        HgPlayer hgPlayer = findHgPlayerByUuid(uuid);
        if (hgPlayer == null) {
            return;
        }

        activePlayers.remove(hgPlayer);
        String tpl = this.config.getTranslation("hungergames.arena.left");
        playerRef.sendMessage(MessageColorUtil.rawStyled(tpl == null ? "" : tpl.replace("{worldName}", this.worldName)));
        getArenaWorld().execute(() -> teleportToMainLobby(playerRef.getReference(), playerRef.getUsername()));

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
        for (HgPlayer hgPlayer : activePlayers) {
            PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }

    public boolean isPlayerInArena(UUID uuid) {
        return findHgPlayerByUuid(uuid) != null;
    }

    public boolean forceStart() {
        if (this.playerSpawnPoints == null || playerSpawnPoints.isEmpty()) {
            return false;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> {
                    getArenaWorld().execute(() -> startGame(true));
                },
                1, TimeUnit.SECONDS
        );

        return true;
    }

    protected void startGame(boolean isForced) {
        // Wymuszenie startu: natychmiast ustawiamy stan ingame i teleportujemy graczy
        this.state = GameState.INGAME_MAIN_PHASE;
        this.currentCountdown = ingameArenaSeconds;
        World world = getArenaWorld();
        if (world == null) {
            return;
        }
        float time = 12;
        Store<EntityStore> store = world.getEntityStore().getStore();
        WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
        worldTimeResource.setDayTime(time / (float) WorldTimeResource.HOURS_PER_DAY, world, store);

        for (HgPlayer hgPlayer : activePlayers) {
            PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
            if (p == null) {
                continue;
            }
            Player player = findPlayerInPlayerComponentsBag(store, p.getReference());
            Inventory inventory = player.getInventory();
            inventory.clear();
            inventory.markChanged();
        }
        teleportPlayersToTheSpawnPoints(playerSpawnPoints);
        String translation = config.getTranslation("hungergames.arena.arenaStarted");

        String input = "The game has been force-started!";
        if (isForced) {
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled(input));
        } else {
            broadcastMessageToActivePlayers(MessageColorUtil.rawStyled(translation));
        }
    }

    @NullableDecl
    private World getArenaWorld() {
        return Universe.get().getWorld(this.worldName);
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

        switch (state) {
            case INGAME_DEATHMATCH_PHASE -> {
                countdown();
                world.execute(() -> {
                    for (HgPlayer hgPlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                        if (p == null) {
                            continue;
                        }
                        Ref<EntityStore> reference = p.getReference();
                        String tpl = this.config.getTranslation("hungergames.arena.gameEndsIn");
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        Player player = findPlayerInPlayerComponentsBag(store, reference);
                        if (player == null) {
                            continue;
                        }
                        updateScoreboardForPlayer(player.getHudManager().getCustomHud(), hgPlayer.getUuid());
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
                        for (HgPlayer hgPlayer : activePlayers) {
                            PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                            if (p != null) {
                                String tpl = this.config.getTranslation("hungergames.arena.deathmatchIn");
                                Ref<EntityStore> reference = p.getReference();
                                Store<EntityStore> store = world.getEntityStore().getStore();
                                Player player = findPlayerInPlayerComponentsBag(store, reference);
                                updateScoreboardForPlayer(player.getHudManager().getCustomHud(), hgPlayer.getUuid());
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
                    getArenaWorld().execute(() -> startGame(false));
                }
                if (isNotEnoughtPlayers(playersCount)) {
                    this.state = GameState.WAITING;
                    currentCountdown = startingArenaSeconds;
                    synchronized (activePlayers) {
                        for (HgPlayer hgPlayer : activePlayers) {
                            PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                            if (p != null) {
                                String tpl = this.config.getTranslation("hungergames.arena.countingCancelled");
                                p.sendMessage(MessageColorUtil.rawStyled(tpl));
                            }
                        }
                    }
                    return;
                }
                world.execute(() -> {
                    for (HgPlayer hgPlayer : activePlayers) {
                        PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                        if (p != null) {
                            String tpl = this.config.getTranslation("hungergames.arena.startIn");
                            Ref<EntityStore> reference = p.getReference();
                            if (reference == null) continue;
                            Store<EntityStore> activeStore = reference.getStore();
                            Player player = findPlayerInPlayerComponentsBag(activeStore, reference);
                            updateScoreboardForPlayer(player.getHudManager().getCustomHud(), hgPlayer.getUuid());
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
                            for (HgPlayer hgPlayer : activePlayers) {
                                PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                                if (p != null) {
                                    String tpl = this.config.getTranslation("hungergames.arena.countingStarted");
                                    Ref<EntityStore> reference = p.getReference();
                                    if (reference == null) continue;
                                    Player player = findPlayerInPlayerComponentsBag(reference.getStore(), reference);
                                    updateScoreboardForPlayer(player.getHudManager().getCustomHud(), hgPlayer.getUuid());
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
                    p.getHudManager().setCustomHud(p.getPlayerRef(), new CustomUIHud(p.getPlayerRef()) {
                        @Override
                        protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {

                        }
                    });
                    teleportToMainLobby(refForTeleport, p.getDisplayName());
                    String tpl = this.config.getTranslation("hungergames.arena.gameEndedReturn");
                    p.sendMessage(MessageColorUtil.rawStyled(tpl));
                } catch (Throwable t) {
                    getLogger().atWarning().withCause(t).log("Skipping teleport for possibly invalid reference for player %s: %s", p.getDisplayName(), t.getMessage());
                }
            }
        });
        reset();
    }

    private void teleportToMainLobby(Ref<EntityStore> refForTeleport, String displayName) {
        World defaultWorld = Objects.requireNonNull(Universe.get().getDefaultWorld());
        Vector3d position = getMainLobbyPosition(defaultWorld, refForTeleport);
        try {
            addTeleportTask(refForTeleport, defaultWorld, position);
        } catch (Throwable t) {
            getLogger().atWarning().withCause(t).log("Failed to schedule teleport during end-game for player %s: %s", displayName, t.getMessage());
        }
    }

    @NonNullDecl
    private static Vector3d getMainLobbyPosition(World defaultWorld, Ref<EntityStore> refForTeleport) {
        ISpawnProvider spawnProvider = defaultWorld.getWorldConfig().getSpawnProvider();
        Store<EntityStore> storeLocal = refForTeleport.getStore();
        Transform spawnPoint = spawnProvider.getSpawnPoint(refForTeleport, storeLocal);
        Vector3d position = spawnPoint.getPosition();
        return position;
    }

    private void startIngamePhase(int ingameArenaSeconds, GameState gamePhase, String key) {
        currentCountdown = ingameArenaSeconds;
        state = gamePhase;
        String tpl = this.config.getTranslation(key);
        synchronized (activePlayers) {
            for (HgPlayer hgPlayer : activePlayers) {
                PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                if (p != null) {
                    p.sendMessage(MessageColorUtil.rawStyled(tpl));
                }
            }
        }
        teleportPlayersToTheSpawnPoints(playerSpawnPoints);
        broadcastMessageToActivePlayers(MessageColorUtil.rawStyled("The game has been started!"));

    }

    private boolean isNotEnoughtPlayers(int playersCount) {
        return playersCount < minimumStartArenaPlayersNumber;
    }

    /**
     * Aktualizuje HUD gracza z jego bieżącą liczbą zabójstw
     */
    private void updateScoreboardForPlayer(CustomUIHud customHud, UUID playerUuid) {
        if (customHud instanceof MinigameHud customHudMinigame) {
            // Pobierz gracza aby uzyskać jego liczbę zabójstw
            HgPlayer hgPlayer = findHgPlayerByUuid(playerUuid);
            String playerKills = getTranslationOrDefault("hungergames.hud.yourKills", "Your Kills").replace("{kills}",String.valueOf((hgPlayer != null ? hgPlayer.getKills() : 0)));

            // Aktualizuj główne informacje
            String arenaTime = getTranslationOrDefault("hungergames.hud.time", "time").replace("{time}", formatHHMMSS(currentCountdown));
            String arenaName = getTranslationOrDefault("hungergames.hud.arena", "arena").replace("{arenaName}", this.worldName);
            customHudMinigame.setTimeText(arenaTime);
            customHudMinigame.setNumOfActivePlayers(this.getArenaPlayersStat());
            customHudMinigame.setArenaName(arenaName);

            // Ustaw liczbę zabójstw gracza
            customHudMinigame.setPlayerKills(playerKills);
        }
    }

    private String buildKillFeedText() {
        String title = getTranslationOrDefault("hungergames.hud.killFeed", "Kills");
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

    protected void teleportPlayersToTheSpawnPoints(List<Vector3d> spawnPoints) {
        // teleportujemy oczekujących graczy do punktów startowych (równomiernie)
        World world = getArenaWorld();
        // Weryfikacja czy mamy spawn pointy
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            getLogger().atWarning().log("Arena '%s' has no spawn points! Cannot teleport players.", this.worldName);
            return;
        }

        // Zrób kopię listy graczy aby uniknąć ConcurrentModificationException
        List<HgPlayer> playerSnapshot;
        synchronized (activePlayers) {
            playerSnapshot = new ArrayList<>(activePlayers);
        }

        world.execute(() -> {
            int idx = 0;
            for (HgPlayer hgPlayer : playerSnapshot) {
                PlayerRef p = Universe.get().getPlayer(hgPlayer.getUuid());
                try {
                    if (p == null || p.getReference() == null) continue;
                    // wybieramy punkt startowy (round-robin)
                    int spawnIndex = idx % spawnPoints.size();
                    Vector3d spawn = spawnPoints.get(spawnIndex);
                    getLogger().atInfo().log("Teleporting player %s to spawn point %d: %.2f, %.2f, %.2f",
                            hgPlayer.getPlayerName(), spawnIndex, spawn.x, spawn.y, spawn.z);
                    addTeleportTask(p.getReference(), world, spawn);
                } catch (Throwable t) {
                    getLogger().atWarning().withCause(t).log("Failed to teleport player %s to spawn point: %s",
                            hgPlayer.getPlayerName(), t.getMessage());
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

    public void addSpawnPoint(Vector3d spawnPoint) {
        if (spawnPoint != null) {
            this.playerSpawnPoints.add(spawnPoint);
        }
    }

    public void clearSpawnPoints() {
        this.playerSpawnPoints.clear();
    }

    public void setArenaLobbySpawnLocation(Vector3d arenaLobbySpawnLocation) {
        if (arenaLobbySpawnLocation != null) {
            // Create a new Vector3d to avoid external modifications
            this.arenaLobbySpawnLocation = new Vector3d(arenaLobbySpawnLocation.x, arenaLobbySpawnLocation.y, arenaLobbySpawnLocation.z);
        }
    }

    public Vector3d getArenaLobbySpawnLocation() {
        return this.arenaLobbySpawnLocation;
    }

    public List<Vector3d> getSpawnPoints() {
        return new ArrayList<>(this.playerSpawnPoints);
    }

    public void join(World playerWorld, UUID uuid) {

        try {
            PlayerRef playerRefFromUUID = Universe.get().getPlayer(uuid);
            Store<EntityStore> store = playerWorld.getEntityStore().getStore();
            Player player = findPlayerInPlayerComponentsBag(store, playerRefFromUUID.getReference());
            if (player == null) {
                return;
            }
            int capacity = (playerSpawnPoints == null || playerSpawnPoints.isEmpty()) ? 1 : playerSpawnPoints.size();
            if (isIngame()) {
                return;
            }
            // Sprawdź czy gracz już jest w arenie
            if (isPlayerInArena(uuid)) {
                return;
            }

            if (activePlayers.size() >= capacity) {
                String tpl = this.config.getTranslation("hungergames.arena.full");
                player.sendMessage(MessageColorUtil.rawStyled(tpl));
                return;
            }

            // Stwórz lub pobierz gracza z bazy danych
            String playerName = player.getDisplayName();
            HgPlayer hgPlayer;

            try {
                Optional<HgPlayer> existingPlayer = playerRepository.findByUuid(uuid);
                if (existingPlayer.isPresent()) {
                    // Gracz istnieje w bazie - pobierz go i zresetuj kills na bieżącą grę
                    hgPlayer = existingPlayer.get();
                    hgPlayer.resetKills(); // Resetuj liczę zabójstw w bieżącej grze
                } else {
                    // Gracz nie istnieje - stwórz nowego
                    hgPlayer = new HgPlayer(uuid, playerName, 0, 0);
                    savePlayerToDatabase(hgPlayer);
                }
            } catch (Exception e) {
                getLogger().atWarning().withCause(e)
                        .log("Failed to load player %s from database, creating new: %s", uuid, e.getMessage());
                hgPlayer = new HgPlayer(uuid, playerName, 0, 0);
                savePlayerToDatabase(hgPlayer);
            }


            PlayerRef playerRef = Universe.get().getPlayer(uuid);
            Ref<EntityStore> reference = playerRef.getReference();
            Inventory inventory = player.getInventory();
            inventory.clear();
            inventory.markChanged();
//            EntityStatMap statMap = (EntityStatMap) store.getComponent(playerRef, EntityStatMap.getComponentType());


            healPlayer(store, reference);
            World arenaWorld = getArenaWorld();

            try {
                activePlayers.add(hgPlayer);

                addTeleportTask(reference, arenaWorld, this.arenaLobbySpawnLocation);

                String tplJoined = this.config.getTranslation("hungergames.arena.joined");
                String tplplayerJoined = this.config.getTranslation("hungergames.arena.numplayerjoined");
                broadcastMessageToActivePlayers(MessageColorUtil.rawStyled(tplplayerJoined == null ? "" : tplplayerJoined.replace("{numberOfPlayers}", String.valueOf(this.activePlayers.size())).replace("{maxNumberOfPlayersInArena}", String.valueOf(this.playerSpawnPoints.size())).replace("{arenaName}", this.worldName)));
                player.sendMessage(MessageColorUtil.rawStyled(tplJoined == null ? "" : tplJoined.replace("{worldName}", this.worldName)));

            } catch (Throwable t) {
                getLogger().atWarning().withCause(t).log("Failed to prepare/teleport player %s to arena %s: %s", uuid, this.worldName, t.getMessage());
            }
        } catch (
                Throwable outer) {
            getLogger().atWarning().withCause(outer).log("Error while finishing join for player %s: %s", uuid, outer.getMessage());
        }

    }

    private static void healPlayer(Store<EntityStore> store, Ref<EntityStore> reference) {
        EntityStatMap statMap = store.getComponent(reference, EntityStatMap.getComponentType());
        if (statMap != null) {
            statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());

        }
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
        try {
            Store<EntityStore> store = playerRef.getStore();
            ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
            Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
            // dodanie komponentu może wyrzucić IllegalStateException jeśli ref jest nieważny — obsłużymy to
            store.addComponent(playerRef, componentType, teleport);
        } catch (IllegalStateException ise) {
            getLogger().atWarning().withCause(ise).log("Invalid entity reference when adding teleport: %s", ise.getMessage());
        } catch (Throwable t) {
            getLogger().atWarning().withCause(t).log("Failed to add teleport task: %s", t.getMessage());
        }
    }

    public boolean isIngame() {
        return this.state.equals(GameState.INGAME_DEATHMATCH_PHASE) || this.state.equals(GameState.INGAME_MAIN_PHASE);
    }

    /**
     * Znajduje gracza w liście aktywnych graczy po UUID
     */
    private HgPlayer findHgPlayerByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        synchronized (activePlayers) {
            for (HgPlayer hgPlayer : activePlayers) {
                if (hgPlayer.getUuid().equals(uuid)) {
                    return hgPlayer;
                }
            }
        }
        return null;
    }

    /**
     * Zapisuje gracza do bazy danych
     */
    private void savePlayerToDatabase(HgPlayer hgPlayer) {
        if (playerRepository == null || hgPlayer == null) {
            return;
        }
        try {
            // Sprawdzenie czy gracz istnieje, jeśli tak to update, jeśli nie to save
            if (playerRepository.exists(hgPlayer.getUuid())) {
                playerRepository.update(hgPlayer);
            } else {
                playerRepository.save(hgPlayer);
            }
        } catch (Exception e) {
            getLogger().atWarning().withCause(e)
                    .log("Failed to save player %s to database: %s", hgPlayer.getPlayerName(), e.getMessage());
        }
    }

}
