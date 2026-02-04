package pl.grzegorz2047.hytale.hungergames.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;

public class MainConfig {

    private String[] itemsToFillChest = new String[]{"Armor_Iron_Chest:1", "Weapon_Sword_Frost:1", "Weapon_Shield_Cobalt:1", "Food_Bread:2"};
    private HashMap<String, String> messages = new HashMap<>();
    private int minimumPlayersToStartArena = 2;
    private final String[] messagesConfigArray = new String[]
            {
                    "noPermission:You do not have permission to perform this action.",
                    "hungergames.arena.notFound:Arena doesn't exist.",
                    "hungergames.arena.notActive:Arena is not active.",
                    "hungergames.arena.alreadyOnArena:You are already on an arena. Leave this first.",
                    "hungergames.arena.generated:Arena generated!",
                    "hungergames.arena.joined:You have joined the arena: {worldName}",
                    "hungergames.arena.left:You have left the arena: {worldName}",
                    "hungergames.arena.full:Arena is full",
                    "hungergames.arena.playerLeftBroadcast:Player left the arena. Current players: {count}",
                    "hungergames.arena.countingCancelled:Countdown cancelled: not enough players",
                    "hungergames.arena.startIn:Start in {seconds}s",
                    "hungergames.arena.arenaStarted:Arena started!",
                    "hungergames.arena.deathmatchStart:Deathmatch start!",
                    "hungergames.arena.deathmatchIn:Deathmatch in {seconds}s",
                    "hungergames.arena.gameEndsIn:Game ends in {seconds}s",
                    "hungergames.arena.gameEndedReturn:Game ended! Returning to lobby.",
                    "hungergames.arena.countingStarted:Counting started: game starts in {seconds}s",
                    "hungergames.block.cannotBreak:<color=#FF0000>You cannot break it</color>",
                    "hungergames.block.id:{id}",
                    "hungergames.item.cannotDrop:<color=#FF0000>You cannot drop it</color>",
                    "hungergames.item.id:{id}",
                    "server.universe.addWorld.worldCreated:World {worldName} created with generator {generator} and storage {storage}.",
                    "server.commands.world.save.savingDone:World {world} saved.",
                    "server.universe.addWorld.alreadyExists:World {worldName} already exists.",
                    "hungergames.arena.enabled:Arena enabled",
                    "hungergames.arena.disabled:Arena disabled",
                    "hungergames.arena.notPlaying:There is no arena you are playing on",
                    "hungergames.arena.alreadyIngame:Arena is already ingame",
                    "hungergames.death.playerBroadcast:Death player: {player}",
                    "hungergames.hud.lobby.welcome:Welcome {username} to the server",
                    "hungergames.hud.time:Time",
                    "hungergames.hud.playersLeft:Players left",
                    "hungergames.hud.arena:Arena",
                    "hungergames.hud.killFeed:Kill Feed",
                    "hungergames.hud.killFeedEmpty:-",
                    "hungergames.ui.arenaList.title:Arena list",
                    "hungergames.ui.arenaList.summary:Showing {count} arenas",
                    "hungergames.ui.arenaList.ingameLabel:ingame",
                    "hungergames.ui.arenaList.joinLabel:Join"
            };

    public String getTranslation(String key) {
        return messages.get(key);
    }

    public ItemStack[] getItemsToFillChest() {
        return parseItemStacks(itemsToFillChest);
    }

    private ItemStack[] parseItemStacks(String[] itemsToFillChest) {
        return Arrays.stream(itemsToFillChest).map(entry -> {
            String[] parts = entry.split(":");
            String itemId = parts[0];
            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException _) {
                }
            }
            return new ItemStack(itemId, amount);
        }).toArray(ItemStack[]::new);
    }

    public static final BuilderCodec<MainConfig> CODEC = BuilderCodec.builder(MainConfig.class, MainConfig::new)
            .append(new KeyedCodec<>("MinimumPlayersToStartArena", Codec.INTEGER), (config, f) -> config.minimumPlayersToStartArena = f, (config) -> config.minimumPlayersToStartArena).addValidator(Validators.nonNull()).documentation("minimumPlayersToStartArena").add()
            .append(new KeyedCodec<>("Messages", Codec.STRING_ARRAY), (config, f) -> config.messages = parseMessages(f), (config) -> config.messagesConfigArray).addValidator(Validators.nonNull()).documentation("messages").add()
            .append(new KeyedCodec<>("ItemsToFillChest", Codec.STRING_ARRAY), (config, f) -> config.itemsToFillChest = f, (config) -> config.itemsToFillChest).addValidator(Validators.nonNull()).documentation("worldsWithClockEnabled").add()
            .build();

    private static <FieldType> HashMap<String, String> parseMessages(FieldType f) {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        for (String messageKey : ((String[]) f)) {
            String[] split = messageKey.split(":");
            if (split.length != 2) continue;
            stringStringHashMap.put(split[0], split[1]);
        }
        return stringStringHashMap;
    }


    public int getMinimumPlayersToStartArena() {
        return minimumPlayersToStartArena;
    }
}
