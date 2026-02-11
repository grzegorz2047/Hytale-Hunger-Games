package pl.grzegorz2047.hytale.hungergames.commands.hg;

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
    private final OptionalArg<Integer> centerRadiusArg;
    private final OptionalArg<Integer> barrierRadiusArg;
    private final OptionalArg<Integer> barrierHeightArg;
    private final OptionalArg<Boolean> generateHillArg;
//    private final OptionalArg<Boolean> withWorldArg;
    private final ArenaManager arenaManager;

    public GenerateArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.arenaNameArg = this.withRequiredArg("arenaName", "creates arena", ArgTypes.STRING);
        this.numberOfSpawnPointsArg = this.withOptionalArg("numberOfSpawnPoints", "number of spawn points", ArgTypes.INTEGER);
        this.centerRadiusArg = this.withOptionalArg("centerRadius", "radius for spawn points", ArgTypes.INTEGER);
        this.barrierRadiusArg = this.withOptionalArg("barrierRadius", "radius for barrier walls", ArgTypes.INTEGER);
        this.barrierHeightArg = this.withOptionalArg("barrierHeight", "wall height for barrier walls", ArgTypes.INTEGER);
        this.generateHillArg = this.withOptionalArg("generateHill", "generates middle hill walls", ArgTypes.BOOLEAN);
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
        String worldName = this.arenaNameArg.get(context);

        Integer numberOfSpawnPointArg = this.numberOfSpawnPointsArg.get(context);
        int numberOfSpawnPoint = numberOfSpawnPointArg != null ? numberOfSpawnPointArg : 8;

        Integer radiusArg = this.centerRadiusArg.get(context);
        int radius = radiusArg != null ? radiusArg : 25;

        if (Universe.get().getWorld(worldName) != null) {
            String tpl = arenaManager.getConfig().getTranslation("server.universe.addWorld.alreadyExists");
            String formatted = tpl == null ? "" : tpl.replace("{worldName}", worldName);
            sender.sendMessage(MessageColorUtil.rawStyled(formatted));
            return null;
        }
        Integer barrierRadiusArg = this.barrierRadiusArg.get(context);
        int barrierRadius = barrierRadiusArg != null ? barrierRadiusArg : 100;
        Integer barrierHeightArg = this.barrierHeightArg.get(context);
        int barrierHeight = barrierHeightArg != null ? barrierHeightArg : 30;

        Boolean generateHillArg = this.generateHillArg.get(context);
        boolean generateHillParam = generateHillArg != null ? generateHillArg : true;
        arenaManager.createArenaWithSpace(context, worldName, sender, numberOfSpawnPoint, radius, barrierRadius, barrierHeight, generateHillParam);
        return null;
    }


}
