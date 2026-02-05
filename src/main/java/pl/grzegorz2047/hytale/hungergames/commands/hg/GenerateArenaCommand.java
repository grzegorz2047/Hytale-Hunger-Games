package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class GenerateArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> arenaNameArg;
    private final OptionalArg<Integer> numberOfSpawnPointsArg;
    private final OptionalArg<Integer> radiusArg;
//    private final OptionalArg<Boolean> withWorldArg;
    private final ArenaManager arenaManager;

    public GenerateArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "creates arena", ArgTypes.STRING);
        this.numberOfSpawnPointsArg = this.withOptionalArg("numberOfSpawnPoints", "number of spawn points", ArgTypes.INTEGER);
        this.radiusArg = this.withOptionalArg("radius", "radius for spawn points", ArgTypes.INTEGER);
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
        String worldName = this.arenaNameArg.get(context);

        Integer numberOfSpawnPointArg = this.numberOfSpawnPointsArg.get(context);
        int numberOfSpawnPoint = numberOfSpawnPointArg != null ? numberOfSpawnPointArg : 8;

        Integer radiusArg = this.radiusArg.get(context);
        int radius = radiusArg != null ? radiusArg : 25;

        if (Universe.get().getWorld(worldName) != null) {
            String tpl = arenaManager.getConfig().getTranslation("server.universe.addWorld.alreadyExists");
            String formatted = tpl == null ? "" : tpl.replace("{worldName}", worldName);
            sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            return null;
        }
        arenaManager.createArenaWithSpace(context, worldName, sender, numberOfSpawnPoint, radius);
        return null;
    }


}
