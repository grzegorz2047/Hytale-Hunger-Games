package pl.grzegorz2047.hytale.hungergames.arena;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.system.WorldConfigSaveSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.arena.stat.ArenaStat;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;
import pl.grzegorz2047.hytale.hungergames.db.ArenaRepository;
import pl.grzegorz2047.hytale.hungergames.db.InMemoryRepository;
import pl.grzegorz2047.hytale.hungergames.db.SqliteArenaRepository;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/*
...existing code...
*/
public class ArenaManager {
    private final int maxHeight = 10;
    private final int spawnHeightYPos = maxHeight + 20;
    private final MainConfig config;
    HashMap<String, HgArena> listOfArenas = new HashMap<>();

    private final ArenaRepository repository;
    private final boolean startSchedulerOnArenaCreate;

    // Domyślny konstruktor: próbuje użyć SQLite, w razie błędu fallback do in-memory
    public ArenaManager(MainConfig config) {
        this(config, null, true);
    }

    // Konstruktor do wstrzyknięcia innego repozytorium (np. zewnętrzne DB lub mock w testach)
    public ArenaManager(MainConfig config, ArenaRepository repository) {
        this(config, repository, true);
    }

    // Konstruktor z kontrolą schedulera (np. testy jednostkowe)
    public ArenaManager(MainConfig config, ArenaRepository repository, boolean startSchedulerOnArenaCreate) {
        this.config = config;
        this.startSchedulerOnArenaCreate = startSchedulerOnArenaCreate;
        ArenaRepository repo = repository;
        if (repo == null) {
            try {
                File dataDir = new File("data");
                if (!dataDir.exists()) dataDir.mkdirs();
                repo = new SqliteArenaRepository("data/arenas.db", config);
                repo.initialize();
            } catch (Throwable t) {
                System.out.println("Warning: SQLite unavailable, using in-memory repository: " + t.getMessage());
                repo = new InMemoryRepository();
            }
        } else {
            try {
                repo.initialize();
            } catch (Throwable ignored) {
            }
        }
        this.repository = repo;
        loadFromRepository();
    }

    // Dodany getter do konfiguracji
    public MainConfig getConfig() {
        return this.config;
    }

    public boolean createArena(String worldName, List<Vector3d> spawnPoints, Vector3d lobbySpawnLocation) {
        HgArena arena = new HgArena(worldName, spawnPoints, lobbySpawnLocation, this.config, this.startSchedulerOnArenaCreate);
        this.listOfArenas.put(worldName, arena);
        try {
            repository.save(arena);
        } catch (Throwable t) {
            System.out.println("Warning: failed to persist arena " + worldName + " : " + t.getMessage());
        }
        return true;
    }

    public boolean forceStartArena(String arenaName, Player player) {
        if (!this.arenaExists(arenaName)) {
            return false;
        }
        HgArena hgArena = getArena(arenaName);
        if (hgArena == null) {
            return false;
        }
        hgArena.join(player.getWorld(), player.getUuid());
        return hgArena.forceStart();
    }

    public boolean arenaExists(String worldName) {
        return this.listOfArenas.containsKey(worldName);
    }

    public boolean canBreak(String worldName) {
        if (!this.arenaExists(worldName)) {
            return true;
        }
        HgArena arena = this.getArena(worldName);
        if (arena == null) {
            return true;
        }
        // Blokuj niszczenie jeśli arena jest aktywna LUB trwa gra
        return !arena.isActive() && !arena.isIngame();
    }

    private HgArena getArena(String worldName) {
        return this.listOfArenas.get(worldName);
    }

    public void setEnableArena(String worldName, boolean value) {
        HgArena a = this.getArena(worldName);
        if (a == null) return;
        a.setActive(value);
        try {
            repository.save(a);
        } catch (Throwable t) {
            System.out.println("Warning: failed to persist arena state " + worldName + " : " + t.getMessage());
        }
    }

    // Ładowanie wszystkich aren z repozytorium
    private void loadFromRepository() {
        try {
            Map<String, HgArena> loaded = repository.loadAll();
            if (loaded != null) {
                this.listOfArenas.putAll(loaded);
            }
        } catch (Throwable t) {
            System.out.println("Warning: failed to load arenas from repository: " + t.getMessage());
        }
    }


