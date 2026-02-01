package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

public class PlaceBlockListenerSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private final HungerGames plugin;
    private final ArenaManager arenaManager;

    public PlaceBlockListenerSystem(HungerGames plugin, ArenaManager arenaManager) {
        super(PlaceBlockEvent.class);
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl PlaceBlockEvent event) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;

        if (!this.arenaManager.canBreak(world.getName())) {
            player.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>You cannot place it</color>"));
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getItemInHand();

        if (item == null) {
            return;
        }
        boolean hasChestInTheName = item.getItemId().toLowerCase().contains("chest");
        if (!hasChestInTheName) {
            return;
        }


    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }


}