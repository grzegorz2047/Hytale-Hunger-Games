package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.message.MessageColorUtil;

import java.util.concurrent.CompletableFuture;

public class ForceStartArenaCommand extends AbstractCommand {
    private final ArenaManager arenaManager;
    private final RequiredArg<String> arenaNameArg;

    public ForceStartArenaCommand(String forcestart, String startsArenaNow, ArenaManager arenaManager) {
        super(forcestart, startsArenaNow);
        this.arenaManager = arenaManager;
        arenaNameArg = this.withRequiredArg("arenaName", "force starts arena with that name", ArgTypes.STRING);
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        String arenaName = arenaNameArg.get(context);
        CommandSender sender = context.sender();
        if (!arenaManager.arenaExists(arenaName)) {
            String tpl = arenaManager.getConfig().getTranslation("hungergames.arena.notFound").replace("{arenaName}", arenaName);
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }
        if (!(sender instanceof Player player)) {
            return null;
        }
        if (!sender.hasPermission("hungergames.admin")) {
            String tpl = arenaManager.getConfig().getTranslation("noPermission");
            sender.sendMessage(MessageColorUtil.rawStyled(tpl));
            return null;
        }
        player.getWorld().execute(() -> arenaManager.forceStartArena(arenaName, player));
        return null;
    }
}
