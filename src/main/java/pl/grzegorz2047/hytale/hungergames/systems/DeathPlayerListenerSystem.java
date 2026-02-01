package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

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
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        Universe.get().sendMessage(Message.raw("Death player: " + playerComponent.getDisplayName()));
        Damage deathInfo = component.getDeathInfo();
        String arenaName = playerComponent.getWorld().getName();

        if (deathInfo != null) {
            Damage.Source source = deathInfo.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) {
                arenaManager.playerDied(playerComponent, playerComponent.getWorld());
                return;
            }
            Ref<EntityStore> sourceRef = entitySource.getRef();
            if (!sourceRef.isValid()) {
                return;
            }

            Player attacker = (Player)store.getComponent(sourceRef, Player.getComponentType());
            if (attacker == null) {
                arenaManager.playerDied(playerComponent, playerComponent.getWorld());
            }
            if (arenaManager.isPlayerPlayingOnArena(arenaName)) {
                arenaManager.playerDied(playerComponent, playerComponent.getWorld(), attacker);
            }

        }
    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }
}