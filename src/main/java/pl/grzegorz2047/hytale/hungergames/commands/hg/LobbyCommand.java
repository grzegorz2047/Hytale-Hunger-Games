package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.teleport.LobbyTeleporter;

import java.util.concurrent.CompletableFuture;

public class LobbyCommand extends AbstractPlayerCommand {
    private final ArenaManager arenaManager;

    public LobbyCommand(String commandName, String desc, ArenaManager arenaManager) {
        super(commandName, desc);
        this.arenaManager = arenaManager;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        if (!(commandContext.sender() instanceof Player player)) {
            return;
        }
        if( arenaManager.isPlayerOnAnyArena(player)) {
            arenaManager.leaveArena(player);
        }
        player.getWorld().execute(()-> LobbyTeleporter.teleportToLobby(player.getPlayerRef()));
    }
}
