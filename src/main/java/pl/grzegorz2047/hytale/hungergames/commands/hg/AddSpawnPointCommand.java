package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class AddSpawnPointCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final ArenaManager arenaManager;

    public AddSpawnPointCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "arena name to add spawn point to", ArgTypes.STRING);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("hungergames.admin")) {
            sender.sendMessage(
                    Message.raw("You don't have permission to use this command")
            );
            return null;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.raw("This command can only be used by a player"));
            return null;
        }

        String arenaName = this.arenaNameArg.get(context);

        if (!arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>Arena '" + arenaName + "' does not exist</color>"));
            return null;
        }

        if (arenaManager.isArenaEnabled(arenaName)) {
            sender.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>Cannot modify arena while it is active or in game</color>"));
            return null;
        }

        // Get player's current position from their entity store
        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null) {
            sender.sendMessage(Message.raw("Unable to get your position"));
            return null;
        }

        Vector3d currentPosition = player.getTransformComponent().getPosition();

        arenaManager.addSpawnPointToArena(arenaName, currentPosition);

        int count = arenaManager.getSpawnPointCount(arenaName);
        sender.sendMessage(MessageColorUtil.rawStyled("<color=#00FF00>Spawn point added to arena '" + arenaName + "' at position: "
            + String.format("%.2f, %.2f, %.2f", currentPosition.x, currentPosition.y, currentPosition.z)
            + " (Total spawn points: " + count + ")</color>"));

        return null;
    }
}
