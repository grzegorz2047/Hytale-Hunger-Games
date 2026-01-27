package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.commands.hg.arena.ArenaManager;

import java.util.concurrent.CompletableFuture;

public class ForceStartArenaCommand extends AbstractCommand {
    private final ArenaManager arenaManager;
    private final RequiredArg<String> arenaName;

    public ForceStartArenaCommand(String forcestart, String startsArenaNow, ArenaManager arenaManager) {
        super(forcestart,startsArenaNow);
        this.arenaManager = arenaManager;
        arenaName = this.withRequiredArg("arenaName", "force starts arena with that name", ArgTypes.STRING);
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        arenaManager.forceStartArena(this.arenaName.get(context));
        return null;
    }
}
