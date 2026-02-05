package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CreateArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final OptionalArg<Integer> numberOfSpawnPointsArg;
    private final OptionalArg<Integer> radiusArg;
    //    private final OptionalArg<Boolean> withWorldArg;
    private final ArenaManager arenaManager;

    public CreateArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "creates arena", ArgTypes.STRING);
        this.numberOfSpawnPointsArg = this.withOptionalArg("numberOfSpawnPoints", "number of spawn points", ArgTypes.INTEGER);
        this.radiusArg = this.withOptionalArg("radius", "radius for spawn points", ArgTypes.INTEGER);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("hungergames.admin")) {
            sender.sendMessage(
                    Message.raw("You dont have permission to use this command")
            );
            return null;
        }
        String worldName = this.arenaNameArg.get(context);
        World world = Universe.get().getWorld(worldName);
        if (arenaManager.arenaExists(worldName)) {
            String tpl = arenaManager.getConfig().getTranslation("server.universe.addWorld.alreadyExists");
            String formatted = tpl == null ? "" : tpl.replace("{worldName}", worldName);
            sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            return null;
        }
        if (!(sender instanceof Player player)) {
            return null;
        }
        if (world == null) {
            String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.worldNotFound");
            String formatted = tpl == null ? "" : tpl.replace("{worldName}", worldName);
            sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            return null;
        }
        World arenaWorld = Objects.requireNonNull(world);
        World playerWorld = player.getWorld();
        playerWorld.execute(() -> {
            ISpawnProvider spawnProvider = arenaWorld.getWorldConfig().getSpawnProvider();
            Ref<EntityStore> reference = player.getReference();
            if(reference == null) {
                sender.sendMessage(Message.raw("Unable to get your position"));
                return;
            }
            assert spawnProvider != null;
            Transform spawnPoint = spawnProvider.getSpawnPoint(reference, reference.getStore());
            if (spawnPoint == null) return;
            Vector3d lobbySpawnPoint = spawnPoint.getPosition();
            boolean arenaCreated = arenaManager.createArena(worldName, new ArrayList<>(), lobbySpawnPoint);
            if (arenaCreated) {
                String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.created");
                String formatted = tpl == null ? "" : tpl.replace("{arenaName}", worldName);
                sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            } else {
                String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.creationFailed");
                String formatted = tpl == null ? "" : tpl.replace("{arenaName}", worldName);
                sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            }
        });
        return null;
    }


}