    public void joinArena(String arenaName, Player player) {
        if (!this.arenaExists(arenaName)) {
            String tpl = this.config.getTranslation("hungergames.arena.notFound");
            player.sendMessage(MessageColorUtil.rawStyled(tpl));
            return;
        }
        HgArena arena = this.getArena(arenaName);
        if (!arena.isActive()) {
            String tpl = this.config.getTranslation("hungergames.arena.notActive");
            player.sendMessage(MessageColorUtil.rawStyled(tpl));
            return;
        }
        if (isPlayerOnAnyArena(player)) {
            String tpl = this.config.getTranslation("hungergames.arena.alreadyOnArena");
            player.sendMessage(MessageColorUtil.rawStyled(tpl));
            return;
        }
        arena.join(player.getWorld(), player.getUuid());
    }


    public boolean isArenaIngame(String arenaName) {
        if (!this.arenaExists(arenaName)) return false;
        HgArena arena = getArena(arenaName);
        return arena != null && arena.isIngame();
    }

    public void createArenaWithSpace(@NonNullDecl CommandContext context, String worldName, CommandSender sender, int numberOfSpawnPoint, int radius) {
        String generatorType = "Flat";
        BuilderCodec<? extends IWorldGenProvider> providerCodec =
                IWorldGenProvider.CODEC.getCodecFor(generatorType);

        if (providerCodec == null) {
            throw new IllegalArgumentException("Unknown generatorType '" + generatorType + "'");
        }
        Vector3d lobbySpawnLocation = new Vector3d(0, spawnHeightYPos, 0);
        CompletableFutureUtil._catch(
                Universe.get().addWorld(worldName, generatorType, "default")
                        .thenAccept(world -> {

                            String tpl = this.config.getTranslation("server.universe.addWorld.worldCreated");
                            String formatted = tpl == null ? "" : tpl.replace("{worldName}", worldName).replace("{generator}", generatorType).replace("{storage}", "default");
                            sender.sendMessage(MessageColorUtil.rawStyled(formatted));

                            BlockType blockType = BlockType.fromString("Soil_Sand_Red");

                            List<CompletableFuture<Void>> generationTasks = new ArrayList<>();
                            List<Vector3d> spawnPoints = new ArrayList<>();
                            for (int i = 0; i < numberOfSpawnPoint; i++) {
                                double angle = 2 * Math.PI * i / numberOfSpawnPoint;

                                int x = (int) Math.round(Math.cos(angle) * radius);
                                int y = 0;
                                int z = (int) Math.round(Math.sin(angle) * radius);
                                Vector3d spawnPoint = new Vector3d(x, y + 1, z);
                                spawnPoints.add(spawnPoint);
                                long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);


                                CompletableFuture<Void> task =
                                        generateSpawnPoints(context, world, chunkIndex, x, z, blockType, y);

                                generationTasks.add(task);
                            }
                            int hillRadius = 10;
                            generationTasks.addAll(generateLobbyAreaTasks(world, lobbySpawnLocation, BlockType.fromString("Wood_Blackwood_Ornate"), 10));
                            generationTasks.addAll(generateHillTasks(context, world, 0, 0, hillRadius, maxHeight, blockType));
                            CompletableFuture
                                    .allOf(generationTasks.toArray(new CompletableFuture[0]))
                                    .thenRun(() -> {
                                        boolean isCreated = createArena(worldName, spawnPoints, lobbySpawnLocation);
                                        if (isCreated) {
                                            String tpl2 = this.config.getTranslation("hungergames.arena.generated");
                                            sender.sendMessage(MessageColorUtil.rawStyled(tpl2));
                                        }
                                        CompletableFuture.runAsync(() -> saveWorld(world), world).thenRun(() -> {
                                            String tpl3 = this.config.getTranslation("server.commands.world.save.savingDone");
                                            String formatted3 = tpl3 == null ? "" : tpl3.replace("{world}", world.getName());
                                            context.sendMessage(MessageColorUtil.rawStyled(formatted3));
                                             if (!(sender instanceof Player player)) {
                                                 return;
                                             }

                                             player.getWorld().execute(() -> {
                                                 addTeleportTask(
                                                         player.getReference(),
                                                         world,
                                                         lobbySpawnLocation
                                                 );
                                             });
                                         });
                                     });
                        })
        ).exceptionally(t -> {
            HytaleLogger.getLogger()
                    .at(Level.SEVERE).withCause(t)
                    .log("Failed to add world '%s'", worldName);
            return null;
        });
    }


    private List<CompletableFuture<Void>> generateLobbyAreaTasks(World world, Vector3d lobbySpawnLocation, BlockType blockType, int maxHeight) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        int radius = 10;
        int floorY = (int) lobbySpawnLocation.y - 1;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int worldX = (int) (lobbySpawnLocation.x + x);
                int worldZ = (int) (lobbySpawnLocation.z + z);

                long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);

                int localX = x;
                int localZ = z;
                CompletableFuture<Void> task = world.getChunkAsync(chunkIndex).thenAccept(chunk -> {
                    // podłoga
                    chunk.setBlock(worldX, floorY, worldZ, blockType);

                    // ściany tylko na obrzeżach
                    if (Math.abs(localX) == radius || Math.abs(localZ) == radius) {
                        for (int y = floorY + 1; y <= floorY + maxHeight; y++) {
                            chunk.setBlock(worldX, y, worldZ, blockType);
                        }
                    }
                }).exceptionally(t -> {
                    HytaleLogger.getLogger()
                            .at(Level.SEVERE)
                            .withCause(t)
                            .log("Error generating lobby area");
                    return null;
                });

                tasks.add(task);
            }
        }
        return tasks;
    }


    @NonNullDecl
    private CompletableFuture<Void> generateSpawnPoints(@NonNullDecl CommandContext context, World world, long chunkIndex, int x, int z, BlockType blockType, int y) {
        return world.getChunkAsync(chunkIndex)
                .thenAccept(chunk ->
                        executeWithBlock(
                                context,
                                chunk,
                                x,
                                y,
                                z,
                                blockType
                        )
                )
                .exceptionally(t -> {
                    HytaleLogger.getLogger()
                            .at(Level.SEVERE)
                            .withCause(t)
                            .log("Error generating spawnpoint");
                    return null;
                });
    }

    protected void executeWithBlock(
            @Nonnull CommandContext context,
            @Nonnull WorldChunk chunk,
            int x,
            int y,
            int z,
            BlockType blockType
    ) {
        chunk.setBlock(x, y, z, blockType);
    }

    private void addTeleportTask(
            Ref<EntityStore> playerRef,
            World world,
            Vector3d spawnCoordPos
    ) {
        if (playerRef == null) return;
        try {
            Store<EntityStore> store = playerRef.getStore();
            ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
            Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
            // addComponent może rzucić IllegalStateException gdy ref jest nieważny - obsłużemy to
            try {
                store.addComponent(playerRef, componentType, teleport);
            } catch (IllegalStateException ise) {
                HytaleLogger.getLogger().atWarning().withCause(ise).log("Invalid entity reference when adding teleport (ArenaManager): %s", ise.getMessage());
            }
        } catch (Throwable t) {
            HytaleLogger.getLogger().atWarning().withCause(t).log("Failed to add teleport task (ArenaManager): %s", t.getMessage());
        }
    }

    private List<CompletableFuture<Void>> generateHillTasks(
            CommandContext context,
            World world,
            int centerX,
            int centerZ,
            int radius,
            int maxHeight,
            BlockType blockType) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                double distance = Math.sqrt(x * x + z * z);

                if (distance > radius) {
                    continue;
                }

                int height = (int) Math.round(maxHeight - distance);

                int worldX = centerX + x;
                int worldZ = centerZ + z;

                long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);

                int localZ = z;
                int localX = x;
                CompletableFuture<Void> task = world.getChunkAsync(chunkIndex).thenAccept(chunk -> {
                    for (int y = 0; y <= height; y++) {
                        chunk.setBlock(worldX, y, worldZ, blockType);
                    }
                    if (Math.abs(localX) == radius || Math.abs(localZ) == radius) {
                        for (int y = 0; y <= height; y++) {
                            chunk.setBlock(worldX, y, worldZ, "Furniture_Dungeon_Chest_Epic");
                        }
                    }
                });
                tasks.add(task);
            }
        }
        return tasks;
    }

    @Nonnull
    private static CompletableFuture<Void> saveWorld(@Nonnull World world) {
        return CompletableFuture.allOf(WorldConfigSaveSystem.saveWorldConfigAndResources(world), ChunkSavingSystems.saveChunksInWorld(world.getChunkStore().getStore()));
    }

    public void createArena(String worldName, int numberOfSpawnPoint) {
        int radius = 25;
        List<Vector3d> spawnPoints = generateSpawnPoints(numberOfSpawnPoint, radius);
        Vector3d lobbySpawnLocation = new Vector3d(0, spawnHeightYPos, 0);
        createArena(worldName, spawnPoints, lobbySpawnLocation);
    }

    @NonNullDecl
    private static List<Vector3d> generateSpawnPoints(int numberOfSpawnPoint, int radius) {
        List<Vector3d> spawnPoints = new ArrayList<>();
        for (int i = 0; i < numberOfSpawnPoint; i++) {
            double angle = 2 * Math.PI * i / numberOfSpawnPoint;

            int x = (int) Math.round(Math.cos(angle) * radius);
            int y = 0;
            int z = (int) Math.round(Math.sin(angle) * radius);
            Vector3d spawnPoint = new Vector3d(x, y + 1, z);
            spawnPoints.add(spawnPoint);
        }
        return spawnPoints;
    }

    public boolean isPlayerPlayingOnArena(String arenaName) {
        if (!this.arenaExists(arenaName)) return false;
        HgArena arena = getArena(arenaName);
        if (arena == null) return false;
        // zwraca true jeśli ktoś aktualnie jest przypisany do areny (oczekuje lub gra)
        return arena.getActivePlayerCount() > 0;
    }

    public void preparePlayerJoinedServer(Player player) {
        Inventory inventory = player.getInventory();
        inventory.clear();
        inventory.setActiveHotbarSlot((byte) 0);
        ItemContainer hotbar = inventory.getHotbar();
        ItemStack arenaChooser = new ItemStack("Prototype_Tool_Book_Mana", 1);
        hotbar.setItemStackForSlot((short) 0, arenaChooser);

        player.getInventory().markChanged();
        World world = player.getWorld();
        ISpawnProvider spawnProvider = Universe.get().getDefaultWorld().getWorldConfig().getSpawnProvider();

//        world.execute(() -> {
//            Transform spawnPoint = spawnProvider.getSpawnPoint(world, world.getWorldConfig().getUuid());
//            Vector3d position = spawnPoint.getPosition();
//            addTeleportTask(
//                    player.getReference(),
//                    world,
//                    position
//            );
//        });
    }


    public int getNumberOfArena() {
        return this.listOfArenas.size();
    }

    public LinkedList<ArenaStat> getArenaStats() {
        return new LinkedList<>(this.listOfArenas.values().stream().filter(HgArena::isActive)
                .map(arena -> new ArenaStat(
                        arena.getWorldName(),
                        arena.isActive(),
                        arena.isIngame(),
                        arena.getActivePlayerCount(),
                        arena.getArenaSize()
                )).sorted(Comparator.comparing(ArenaStat::arenaSize))
                .toList());
    }

    public void playerLeft(PlayerRef playerRef) {
        this.listOfArenas.forEach((_, value) -> value.playerLeft(playerRef));
    }

    public boolean isPlayerOnAnyArena(Player player) {
        return this.listOfArenas.values().stream().anyMatch(arena -> arena.isPlayerInArena(player));
    }



    public void leaveArena(Player player) {
        this.playerLeft(player.getPlayerRef());
    }

    public void playerDied(Player deadPlayer, World world, Player attackerPlayer) {
        this.getArena(world.getName()).playerDied(deadPlayer, attackerPlayer);
    }

    public void playerDied(Player deadPlayer, World world) {
        this.getArena(world.getName()).playerDied(deadPlayer, null);
    }
}
