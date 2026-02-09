package pl.grzegorz2047.hytale.hungergames.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;

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

    public void initArenaScoreboard(String arenaName, Player player, PlayerRef playerRef, String numOfActivePlayers) {
        boolean isHudEnabled = this.config.isHudEnabled();
        String arenaNameHud = config.getTranslation("hungergames.hud.arena").replace("{arenaName}", arenaName);

        if (isHudEnabled) {
            MinigameHud hud = new MinigameHud(playerRef, 24, 300, true);
            player.getHudManager().setCustomHud(playerRef, hud);
            hud.setArenaName(arenaNameHud);
            hud.setNumOfActivePlayers(numOfActivePlayers);
            String time = config.getTranslation("hungergames.hud.time").replace("{time}", "00:00");
            hud.setTimeText(time);
        }
    }

    public void resetHud(PlayerRef playerRef) {
        Player player = getPlayer(playerRef);
        HudManager hudManager = player.getHudManager();
        hudManager.setCustomHud(playerRef, new CustomUIHud(playerRef) {
            @Override
            protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {

            }
        });
    }

    @NullableDecl
    private static Player getPlayer(PlayerRef playerRef) {
        Player player = null;
        Ref<EntityStore> reference = playerRef.getReference();
        if (reference != null) {
            player = findPlayerInPlayerComponentsBag(reference.getStore(), reference);
        }
        return player;
    }
}
