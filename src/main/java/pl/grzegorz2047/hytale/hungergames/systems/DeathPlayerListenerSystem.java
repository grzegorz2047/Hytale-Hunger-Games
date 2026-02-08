package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
public class DeathPlayerListenerSystem extends DeathSystems.OnDeathSystem {
    private final HungerGames plugin;
    private final ArenaManager arenaManager;

    public DeathPlayerListenerSystem(HungerGames plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        Player diedPlayer = (Player) store.getComponent(ref, Player.getComponentType());
        assert diedPlayer != null;
        String tpl = arenaManager.getConfig().getTranslation("hungergames.death.playerBroadcast");
        String formatted = tpl == null ? "Death player: " + diedPlayer.getDisplayName() : tpl.replace("{player}", diedPlayer.getDisplayName());
        Universe.get().sendMessage(MessageColorUtil.rawStyled(formatted));
        Damage deathInfo = component.getDeathInfo();
        World world = diedPlayer.getWorld();
        String arenaName = world.getName();
        component.setShowDeathMenu(false);
        world.execute(() -> {
            if (deathInfo != null) {
                Damage.Source source = deathInfo.getSource();
                if (!(source instanceof Damage.EntitySource entitySource)) {
//                    arenaManager.playerDied(diedPlayer, world);
                    return;
                }
                Ref<EntityStore> sourceRef = entitySource.getRef();
                if (!sourceRef.isValid()) {
                    return;
                }

                Player attacker = (Player) store.getComponent(sourceRef, Player.getComponentType());
                if (attacker == null) {
//                    arenaManager.playerDied(diedPlayer, world);
                    return;
                }
                if (arenaManager.isPlayerPlayingOnArena(arenaName)) {
//                    arenaManager.playerDied(diedPlayer, world, attacker);
                }
            }
        });

    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }
}