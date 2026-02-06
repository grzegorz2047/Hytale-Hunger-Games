package pl.grzegorz2047.hytale.hungergames.listeners;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.arena.HgPlayer;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.hud.MinigameHud;
import pl.grzegorz2047.hytale.hungergames.hud.LobbyHud;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;
import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerRefInPlayerRefComponentsBag;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Listener for player connection events.
 * <p>
 * Listens to:
 * - PlayerConnectEvent - When a player connects to the server
 * - PlayerDisconnectEvent - When a player disconnects from the server
 */
public class PlayerListeners {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final HungerGames plugin;
    private final ArenaManager arenaManager;
    private final Config<MainConfig> config;

    public PlayerListeners(HungerGames plugin, ArenaManager arenaManager, Config<MainConfig> config) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.config = config;
    }

    /**
     * Register all player event listeners.
     *
     * @param eventBus The event registry to register listeners with
     */
    public void register(EventRegistry eventBus) {

        eventBus.register(PlayerConnectEvent.class, this::onPlayerConnect);
        eventBus.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        eventBus.registerGlobal(PlayerReadyEvent.class, this::onPlayerJoin);
        eventBus.registerGlobal(DrainPlayerFromWorldEvent.class, this::onPlayerWorldLeave);
        eventBus.registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerWorldEnter);
    }


    private void onPlayerWorldEnter(AddPlayerToWorldEvent addPlayerToWorldEvent) {
        Holder<EntityStore> holder = addPlayerToWorldEvent.getHolder();
        World world = addPlayerToWorldEvent.getWorld();
        Player player = getPlayer(holder);
        if (player == null) return;
        boolean playerOnAnyArena = arenaManager.isPlayerOnAnyArena(player);
        HudManager hudManager = player.getHudManager();
        PlayerRef playerRef = player.getPlayerRef();
        boolean isHudEnabled = this.config.get().isHudEnabled();
        if (isHudEnabled) {
            initMinigameHud(playerOnAnyArena, hudManager, playerRef);
        }
    }

    private void initMinigameHud(boolean playerOnAnyArena, HudManager hudManager, PlayerRef playerRef) {
        if (playerOnAnyArena) {
            hudManager.setCustomHud(playerRef, new MinigameHud(playerRef, 24, 300, true));
//            MultipleHUD.getInstance().setCustomHud(player,playerRef,"hg_scoreboard", new MinigameHud(playerRef, 24, 300, true));
        } else {
            String tpl = this.config.get().getTranslation("hungergames.hud.lobby.welcome");
            String formatted = tpl.replace("{username}", playerRef.getUsername());
            LobbyHud lobbyHud = new LobbyHud(playerRef, 24, formatted);
            hudManager.setCustomHud(playerRef, lobbyHud);
            Optional<HgPlayer> globalKills;
            try {
                globalKills = arenaManager.getGlobalKills(playerRef);
                if (globalKills.isPresent()) {
                    String kills = this.config.get().getTranslation("hungergames.hud.lobby.globalKills").replace("{kills}", String.valueOf(globalKills.get().getGlobalKills()));
                    lobbyHud.setKillStats(kills);

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

//            MultipleHUD.getInstance().hideCustomHud(player,"scoreboard_hg");
        }
    }


    @NullableDecl
    private static Player getPlayer(Holder<EntityStore> holder) {
        return holder.getComponent(Player.getComponentType());
    }

    private void onPlayerWorldLeave(DrainPlayerFromWorldEvent drainPlayerFromWorldEvent) {
        Holder<EntityStore> holder = drainPlayerFromWorldEvent.getHolder();
        Player player = getPlayer(holder);
        if (player == null) return;
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef != null) {
            arenaManager.playerLeft(playerRef);
        }
        Inventory inventory = player.getInventory();
    }


    /**
     * Handle player connect event.
     *
     * @param event The player connect event
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        String playerName = event.getPlayerRef() != null ? event.getPlayerRef().getUsername() : "Unknown";
        String worldName = event.getWorld() != null ? event.getWorld().getName() : "unknown";

        LOGGER.at(Level.INFO).log("[MyHytaleMod] Player %s connected to world %s", playerName, worldName);

        // TODO: Add your player join logic here
        // Examples:
        // - Send welcome message
        // - Load player data
        // - Announce join to other players
    }

    /**
     * Handle player disconnect event.
     *
     * @param event The player disconnect event
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        arenaManager.playerLeft(playerRef);
    }

    private void onPlayerJoin(PlayerReadyEvent event) {
        Ref<EntityStore> refEntityStore = event.getPlayerRef();
        if (!refEntityStore.isValid()) {
            return;
        }
        Store<EntityStore> store = refEntityStore.getStore();
        EntityStore externalData = store.getExternalData();
        World world = externalData.getWorld();
        PlayerRef playerRef = findPlayerRefInPlayerRefComponentsBag(store, refEntityStore);
        if (playerRef == null) {
            return;
        }
        Player player = findPlayerInPlayerComponentsBag(store, refEntityStore);
        if (player == null) {
            return;
        }
        boolean playerFirstJoin = player.isFirstSpawn();
        arenaManager.preparePlayerJoinedServer(player);
        if (!arenaManager.isPlayerOnAnyArena(player)) {
//            world.execute(()
//                    -> LobbyTeleporter.teleportToLobby(playerRef));

        }
    }
}
