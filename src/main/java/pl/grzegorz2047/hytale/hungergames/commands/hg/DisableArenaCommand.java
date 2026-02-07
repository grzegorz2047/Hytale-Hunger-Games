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
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class DisableArenaCommand extends AbstractCommand {

    @Nonnull
    private final RequiredArg<String> worldNameArg;
    private final ArenaManager arenaManager;

    public DisableArenaCommand(@NullableDecl String name, @NullableDecl String description, ArenaManager arenaManager) {
        super(name, description);
        this.worldNameArg = this.withRequiredArg("arenaName", "disables arena", ArgTypes.STRING);
        this.arenaManager = arenaManager;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        CommandSender sender = context.sender();
        if(!sender.hasPermission("hungergames.admin")) {
            String tpl = arenaManager.getConfig().getTranslation("noPermission");
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }
        String worldName = this.worldNameArg.get(context);

        World world = Universe.get().getWorld(worldName);
        if (world == null || !arenaManager.arenaExists(worldName)) {
            String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.notFound").replace("{arenaName}", worldName);
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }
        WorldConfig worldConfig = world.getWorldConfig();
        worldConfig.setCanSaveChunks(true);
        worldConfig.markChanged();
        this.arenaManager.setEnableArena(worldName, false);
        String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.disabled").replace("{arenaName}", worldName);
        sender.sendMessage(MessageColorUtil.rawStyled(tpl));
        return null;
    }


}
