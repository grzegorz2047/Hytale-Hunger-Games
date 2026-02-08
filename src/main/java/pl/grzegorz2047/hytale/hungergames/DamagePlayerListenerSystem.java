package pl.grzegorz2047.hytale.hungergames;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import javax.annotation.Nonnull;

public class DamagePlayerListenerSystem extends DamageEventSystem {

    private final HungerGames plugin;
    private final ArenaManager arenaManager;

    public DamagePlayerListenerSystem(HungerGames plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        // CRITICAL: Register in the Filter Damage Group to run BEFORE damage is applied
        // Default is Inspect group (after damage), which cannot prevent damage
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Return Query.any() to handle all entities that receive damage
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        // Skip if already cancelled by another system
        if (damage.isCancelled()) {
            return;
        }

        // Get reference to the damaged entity
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        // Check if target is a player
        Player player = store.getComponent(targetRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        String username = player.getPlayerRef().getUsername();

        // Check if player has godmode enabled
        World world = player.getWorld();
        if (arenaManager.isInArenaLobby(world.getName(), player.getUuid())) {
            // Cancel all damage for godmode players
            damage.setCancelled(true);
            player.sendMessage(Message.translation(
                    String.format("Arena Lobby: Blocked %.1f damage!", damage.getAmount())));
        }
    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }
}