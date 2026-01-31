package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import java.util.concurrent.CompletableFuture;

public class JoinArenaCommand extends AbstractCommand {
    private final ArenaManager arenaManager;
    private final RequiredArg<String> arenaNameArg;

    public JoinArenaCommand(String forcestart, String startsArenaNow, ArenaManager arenaManager) {
        super(forcestart, startsArenaNow);
        this.arenaManager = arenaManager;
        arenaNameArg = this.withRequiredArg("arenaName", "force starts arena with that name", ArgTypes.STRING);
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            return null;
        }
        String arenaName = this.arenaNameArg.get(context);
        if (arenaManager.isArenaIngame(arenaName)) {
            player.sendMessage(Message.raw("Arena is already ingame"));
            return null;
        }
        arenaManager.joinArena(arenaName, player);
        return null;
    }
}
