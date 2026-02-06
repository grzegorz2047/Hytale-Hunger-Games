package pl.grzegorz2047.hytale.hungergames.arena;

import java.util.UUID;
import java.util.Objects;

/**
 * Reprezentacja gracza w arenie Hunger Games.
 * Przechowuje stan gracza podczas gry, włączając liczbę dokonanych zabójstw.
 */
public class HgPlayer {
    private final UUID uuid;
    private final String playerName;
    private int kills; // Zabójstwa w bieżącej grze
    private int globalKills; // Suma wszystkich zabójstw ze wszystkich gier (z bazy danych)

    public HgPlayer(UUID uuid, String playerName) {
        this(uuid, playerName, 0, 0);
    }

    public HgPlayer(UUID uuid, String playerName, int kills) {
        this(uuid, playerName, kills, 0);
    }

    public HgPlayer(UUID uuid, String playerName, int kills, int globalKills) {
        this.uuid = Objects.requireNonNull(uuid, "UUID nie może być null");
        this.playerName = Objects.requireNonNull(playerName, "Nazwa gracza nie może być null");
        this.kills = Math.max(0, kills);
        this.globalKills = Math.max(0, globalKills);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getKills() {
        return kills;
    }

    public int getGlobalKills() {
        return globalKills;
    }

    public void addKill() {
        this.kills++;
    }

    public void addGlobalKill() {
        this.globalKills++;
    }

    public void resetKills() {
        this.kills = 0;
    }

    public void setGlobalKills(int globalKills) {
        this.globalKills = Math.max(0, globalKills);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HgPlayer hgPlayer = (HgPlayer) o;
        return Objects.equals(uuid, hgPlayer.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "HgPlayer{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", kills=" + kills +
                ", globalKills=" + globalKills +
                '}';
    }
}
