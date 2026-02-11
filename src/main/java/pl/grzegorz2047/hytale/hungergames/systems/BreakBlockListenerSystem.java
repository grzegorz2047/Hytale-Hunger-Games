package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

public class BreakBlockListenerSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {


    private final HungerGames plugin;
    private final ArenaManager arenaManager;

    public BreakBlockListenerSystem(HungerGames hungerGames, ArenaManager arenaManager) {
        super(BreakBlockEvent.class);
        this.plugin = hungerGames;
        this.arenaManager = arenaManager;
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl BreakBlockEvent event) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;

        if (!this.arenaManager.canBreak(world.getName())) {
            String tpl = arenaManager.getConfig().getTranslation("hungergames.block.cannotBreak");
            player.sendMessage(MessageColorUtil.rawStyled(tpl));
            event.setCancelled(true);
            return;
        }
        BlockType blockType = event.getBlockType();
        String itemId = blockType.getId();
        String tplId = arenaManager.getConfig().getTranslation("hungergames.block.id");
        String formatted = tplId == null ? itemId : tplId.replace("{id}", itemId);
//        System.out.println(formatted);
        if (!isAChestNamed(event, player, itemId.toLowerCase())) return;

    }

    private static boolean isAChestNamed(@NonNullDecl BreakBlockEvent event, Player player, String itemId) {
        return itemId.contains("chest");
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public void register(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(this);
    }
}
