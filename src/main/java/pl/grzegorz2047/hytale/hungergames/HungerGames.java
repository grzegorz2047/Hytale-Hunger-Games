package pl.grzegorz2047.hytale.hungergames;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.commands.hg.HungerGamesCommand;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.events.PlayerInteractMouseEventListener;
import pl.grzegorz2047.hytale.hungergames.hud.ArenaListPage;
import pl.grzegorz2047.hytale.hungergames.hud.HudService;
import pl.grzegorz2047.hytale.hungergames.listeners.PlayerListeners;
import pl.grzegorz2047.hytale.hungergames.systems.*;
import pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractLib;
import pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractionEvent;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;


/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class HungerGames extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ArenaManager arenaManager;
    private Config<MainConfig> config;
    private ArenaListPage arenaListPage;
    private HudService hudService;

    //    private final SubmissionPublisher<PlayerInteractionEvent> publisher = new SubmissionPublisher<>();

    public HungerGames(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @NullableDecl
    @Override
    public CompletableFuture<Void> preLoad() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        config = loadConfig();
        return super.preLoad();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        hudService = new HudService(config.get());
        this.arenaManager = new ArenaManager(config.get(), hudService);

        arenaListPage = new ArenaListPage(this.arenaManager, config.get());
        PlayerInteractLib playerInteractLib = (PlayerInteractLib) PluginManager.get().getPlugin(PluginIdentifier.fromString("Hytale:PlayerInteractLib"));

        SubmissionPublisher<PlayerInteractionEvent> instancePublisher = playerInteractLib.getPublisher();
        instancePublisher.subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(PlayerInteractionEvent item) {
                String s = item.itemInHandId();
                if (s == null) {
                    return;
                }
                UUID playerUuid = UUID.fromString(item.uuid());
                PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
                if (playerRef == null) {
                    return;
                }

                World world = Universe.get().getWorld(playerRef.getWorldUuid());
                if (world == null) {
                    return;
                }
                world.execute(() -> {
                    Player player = findPlayerInPlayerComponentsBag(playerRef.getReference().getStore(), playerRef.getReference());
                    if (player == null) {
                        return;
                    }
                    InteractionType interactionType = item.interactionType();
                    if (!(interactionType.equals(InteractionType.Primary) || interactionType.equals(InteractionType.Secondary))) {
                        return;
                    }
                    if (s.equalsIgnoreCase("Prototype_Tool_Staff_Mana")) {
                        arenaManager.leaveArena(player);
                        return;
                    }
                    if (!s.equalsIgnoreCase("Prototype_Tool_Book_Mana")) {
                        return;
                    }
                    PageBuilder pageBuilder = arenaListPage.prepareArenaListPage(playerRef, player, HungerGames.this.arenaManager.getArenaStats());
                    pageBuilder
                            .open(playerRef.getReference().getStore());


//                    debugInteraction(item);
//                System.out.println("event " + item.interactionType());
                });
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });
        new PlayerListeners(this, arenaManager, config, hudService).register(getEventRegistry());
        new PlayerInteractMouseEventListener(this).register(getEventRegistry());
        new InventoryUseListenerSystem(this, arenaManager, config.get()).register(getEntityStoreRegistry());
        new PlaceBlockListenerSystem(this, arenaManager, config.get()).register(getEntityStoreRegistry());
        new BreakBlockListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        new DropItemListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        new DeathPlayerListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        new DamagePlayerListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        this.getCommandRegistry().registerCommand(new HungerGamesCommand(this.getName(), this.getManifest().getVersion().toString(), arenaManager, config.get(), arenaListPage));
    }

    private Config<MainConfig> loadConfig() {
        config = this.withConfig("HungerGames", MainConfig.CODEC);
        config.load();
        config.save();
        return config;
    }
}

