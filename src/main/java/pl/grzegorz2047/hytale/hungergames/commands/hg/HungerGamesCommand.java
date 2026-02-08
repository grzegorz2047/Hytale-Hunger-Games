package pl.grzegorz2047.hytale.hungergames.commands.hg;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import pl.grzegorz2047.hytale.hungergames.arena.ArenaManager;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class HungerGamesCommand extends AbstractCommandCollection {

    private final String pluginName;
    private final String pluginVersion;
    private final MainConfig mainConfig;

    public HungerGamesCommand(String pluginName, String pluginVersion, ArenaManager arenaManager) {
        this(pluginName, pluginVersion, arenaManager, null);
        this.setPermissionGroups(GameMode.Adventure.toString(), GameMode.Creative.toString());
    }

    public HungerGamesCommand(String pluginName, String pluginVersion, ArenaManager arenaManager, MainConfig mainConfig) {
        super("hg", "Main command for Hunger Games plugin");
        this.setPermissionGroups(GameMode.Adventure.toString(), GameMode.Creative.toString());

        this.addAliases("hungergames", "survivalgames", "sg");
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.mainConfig = mainConfig;
//        this.addSubCommand(new TeleportAllCommand());
        this.addSubCommand(new CreateArenaCommand("create", "creates raw arena to configure", arenaManager));
        this.addSubCommand(new DisableArenaCommand("disable", "disables arena", arenaManager));
        this.addSubCommand(new EnableArenaCommand("enable", "enables arena", arenaManager));
        this.addSubCommand(new GenerateArenaCommand("generate", "creates arena with premade world", arenaManager));
        this.addSubCommand(new ForceStartArenaCommand("forcestart", "starts arena now", arenaManager));
        this.addSubCommand(new LeaveArenaCommand("leave", "leaves arena", arenaManager));
        this.addSubCommand(new JoinArenaCommand("join", "enters arena", arenaManager));
        this.addSubCommand(new LobbyCommand("lobby", "teleports to main world", arenaManager));
        this.addSubCommand(new SetLobbyCommand("setlobby", "sets the lobby spawn point for an arena", arenaManager, mainConfig));
        this.addSubCommand(new AddSpawnPointCommand("addspawn", "adds a spawn point at your current position", arenaManager));
        this.addSubCommand(new ClearSpawnPointsCommand("clearspawns", "clears all spawn points from an arena", arenaManager));
    }
}