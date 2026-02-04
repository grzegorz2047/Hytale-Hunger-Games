package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.teleport.LobbyTeleporter;

import java.util.concurrent.CompletableFuture;

public class LobbyCommand extends AbstractCommand {
    private final ArenaManager arenaManager;

    public LobbyCommand(String commandName, String desc, ArenaManager arenaManager) {
        super(commandName, desc);
        this.setPermissionGroup(GameMode.Adventure);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            return null;
        }
        if( arenaManager.isPlayerOnAnyArena(player)) {
            arenaManager.leaveArena(player);
        }
        player.getWorld().execute(()-> LobbyTeleporter.teleportToLobby(player.getPlayerRef()));
        return null;
    }
}
