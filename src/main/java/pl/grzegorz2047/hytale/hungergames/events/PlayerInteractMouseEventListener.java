package pl.grzegorz2047.hytale.hungergames.events;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import pl.grzegorz2047.hytale.hungergames.HungerGames;

public class PlayerInteractMouseEventListener {
    private final HungerGames hungerGames;

    public PlayerInteractMouseEventListener(HungerGames hungerGames) {
        this.hungerGames = hungerGames;
    }

    public void register(EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerMouseButtonEvent.class, PlayerInteractMouseEventListener::onInteract);
    }

    private static void onInteract(PlayerMouseButtonEvent event) {
        Player player = event.getPlayer();
        MouseButtonEvent mouseEvent = event.getMouseButton();
        // Check which button was pressed
        if (mouseEvent.mouseButtonType == MouseButtonType.Left) {
            // Left click pressed
//            handleLeftClick(player, event);
        } /*else if (mouseEvent.isRightButton() && mouseEvent.isPressed()) {
            // Right click pressed
            handleRightClick(player, event);
        }*/

//        // Check if targeting a block
//        Vector3i targetBlock = event.getTargetBlock();
//        if (targetBlock != null) {
//            // Block interaction
//            if (isProtectedArea(targetBlock)) {
//                event.setCancelled(true);
//                player.sendMessage("You cannot interact with blocks here!");
//                return;
//            }
//        }
//
//        // Check if targeting an entity
//        Entity targetEntity = event.getTargetEntity();
//        if (targetEntity != null) {
//            // Entity interaction
//            handleEntityClick(player, targetEntity, mouseEvent);
//        }
//        Item itemInHand = mouseEvent.getItemInHand();
//        if (itemInHand.getId().equals("Prototype_Tool_Book_Mana")) {
//            mouseEvent.getPlayer().sendMessage(Message.raw("YO!!!!"));
//        } else {
//            mouseEvent.getPlayer().sendMessage(Message.raw("NOOO!!!!"));
//
//            return;
//        }
    }
}
