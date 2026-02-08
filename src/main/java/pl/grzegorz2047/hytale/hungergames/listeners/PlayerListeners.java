package pl.grzegorz2047.hytale.hungergames.listeners;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.hud.HudService;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;
import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerRefInPlayerRefComponentsBag;

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
    private final HudService hudService;

    public PlayerListeners(HungerGames plugin, ArenaManager arenaManager, Config<MainConfig> config, HudService hudService) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.config = config;
        this.hudService = hudService;
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


    private void onPlayerWorldEnter(AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        Player player = getPlayer(holder);
        if (player == null) return;
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        World world = event.getWorld();
        if (world.equals(Universe.get().getDefaultWorld())) {
            boolean isHudEnabled = config.get().isHudEnabled();
            if (isHudEnabled) {
                try {
                    var globalKills = arenaManager.getGlobalKills(playerRef);
                    if (globalKills.isPresent()) {
                        String kills = config.get().getTranslation("hungergames.hud.lobby.globalKills").replace("{kills}", String.valueOf(globalKills.get().getGlobalKills()));
                        hudService.initLobbyHud(player.getHudManager(), playerRef, kills);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    @NullableDecl
    private static Player getPlayer(Holder<EntityStore> holder) {
        return holder.getComponent(Player.getComponentType());
    }

    private void onPlayerWorldLeave(DrainPlayerFromWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
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
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Holder<EntityStore> holder = event.getHolder();

        if (config.get().forceLobbySpawn()) {
            event.setWorld(Universe.get().getDefaultWorld());
            holder.removeComponent(TransformComponent.getComponentType());
        }
    }

    /**
     * Handle player disconnect event.
     *
     * @param event The player disconnect event
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        arenaManager.playerDisconnected(playerRef);
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
        boolean playerOnAnyArena = arenaManager.isPlayerOnAnyArena(player);
        if(world.equals(Universe.get().getDefaultWorld())) {
            arenaManager.preparePlayerJoinedServer(player);
        }
    }
}
