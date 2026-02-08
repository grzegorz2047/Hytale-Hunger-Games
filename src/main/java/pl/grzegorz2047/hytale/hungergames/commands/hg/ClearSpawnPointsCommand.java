package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ClearSpawnPointsCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final ArenaManager arenaManager;

    public ClearSpawnPointsCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "arena name to clear spawn points from", ArgTypes.STRING);
        this.arenaManager = arenaManager;
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

        String arenaName = this.arenaNameArg.get(context);

        if (!arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>Arena '" + arenaName + "' does not exist</color>"));
            return null;
        }

        if (arenaManager.isArenaEnabled(arenaName)) {
            sender.sendMessage(MessageColorUtil.rawStyled("<color=#FF0000>Cannot modify arena while it is active or in game</color>"));
            return null;
        }

        arenaManager.clearSpawnPointsInArena(arenaName);
        sender.sendMessage(MessageColorUtil.rawStyled("<color=#00FF00>All spawn points cleared from arena '" + arenaName + "'</color>"));

        return null;
    }
}
