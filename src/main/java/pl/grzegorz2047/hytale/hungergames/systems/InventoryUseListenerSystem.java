package pl.grzegorz2047.hytale.hungergames.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryUseListenerSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    private final HungerGames plugin;
    private final ArenaManager arenaManager;
    private final List<ItemStack> stacks;
    private final MainConfig mainConfig;

    public InventoryUseListenerSystem(HungerGames plugin, ArenaManager arenaManager, MainConfig mainConfig) {
        super(UseBlockEvent.Pre.class);
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.mainConfig = mainConfig;
        stacks = List.of(mainConfig.getItemsToFillChest());
    }


    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl UseBlockEvent.Pre useBlockEventPre) {

        Ref<EntityStore> playerRef = useBlockEventPre.getContext().getEntity();
        Player player = store.getComponent(useBlockEventPre.getContext().getEntity(), Player.getComponentType());
        if (player == null) {
            return;
        }

        Vector3i target = useBlockEventPre.getTargetBlock();
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        String worldName = world.getName();
        if (!arenaManager.isArenaIngame(worldName)) {
            return;
        }
        if (!arenaManager.isPlayerPlayingOnArena(worldName)) {
            return;
        }
        BlockState blockType = world.getState(target.getX(), target.getY(), target.getZ(), true);

        if (blockType instanceof ItemContainerState itemContainerState) {
            if (useBlockEventPre.getInteractionType().toString().equals("Use")) {

                Map<UUID, ContainerBlockWindow> windows = itemContainerState.getWindows();
                if (!windows.isEmpty()) {
                    windows.forEach((key, value) -> {
                        ItemContainer itemContainer = value.getItemContainer();
                        ItemStack itemStack = itemContainer.getItemStack((short) 0);
                        assert itemStack != null;
                        String itemId = itemStack.getItemId();
                        System.out.println(itemId);
                    });
                    useBlockEventPre.setCancelled(true);
                    return;
                }
//                player.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>Opened inventory</color>"));
                Vector3i position = blockType.getPosition();
                if (arenaManager.isBlockOpenedInArena(position, worldName)) {
                    return;
                }
                short chestSize = itemContainerState.getItemContainer().getCapacity();
                List<Short> slots = shuffleSlots(chestSize);

                ClearTransaction clearContainer = itemContainerState.getItemContainer().clear();
                boolean chestIsClear = clearContainer.succeeded();
                if (chestIsClear) {
                    setItemsInChestInRandomizedSlots(itemContainerState, stacks, slots);
                    arenaManager.addBlockOpenedInArena(position, worldName);
                } else {
                    useBlockEventPre.setCancelled(true);
                }

            }
        }
    }

    @NonNullDecl
    private static List<Short> shuffleSlots(short chestSize) {
        List<Short> slots = new ArrayList<>();
        for (short s = 0; s < chestSize; s++) {
            slots.add(s);
        }
        Collections.shuffle(slots, ThreadLocalRandom.current());
        return slots;
    }

    private static void setItemsInChestInRandomizedSlots(ItemContainerState itemContainerState, List<ItemStack> stacks, List<Short> slots) {
        int maxFill = Math.min(stacks.size(), slots.size());
        if (maxFill <= 0) {
            return;
        }
        int itemsToPlace = ThreadLocalRandom.current().nextInt(1, maxFill + 1);
        List<ItemStack> shuffledStacks = new ArrayList<>(stacks);
        Collections.shuffle(shuffledStacks, ThreadLocalRandom.current());
        for (int i = 0; i < itemsToPlace; i++) {
            short arraySlotNumer = slots.get(i);
            ItemStack itemStack = shuffledStacks.get(i);
            itemContainerState.getItemContainer().setItemStackForSlot(arraySlotNumer, itemStack);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public void register(ComponentRegistryProxy<EntityStore> eventbus) {
        eventbus.registerSystem(this);
    }
}
