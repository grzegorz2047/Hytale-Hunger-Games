package pl.grzegorz2047.hytale.hungergames;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import pl.grzegorz2047.hytale.hungergames.commands.hg.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.events.PlayerInteractMouseEventListener;
import pl.grzegorz2047.hytale.hungergames.systems.InventoryUseListenerSystem;
import pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractLib;

import javax.annotation.Nonnull;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class HungerGames extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ArenaManager arenaManager = new ArenaManager();
//    private final SubmissionPublisher<PlayerInteractionEvent> publisher = new SubmissionPublisher<>();

    public HungerGames(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        PlayerInteractLib playerInteractLib = (PlayerInteractLib) PluginManager.get().getPlugin(PluginIdentifier.fromString("Hytale:PlayerInteractLib"));

        SubmissionPublisher<pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractionEvent> instancePublisher = playerInteractLib.getPublisher();
        instancePublisher.subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractionEvent item) {
                debugInteraction(item);
//                System.out.println("event " + item.interactionType());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
        new PlayerInteractMouseEventListener(this).register(getEventRegistry());
        new InventoryUseListenerSystem(this).register(getEntityStoreRegistry());
        this.getCommandRegistry().registerCommand(new HungerGamesCommand(this.getName(), this.getManifest().getVersion().toString(), arenaManager));
    }

    private static void debugInteraction(pl.grzegorz2047.hytale.lib.playerinteractlib.PlayerInteractionEvent item) {
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
}

