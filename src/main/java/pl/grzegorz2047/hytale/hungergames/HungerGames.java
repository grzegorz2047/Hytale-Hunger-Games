package pl.grzegorz2047.hytale.hungergames;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.arena.stat.ArenaStat;
import pl.grzegorz2047.hytale.hungergames.commands.hg.HungerGamesCommand;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.events.PlayerInteractMouseEventListener;
import pl.grzegorz2047.hytale.hungergames.listeners.PlayerListeners;
import pl.grzegorz2047.hytale.hungergames.systems.BreakBlockListenerSystem;
import pl.grzegorz2047.hytale.hungergames.systems.DropItemListenerSystem;
import pl.grzegorz2047.hytale.hungergames.systems.InventoryUseListenerSystem;
import pl.grzegorz2047.hytale.hungergames.systems.PlaceBlockListenerSystem;
import pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractLib;
import pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractionEvent;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;


/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class HungerGames extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ArenaManager arenaManager = new ArenaManager();
    private Config<MainConfig> config;
    //    private final SubmissionPublisher<PlayerInteractionEvent> publisher = new SubmissionPublisher<>();

    public HungerGames(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @NullableDecl
    @Override
    public CompletableFuture<Void> preLoad() {
        this.config = loadConfig();
        return super.preLoad();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
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
                PlayerRef playerRef = Universe.get().getPlayer(UUID.fromString(item.uuid()));
                World world = Universe.get().getWorld(playerRef.getWorldUuid());

                if (s.equalsIgnoreCase("Prototype_Tool_Staff_Mana")) {
                    arenaManager.teleportToLobby(playerRef);
                    return;
                }
                if (s.equalsIgnoreCase("Prototype_Tool_Book_Mana")) {

                    world.execute(() -> {
                        String html = """
                                <div class="page-overlay">
                                    <div class="container" data-hyui-title="{{$title}}" style="anchor-height: 800;anchor-width: 900;">
                                        <div class="container-contents" style="layout-mode: Top; ">
                                            <p id="summary" style="padding: 4;">{{$summary}}</p>
                                aaa
                                            <div id="list" style="layout-mode: Top; padding: 6; ">
                                                {{#each arenas}}
                                                <div class="bounty-card" style="layout-mode: Left; padding: 10;">
                                                    <p style="flex-weight: 2;">{{$worldName}}</p>
                                                    <p style="flex-weight: 1;">{{$activePlayerCount}}/{{$arenaSize}}</p>
                                                    <p style="flex-weight: 1;">ingame: {{$ingame}}</p>
                                                    <button value="aaa" data="bbb" id="btn-{{$worldName}}" class="small-tertiary-button">Join</button>
                                                </div>
                                                {{/each}}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                """;
                        Player player = findPlayerInPlayerComponentsBag(playerRef.getReference().getStore(), playerRef.getReference());
                        if (player == null) {
                            return;
                        }
                        LinkedList<ArenaStat> arenaStats = arenaManager.getArenaStats();
                        TemplateProcessor template = new TemplateProcessor()
                                .setVariable("title", "Arena list")
                                .setVariable("summary", "Showing " + arenaStats.size() + " arenas")
                                .setVariable("arenas", arenaStats);

                        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                                .fromTemplate(html, template)
                                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);
                        arenaStats.forEach(arenaStat -> {
                            pageBuilder.addEventListener("btn-" + arenaStat.worldName(), CustomUIEventBindingType.Activating, (data, ctx) -> {
                                System.out.println(data);
                                ctx.getPage().ifPresent(HyUIPage::close);
                                arenaManager.joinArena(arenaStat.worldName(), player);
                            });
                        });
                        pageBuilder
                                .open(playerRef.getReference().getStore());

                    });
                }
                debugInteraction(item);
//                System.out.println("event " + item.interactionType());
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });
        new PlayerListeners(this, arenaManager).register(getEventRegistry());
        new PlayerInteractMouseEventListener(this).register(getEventRegistry());
        new InventoryUseListenerSystem(this, arenaManager, config.get()).register(getEntityStoreRegistry());
        new PlaceBlockListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        new BreakBlockListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        new DropItemListenerSystem(this, arenaManager).register(getEntityStoreRegistry());
        this.getCommandRegistry().registerCommand(new HungerGamesCommand(this.getName(), this.getManifest().getVersion().toString(), arenaManager));
    }

    private static void debugInteraction(PlayerInteractionEvent item) {
        SyncInteractionChain i = item.interaction();
        InteractionChainData d = i.data;

        System.out.println("========== INTERACTION DEBUG ==========");
        System.out.println("chainId: " + i.chainId);
        System.out.println("type: " + i.interactionType);
        System.out.println("state: " + i.state);
        System.out.println("initial: " + i.initial);
        System.out.println("desync: " + i.desync);

        System.out.println("itemInHandId: " + i.itemInHandId);
        System.out.println("utilityItemId: " + i.utilityItemId);
        System.out.println("toolsItemId: " + i.toolsItemId);

        if (d == null) {
            System.out.println("data: null");
        } else {
            System.out.println("entityId: " + d.entityId);
            System.out.println("proxyId: " + d.proxyId);
            System.out.println("targetSlot: " + d.targetSlot);

            System.out.println("hitLocation: " + d.hitLocation);
            System.out.println("hitNormal: " + d.hitNormal);
            System.out.println("hitDetail: " + d.hitDetail);
            System.out.println("blockPosition: " + d.blockPosition);
        }

        System.out.println("=======================================");
    }

    private Config<MainConfig> loadConfig() {
        config = this.withConfig("HungerGames", MainConfig.CODEC);
        this.config.load();
        this.config.save();
        return config;
    }

    @NullableDecl
    private static Player findPlayerInPlayerComponentsBag(Store<EntityStore> store, Ref<EntityStore> refEntityStore) {
        return store.getComponent(refEntityStore, Player.getComponentType());
    }
}

