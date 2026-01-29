package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.lib.playerinteractlib.message.MessageColorUtil;

public class DropItemListenerSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {
    private final HungerGames hungerGames;
    private final ArenaManager arenaManager;

    public DropItemListenerSystem(HungerGames hungerGames, ArenaManager arenaManager) {
        super(DropItemEvent.Drop.class);
        this.hungerGames = hungerGames;
        this.arenaManager = arenaManager;
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl DropItemEvent.Drop event) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;
        ItemStack itemStack = event.getItemStack();
        player.sendMessage(Message.raw(itemStack.getItemId()));
        if(itemStack.getItemId().equalsIgnoreCase("Prototype_Tool_Book_Mana")) {
            player.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>You cannot drop it</color>"));
            event.setCancelled(true);
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
