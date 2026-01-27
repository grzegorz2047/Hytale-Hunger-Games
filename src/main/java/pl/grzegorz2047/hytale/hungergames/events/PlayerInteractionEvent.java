package pl.grzegorz2047.hytale.hungergames.events;

import com.hypixel.hytale.protocol.InteractionType;

public class PlayerInteractionEvent {
    private final InteractionType interactionType;
    private final String uuid;

    public PlayerInteractionEvent(InteractionType interactionType, String uuid) {
        this.interactionType = interactionType;
        this.uuid = uuid;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public String getUuid() {
        return uuid;
    }
}
