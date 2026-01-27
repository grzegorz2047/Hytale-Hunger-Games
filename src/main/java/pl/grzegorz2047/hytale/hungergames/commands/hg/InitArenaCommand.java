package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.commands.hg.arena.ArenaManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class InitArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> worldNameArg;
    private final ArenaManager arenaManager;

    public InitArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.worldNameArg = this.withRequiredArg("worldName", "creates arena", ArgTypes.STRING);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        String worldName = this.worldNameArg.get(context);

        int numberOfSpawnPoint = 8;
        int radius = 25;

        if (Universe.get().getWorld(worldName) != null) {
            sender.sendMessage(
                    Message.translation("server.universe.addWorld.alreadyExists")
                            .param("worldName", worldName)
            );
            return null;
        }

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
                            List<Vector3i> spawnPoints = new ArrayList<>();
                            for (int i = 0; i < numberOfSpawnPoint; i++) {
                                double angle = 2 * Math.PI * i / numberOfSpawnPoint;

                                int x = (int) Math.round(Math.cos(angle) * radius);
                                int y = 0;
                                int z = (int) Math.round(Math.sin(angle) * radius);
                                Vector3i spawnPoint = new Vector3i(x, y + 1, z);
                                spawnPoints.add(spawnPoint);
                                long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);


                                CompletableFuture<Void> task =
                                        generateSpawnPoints(context, world, chunkIndex, x, z, blockType, y);

                                generationTasks.add(task);
                            }
                            int maxHeight = 10;
                            int hillRadius = 10;
                            Vector3i lobbySpawnLocation = new Vector3i(0, maxHeight + 1, 0);
                            generationTasks.addAll(generateHillTasks(context, world, 0, 0, hillRadius, maxHeight, blockType));
                            CompletableFuture
                                    .allOf(generationTasks.toArray(new CompletableFuture[0]))
                                    .thenRun(() -> {
                                        boolean isCreated = arenaManager.createArena(worldName, spawnPoints, lobbySpawnLocation);
                                        if (isCreated) {
                                            sender.sendMessage(
                                                    Message.raw("Arena wygenerowana!")
                                            );
                                        }


                                        if (!(sender instanceof Player player)) {
                                            return;
                                        }

                                        player.getWorld().execute(() -> {
                                            addTeleportTask(
                                                    player.getReference(),
                                                    world,
                                                    new Vector3d(0, 5, 0)
                                            );
                                        });
                                    });
                        })
        ).exceptionally(t -> {
            LOGGER.at(Level.SEVERE).withCause(t)
                    .log("Failed to add world '%s'", worldName);
            return null;
        });

        return null;
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
}
