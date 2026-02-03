package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import java.util.concurrent.CompletableFuture;

public class LeaveArenaCommand extends AbstractCommand {
    private final ArenaManager arenaManager;

    public LeaveArenaCommand(String commandName, String commandDescription, ArenaManager arenaManager) {
        super(commandName, commandDescription);
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP

        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            return null;
        }
        if (!arenaManager.isPlayerOnAnyArena(player)) {
            player.sendMessage(Message.raw("There is no arena you are playing on"));
            return null;
        }
        arenaManager.leaveArena(player);
        return null;
    }
}
