package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeFloat;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class InitArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final OptionalArg<Boolean> withWorldArg;
    private final ArenaManager arenaManager;

    public InitArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "creates arena", ArgTypes.STRING);
        this.withWorldArg = this.withOptionalArg("withWorld", "generate also world", ArgTypes.BOOLEAN);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        String worldName = this.arenaNameArg.get(context);

        int numberOfSpawnPoint = 8;
        int radius = 25;

        if (Universe.get().getWorld(worldName) != null) {
            sender.sendMessage(
                    Message.translation("server.universe.addWorld.alreadyExists")
                            .param("worldName", worldName)
            );
            return null;
        }
        if (this.withWorldArg.provided(context)) {
            if (this.withWorldArg.get(context) == true) {
                arenaManager.createArenaWithSpace(context, worldName, sender, numberOfSpawnPoint, radius);
            } else {
                arenaManager.createArena(worldName, numberOfSpawnPoint);

            }
        } else {
            arenaManager.createArenaWithSpace(context, worldName, sender, numberOfSpawnPoint, radius);
        }
        return null;
    }


}
