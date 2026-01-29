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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.system.WorldConfigSaveSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.grzegorz2047.hytale.hungergames.db.ArenaRepository;
import pl.grzegorz2047.hytale.hungergames.db.InMemoryRepository;
import pl.grzegorz2047.hytale.hungergames.db.SqliteArenaRepository;

import javax.annotation.Nonnull;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/*
...existing code...
*/
public class ArenaManager {
    HashMap<String, HgArena> listOfArenas = new HashMap<>();

    private final ArenaRepository repository;

    // Domyślny konstruktor: próbuje użyć SQLite, w razie błędu fallback do in-memory
    public ArenaManager() {
        ArenaRepository repo;
        try {
            File dataDir = new File("data");
            if (!dataDir.exists()) dataDir.mkdirs();
            repo = new SqliteArenaRepository("data/arenas.db");
            repo.initialize();
        } catch (Throwable t) {
            System.out.println("Warning: SQLite unavailable, using in-memory repository: " + t.getMessage());
            repo = new InMemoryRepository();
        }
        this.repository = repo;
        loadFromRepository();
    }

    // Konstruktor do wstrzyknięcia innego repozytorium (np. zewnętrzne DB lub mock w testach)
    public ArenaManager(ArenaRepository repository) {
        this.repository = repository;
        try {
            this.repository.initialize();
        } catch (Throwable ignored) {
        }
        loadFromRepository();
    }

    public boolean createArena(String worldName, List<Vector3d> spawnPoints, Vector3d lobbySpawnLocation) {
        HgArena arena = new HgArena(worldName, spawnPoints, lobbySpawnLocation);
        this.listOfArenas.put(worldName, arena);
        try {
            repository.save(arena);
        } catch (Throwable t) {
            System.out.println("Warning: failed to persist arena " + worldName + " : " + t.getMessage());
        }
        return true;
    }

    public boolean forceStartArena(String arenaName) {
        if (!this.arenaExists(arenaName)) {
            return false;
        }
        HgArena hgArena = getArena(arenaName);
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
        return !arena.isActive();
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


    public void addPlayer(String arenaName, Player player) {
        if (!this.arenaExists(arenaName)) {
            player.sendMessage(Message.raw("Arena doesnt exist"));
            return;
        }
        HgArena arena = this.getArena(arenaName);
        if (!arena.isActive()) {
            player.sendMessage(Message.raw("Arena is not active"));
            return;
        }
        arena.join(player);
    }



    public boolean isArenaIngame(String arenaName) {
        return false;
    }

    public void createArenaSpace(@NonNullDecl CommandContext context, String worldName, CommandSender sender, int numberOfSpawnPoint, int radius) {
        String generatorType = "Flat";
        BuilderCodec<? extends IWorldGenProvider> providerCodec =
                IWorldGenProvider.CODEC.getCodecFor(generatorType);

        if (providerCodec == null) {
            throw new IllegalArgumentException("Unknown generatorType '" + generatorType + "'");
        }

        CompletableFutureUtil._catch(
                Universe.get().addWorld(worldName, generatorType, "default")
                        .thenAccept(world -> {

                            sender.sendMessage(
                                    Message.translation("server.universe.addWorld.worldCreated")
                                            .param("worldName", worldName)
                                            .param("generator", generatorType)
                                            .param("storage", "default")
                            );

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
                            int maxHeight = 10;
                            int hillRadius = 10;
                            Vector3d lobbySpawnLocation = new Vector3d(0, maxHeight + 20, 0);
                            generationTasks.addAll(generateLobbyAreaTasks(world, lobbySpawnLocation, BlockType.fromString("Wood_Blackwood_Ornate"), 10));
                            generationTasks.addAll(generateHillTasks(context, world, 0, 0, hillRadius, maxHeight, blockType));
                            CompletableFuture
                                    .allOf(generationTasks.toArray(new CompletableFuture[0]))
                                    .thenRun(() -> {
                                        boolean isCreated = createArena(worldName, spawnPoints, lobbySpawnLocation);
                                        if (isCreated) {
                                            sender.sendMessage(
                                                    Message.raw("Arena wygenerowana!")
                                            );
                                        }
                                        CompletableFuture.runAsync(() -> saveWorld(world), world).thenRun(() -> {
                                            context.sendMessage(Message.translation("server.commands.world.save.savingDone").param("world", world.getName()));
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
        Store<EntityStore> store = playerRef.getStore();
        ComponentType<EntityStore, Teleport> componentType = Teleport.getComponentType();
        Teleport teleport = Teleport.createForPlayer(world, spawnCoordPos, Vector3f.NaN);
        store.addComponent(playerRef, componentType, teleport);
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

                CompletableFuture<Void> task = world.getChunkAsync(chunkIndex).thenAccept(chunk -> {
                    for (int y = 0; y <= height; y++) {
                        chunk.setBlock(worldX, y, worldZ, blockType);
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
}
