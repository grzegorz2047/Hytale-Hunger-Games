package pl.grzegorz2047.hytale.hungergames.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class LobbyHud extends CustomUIHud {
    private final int fontSize;
    private String message;
    private String stats = ""; // Statystyki gracza (np. "Kills: 42")

    public LobbyHud(PlayerRef playerRef, int fontSize, String message) {
        super(playerRef);
        this.fontSize = fontSize;
        this.message = message == null ? "" : message;
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Huds/Lobby.ui");
        uiCommandBuilder.set("#LobbyWelcome.Text", this.message);
        uiCommandBuilder.set("#LobbyWelcome.Style.FontSize", this.fontSize);
        uiCommandBuilder.set("#LobbyStats.Text", this.stats);
    }

    public void setMessage(String message) {
        this.message = message;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LobbyWelcome.Text", this.message);
        builder.set("#LobbyStats.Text", this.stats);
        this.update(false, builder);
    }

    public void setKillStats(String stats) {
        this.stats = stats;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LobbyWelcome.Text", this.message);
        builder.set("#LobbyStats.Text", this.stats);
        this.update(false, builder);
    }
}
