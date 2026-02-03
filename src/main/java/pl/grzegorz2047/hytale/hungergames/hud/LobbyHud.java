package pl.grzegorz2047.hytale.hungergames.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class LobbyHud extends CustomUIHud {
    private final int fontSize;
    private String message;

    public LobbyHud(PlayerRef playerRef, int fontSize, String message) {
        super(playerRef);
        this.fontSize = fontSize;
        this.message = message == null ? "Welcome on the server" : message;
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Huds/Lobby.ui");
        uiCommandBuilder.set("#LobbyWelcome.Text", this.message);
        uiCommandBuilder.set("#LobbyWelcome.Style.FontSize", this.fontSize);
    }

    public void setMessage(String message) {
        if (message == null || message.equals(this.message)) {
            return;
        }
        this.message = message;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LobbyWelcome.Text", this.message);
        this.update(false, builder);
    }
}
