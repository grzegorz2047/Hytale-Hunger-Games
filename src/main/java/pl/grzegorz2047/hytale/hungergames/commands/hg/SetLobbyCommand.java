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
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class SetLobbyCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final ArenaManager arenaManager;
    private final MainConfig mainConfig;

    public SetLobbyCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        this(name, description, arenaManager, null);
    }

    public SetLobbyCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager, MainConfig mainConfig) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "arena name to set lobby for", ArgTypes.STRING);
        this.arenaManager = arenaManager;
        this.mainConfig = mainConfig;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("hungergames.admin")) {
            String tpl = arenaManager.getConfig().getTranslation("noPermission");
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }

        if (!(sender instanceof Player player)) {
            String tpl = mainConfig != null ? mainConfig.getTranslation("hungergames.command.onlyForPlayer") : "This command can only be used by a player";
            sender.sendMessage(Message.raw(tpl));
            return null;
        }

        String arenaName = this.arenaNameArg.get(context);

        if (!arenaManager.arenaExists(arenaName)) {
            String tpl = mainConfig != null ? mainConfig.getTranslation("hungergames.command.arenaDoesNotExist") : "Arena '" + arenaName + "' does not exist";
            sender.sendMessage(MessageColorUtil.rawStyled(tpl.replace("{arenaName}", arenaName)));
            return null;
        }

        if (arenaManager.isArenaEnabled(arenaName)) {
            String tpl = mainConfig != null ? mainConfig.getTranslation("hungergames.command.cannotModifyActiveArena") : "Cannot modify arena while it is active or in game";
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }

        // Get player's current position from their entity store
        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null) {
            sender.sendMessage(Message.raw("Unable to get your position"));
            return null;
        }

        Vector3d currentPosition = player.getTransformComponent().getPosition();

        arenaManager.setLobbySpawnLocation(arenaName, currentPosition);

        sender.sendMessage(MessageColorUtil.rawStyled("<color=#00FF00>Lobby spawn point set for arena '" + arenaName + "' at position: "
            + String.format("%.2f, %.2f, %.2f", currentPosition.x, currentPosition.y, currentPosition.z) + "</color>"));

        return null;
    }
}
