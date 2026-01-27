package pl.grzegorz2047.hytale.hungergames;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import pl.grzegorz2047.hytale.hungergames.commands.hg.ForceStartArenaCommand;
import pl.grzegorz2047.hytale.hungergames.commands.hg.InitArenaCommand;
import pl.grzegorz2047.hytale.hungergames.commands.hg.arena.ArenaManager;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class HungerGamesCommand extends AbstractCommandCollection {

    private final String pluginName;
    private final String pluginVersion;

    public HungerGamesCommand(String pluginName, String pluginVersion, ArenaManager arenaManager) {
        super("hg", "Prints a test message from the " + pluginName + " plugin.");
        this.addAliases("hungergames", "survivalgames", "sg");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
//        this.addSubCommand(new TeleportAllCommand());
        this.addSubCommand(new InitArenaCommand("init", "creates arena", arenaManager));
        this.addSubCommand(new ForceStartArenaCommand("forcestart", "starts arena now", arenaManager));
    }
}