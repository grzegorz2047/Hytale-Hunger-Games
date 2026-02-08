package pl.grzegorz2047.hytale.hungergames.commands.hg;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.HungerGames;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.hud.ArenaListPage;

import java.util.concurrent.CompletableFuture;

public class ListArenaCommand extends AbstractPlayerCommand {
    private final ArenaManager arenaManager;
    private final ArenaListPage arenaListPage;

    public ListArenaCommand(String name, String description, ArenaManager arenaManager, ArenaListPage arenaListPage) {
        super(name, description);
        this.arenaManager = arenaManager;
        this.arenaListPage = arenaListPage;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        if (!(commandContext.sender() instanceof Player player)) {
            return;
        }
        PageBuilder pageBuilder = arenaListPage.prepareArenaListPage(playerRef, player, this.arenaManager.getArenaStats());
        pageBuilder
                .open(playerRef.getReference().getStore());
    }
}
