package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class DisableArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> worldNameArg;
    private final ArenaManager arenaManager;

    public DisableArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.worldNameArg = this.withRequiredArg("arenaname", "disables arena", ArgTypes.STRING);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        if(!sender.hasPermission("hungergames.admin")) {
            sender.sendMessage(
                    Message.raw("You dont have permission to use this command")
            );
            return null;
        }
        String worldName = this.worldNameArg.get(context);

        World world = Universe.get().getWorld(worldName);
        if (world == null || !arenaManager.arenaExists(worldName)) {
            sender.sendMessage(
                    Message.raw("Arena doesnt exist")
            );
            return null;
        }
        WorldConfig worldConfig = world.getWorldConfig();
        worldConfig.setCanSaveChunks(true);
        worldConfig.markChanged();
        this.arenaManager.setEnableArena(worldName, false);
        sender.sendMessage(Message.raw("Arena disabled"));
        return null;
    }


}
