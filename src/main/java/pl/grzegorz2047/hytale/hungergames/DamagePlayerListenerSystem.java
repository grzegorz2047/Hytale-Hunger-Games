package pl.grzegorz2047.hytale.hungergames;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import javax.annotation.Nonnull;
import java.util.Objects;

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

        // Check if target is a playerDamaged
        Player playerDamaged = store.getComponent(targetRef, Player.getComponentType());
        if (playerDamaged == null) {
            return;
        }
        String username = playerDamaged.getPlayerRef().getUsername();

        // Check if playerDamaged has godmode enabled
        World world = playerDamaged.getWorld();
//        System.out.println("DAMAMAMAMAMAMMGE");
        float damageAmount = damage.getAmount();
        if (arenaManager.isInArenaLobby(world.getName(), playerDamaged.getUuid())) {
            // Cancel all damage for godmode players
            damage.setCancelled(true);
            playerDamaged.sendMessage(Message.translation(
                    String.format("Arena Lobby: Blocked %.1f damage!", damageAmount)));
        } else if (arenaManager.isInGracePeriod(world.getName(), playerDamaged.getUuid())) {
            // Check if damage is from another player during grace period
            Damage.Source source = damage.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> sourceRef = entitySource.getRef();
                if (sourceRef.isValid()) {
                    Player attacker = store.getComponent(sourceRef, Player.getComponentType());
                    if (attacker != null) {
                        // This is player vs player damage during grace period - cancel it
                        damage.setCancelled(true);
                        attacker.sendMessage(Message.translation(
                                "Grace period is active! You cannot damage other players yet."));
                        return;
                    }
                }
            }
            // Allow environmental damage during grace period (falls, etc.)
        } else {

            EntityStatMap entityStatMapComponent = chunk.getComponent(index, EntityStatMap.getComponentType());

            assert entityStatMapComponent != null;

            int healthStat = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthValue = entityStatMapComponent.get(healthStat);
            Objects.requireNonNull(healthValue);
            boolean isDead = chunk.getArchetype().contains(DeathComponent.getComponentType());
            if (isDead) {
                damage.setCancelled(true);
            } else {
                float healthAmount = healthValue.get();
//                System.out.println("Health value: " + healthValue.get());
//                System.out.println("Health value: " + healthValue.asPercentage());
//                System.out.println("Health: " + healthAmount);
//                System.out.println("Health min " + healthValue.getMin());
//                System.out.println("Damage: " + damageAmount);

                float newValue = healthAmount - damageAmount;
//                System.out.println("AAAAAND DAMAGE: " + damageAmount + " NEW HEALTH: " + newValue);
                if (!arenaManager.isArenaIngame(world.getName())) {
                    return;
                }
                if (newValue <= healthValue.getMin()) {
//                    System.out.println("DAWDWDaDWbbbbbbaaaaa");
                    damage.setCancelled(true);
                    Damage.Source source = damage.getSource();
                    if (!(source instanceof Damage.EntitySource entitySource)) {
                        arenaManager.playerDied(playerDamaged, world);
//                        System.out.println("ded eleon");
                        return;
                    }
                    Ref<EntityStore> sourceRef = entitySource.getRef();
                    if (!sourceRef.isValid()) {
                        return;
                    }
                    playerDamaged.getWorld().execute(() -> {
//                        System.out.println("cancel!!!!!!");
                        Player attacker = store.getComponent(sourceRef, Player.getComponentType());
                        if (attacker == null) {
                            arenaManager.playerDied(playerDamaged, world);
//                            System.out.println("ded aleon!?@?@?@?@@");
                        } else {
                            arenaManager.playerDied(playerDamaged, world, attacker);
//                            System.out.println("killled!!!!!!!!!!!!!");

                        }
                    });
                } else {
                    System.out.println("ELESELSE AAAAAND DAMAGE: " + damageAmount + " NEW HEALTH: " + newValue);

                }

            }
        }
    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }
}