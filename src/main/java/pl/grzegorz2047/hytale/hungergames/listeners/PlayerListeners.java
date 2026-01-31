package pl.grzegorz2047.hytale.hungergames.listeners;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;


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

    public PlayerListeners(HungerGames plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
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
    }

    private void onPlayerWorldLeave(DrainPlayerFromWorldEvent drainPlayerFromWorldEvent) {
        Holder<EntityStore> holder = drainPlayerFromWorldEvent.getHolder();
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }
        arenaManager.playerLeft(playerRef);
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
        String playerName = event.getPlayerRef().getUsername();
        arenaManager.playerLeft(event.getPlayerRef());
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

    }

    @NullableDecl
    public static PlayerRef findPlayerRefInPlayerRefComponentsBag(Store<EntityStore> store, Ref<EntityStore> refEntityStore) {
        return store.getComponent(refEntityStore, PlayerRef.getComponentType());
    }

    @NullableDecl
    private static Player findPlayerInPlayerComponentsBag(Store<EntityStore> store, Ref<EntityStore> refEntityStore) {
        return store.getComponent(refEntityStore, Player.getComponentType());
    }
}