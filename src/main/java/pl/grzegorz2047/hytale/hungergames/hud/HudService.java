package pl.grzegorz2047.hytale.hungergames.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

public class HudService {

    private final MainConfig config;

    public HudService(MainConfig config) {
        this.config = config;
    }

    public void initLobbyHud(HudManager hudManager, PlayerRef playerRef, String kills) {
        String tpl = config.getTranslation("hungergames.hud.lobby.welcome");
        LobbyHud lobbyHud = new LobbyHud(playerRef, 24, tpl);
        hudManager.setCustomHud(playerRef, lobbyHud);
        lobbyHud.setKillStats(kills);

    }
    public void initArenaScoreboard(String arenaName, Player player, PlayerRef playerRef) {
        boolean isHudEnabled = this.config.isHudEnabled();

        if (isHudEnabled) {
            MinigameHud hud = new MinigameHud(playerRef, 24, 300, true);
            player.getHudManager().setCustomHud(playerRef, hud);
            hud.setArenaName(arenaName);
            hud.setNumOfActivePlayers("");
        }
    }
}
