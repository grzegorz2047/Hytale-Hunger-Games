package pl.grzegorz2047.hytale.hungergames.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class MinigameHud extends CustomUIHud {
    private static final int CONTAINER_RIGHT = 55;
    private final int widthClockArea;
    private final int CONTAINER_HEIGHT = 300;
    private final int CONTAINER_TOP = 300;
    private final boolean backgroundColorEnabled;
    private volatile String time = "Time: 00:00";
    private final int fontSize;
    private String arenaName = "Arena name: None";
    private String numOfActivePlayers = "Players left: 0";
    private String killFeedText = "Kills: 0";
    private String playerKills = "Your Kills: 0";

    public MinigameHud(PlayerRef playerRef, int fontSize, int widthClockArea, boolean backgroundColorEnabled) {
        super(playerRef);
        this.fontSize = fontSize;
        this.widthClockArea = widthClockArea;
        this.backgroundColorEnabled = backgroundColorEnabled;
    }


    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Huds/Scoreboard.ui");
        String groupId;
        if (backgroundColorEnabled) {
            groupId = "#SimpleHudObjWithOpacity";
            uiCommandBuilder.remove("#SimpleHudObj");
        } else {
            groupId = "#SimpleHudObj";
            uiCommandBuilder.remove("#SimpleHudObjWithOpacity");
        }
        uiCommandBuilder.set("#ArenaTimer.Style.FontSize", this.fontSize);
        uiCommandBuilder.set("#ArenaTimer.Text", this.time);
        uiCommandBuilder.set("#ArenaName.Text", this.arenaName);
        uiCommandBuilder.set("#ArenaNumberOfPlayers.Text" , this.numOfActivePlayers);
        uiCommandBuilder.set("#PlayerKills.Text", this.playerKills);
        Anchor data = new Anchor();
        data.setWidth(Value.of(this.widthClockArea));
        data.setHeight(Value.of(CONTAINER_HEIGHT));
        data.setTop(Value.of(CONTAINER_TOP));
        data.setRight(Value.of(CONTAINER_RIGHT));
        uiCommandBuilder.setObject(
                groupId + ".Anchor",
                data
        );
    }

    public void setTimeText(String timeText) {
        if (timeText == null || timeText.equals(this.time)) {
            return;
        }
        this.time = timeText;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ArenaName.Text", this.arenaName);
        builder.set("#ArenaNumberOfPlayers.Text", this.numOfActivePlayers);
        builder.set("#ArenaTimer.Text", this.time);
        builder.set("#PlayerKills.Text", this.playerKills);
        this.update(false, builder);
    }


    private static final int[][] COLORS = {
            {74, 105, 189},   // noc 00:00
            {255, 217, 102},  // świt 06:00
            {255, 255, 255},  // dzień 12:00
            {255, 159, 67},   // zachód 18:00
            {74, 105, 189}    // noc 24:00
    };

    private static final int[] HOURS = {0, 6, 12, 18, 24};

    public static String getTextColorByTime(double hour) {

        for (int i = 0; i < HOURS.length - 1; i++) {
            if (hour >= HOURS[i] && hour <= HOURS[i + 1]) {
                double progress =
                        (hour - HOURS[i]) / (HOURS[i + 1] - HOURS[i]);

                int r = lerp(COLORS[i][0], COLORS[i + 1][0], progress);
                int g = lerp(COLORS[i][1], COLORS[i + 1][1], progress);
                int b = lerp(COLORS[i][2], COLORS[i + 1][2], progress);

                return String.format("#%02X%02X%02X", r, g, b);
            }
        }
        return "#FFFFFF"; // fallback
    }

    private static int lerp(int start, int end, double t) {
        return (int) Math.round(start + (end - start) * t);
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ArenaName.Text", this.arenaName);
        builder.set("#ArenaNumberOfPlayers.Text", this.numOfActivePlayers);
        builder.set("#ArenaTimer.Text", this.time);
        builder.set("#PlayerKills.Text", this.playerKills);
        this.update(false, builder);
    }

    public void setNumOfActivePlayers(String numOfActivePlayers) {
        this.numOfActivePlayers = numOfActivePlayers;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ArenaName.Text", this.arenaName);
        builder.set("#ArenaNumberOfPlayers.Text", this.numOfActivePlayers);
        builder.set("#ArenaTimer.Text", this.time);
        builder.set("#PlayerKills.Text", this.playerKills);
        this.update(false, builder);
    }

    public void setPlayerKills(String playerKills) {
        if (playerKills == null || playerKills.equals(this.playerKills)) {
            return;
        }
        this.playerKills = playerKills;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ArenaName.Text", this.arenaName);
        builder.set("#ArenaNumberOfPlayers.Text", this.numOfActivePlayers);
        builder.set("#ArenaTimer.Text", this.time);
        builder.set("#PlayerKills.Text", this.playerKills);
        this.update(false, builder);
    }
}
