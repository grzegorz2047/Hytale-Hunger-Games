package pl.grzegorz2047.hytale.hungergames.hud;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.grzegorz2047.hytale.hungergames.arena.HgPlayer;
import pl.grzegorz2047.hytale.hungergames.config.MainConfig;

import static pl.grzegorz2047.hytale.hungergames.util.PlayerComponentUtils.findPlayerInPlayerComponentsBag;

public class HudService {

    private final MainConfig config;
    private final PluginBase multipleHud;

    public HudService(MainConfig config) {
        this.config = config;
        multipleHud = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));
    }

    public void initLobbyHud(HudManager hudManager, Player player, PlayerRef playerRef, String kills) {
        String tpl = config.getTranslation("hungergames.hud.title");
        LobbyHud lobbyHud = new LobbyHud(playerRef, 24, tpl);
        if (multipleHud == null) {
//            this.playersHud.put(playerRef, hud);
            hudManager.setCustomHud(playerRef, lobbyHud);
        } else {
            MultipleHUD.getInstance().setCustomHud(player, playerRef, "HungerGames2047_lobby", lobbyHud);
//            this.playersHud.put(playerRef, hud);
        }
        lobbyHud.setKillStats(kills);
    }

    public void initArenaScoreboard(String arenaName, Player player, PlayerRef playerRef, String numOfActivePlayers, HgPlayer hgPlayer) {
        boolean isHudEnabled = this.config.isHudEnabled();
        String arenaNameHud = config.getTranslation("hungergames.hud.arena").replace("{arenaName}", arenaName);

        if (isHudEnabled) {
            MinigameHud hud = new MinigameHud(playerRef, 24, 300, true);
            if (multipleHud == null) {
//            this.playersHud.put(playerRef, hud);
                player.getHudManager().setCustomHud(playerRef, hud);
            } else {
                MultipleHUD.getInstance().setCustomHud(player, playerRef, "HungerGames2047_arena_scoreboard", hud);
//            this.playersHud.put(playerRef, hud);
            }
            hgPlayer.setCustomHud(hud);
            hud.setArenaName(arenaNameHud);
            hud.setNumOfActivePlayers(numOfActivePlayers);
            String time = config.getTranslation("hungergames.hud.time").replace("{time}", "00:00");
            hud.setTimeText(time);
            String title = config.getTranslation("hungergames.hud.title");
            hud.setMessage(title);
        }
    }

    public static void resetHud(PlayerRef playerRef, String hudId) {
        Player player = getPlayer(playerRef);
        HudManager hudManager = player.getHudManager();
        PluginBase multipleHud = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));
        if (multipleHud == null) {
//            this.playersHud.put(playerRef, hud);
            hudManager.setCustomHud(playerRef, new CustomUIHud(playerRef) {
                @Override
                protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {

                }
            });
        } else {
            MultipleHUD.getInstance().hideCustomHud(player, hudId);
//            this.playersHud.put(playerRef, hud);
        }

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
