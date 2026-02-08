package pl.grzegorz2047.hytale.hungergames.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;

public class MainConfig {

    private String[] itemsToFillChest = new String[]{"Weapon_Arrow_Crude:5", "Weapon_Shortbow_Combat:1", "Armor_Iron_Chest:1",  "Weapon_Sword_Crude:1",  "Weapon_Shield_Cobalt:1", "Food_Bread:2"};
    private HashMap<String, String> messages = new HashMap<>();
    private int minimumPlayersToStartArena = 2;
    private int deathmatchArenaSeconds = 60;
    private int startingArenaSeconds = 10;
    private int ingameArenaSeconds = 60;
    private boolean isHudEnabled = true;
    private boolean shouldPrepareInventoryOnLobbyJoin = true;

    public MainConfig() {
        this.messages = parseMessages(messagesConfigArray);
    }

    public String[] getMessagesConfigArray() {
        return messagesConfigArray;
    }

    public HashMap<String, String> getMessages() {
        return messages;
    }

    private final String[] messagesConfigArray = new String[]
            {
                    "noPermission:You do not have permission to perform this action.",
                    "hungergames.arena.notFound:Arena {arenaName} doesn't exist.",
                    "hungergames.arena.notActive:Arena {arenaName} is not active.",
                    "hungergames.arena.alreadyOnArena:You are already on an arena. Leave this first.",
                    "hungergames.arena.generated:Arena generated!",
                    "hungergames.arena.joined:You have joined the arena: {worldName}",
                    "hungergames.arena.numplayerjoined:Player joined {arenaName} {numberOfPlayers}/{maxNumberOfPlayersInArena}",
                    "hungergames.arena.left:You have left the arena: {worldName}",
                    "hungergames.hud.lobby.globalKills:Your kills: {kills}",
                    "hungergames.arena.full:Arena is full",
                    "hungergames.arena.gameEndedWinner: Game ended! Winner: {player}",
                    "hungergames.arena.playerLeftBroadcast:Player left the arena. Current players: {count}",
                    "hungergames.arena.countingCancelled:Countdown cancelled: not enough players",
                    "hungergames.arena.startIn:Start in {seconds}s",
                    "hungergames.arena.arenaStarted:Arena {arenaName} started!",
                    "hungergames.arena.deathmatchStart:Deathmatch start!",
                    "hungergames.arena.deathmatchIn:Deathmatch in {seconds}s",
                    "hungergames.arena.gameEndsIn:Game ends in {seconds}s",
                    "hungergames.arena.gameEndedReturn:Game ended! Returning to lobby.",
                    "hungergames.arena.countingStarted:Counting started: game starts in {seconds}s",
                    "hungergames.block.cannotBreak:<color=#FF0000>You cannot break it</color>",
                    "hungergames.block.cannotPlace:<color=#FF0000>You cannot place it</color>",
                    "hungergames.block.id:{id}",
                    "hungergames.item.cannotDrop:<color=#FF0000>You cannot drop it</color>",
                    "hungergames.item.id:{id}",
                    "hungergames.inventory.opened:Opened inventory",
                    "server.universe.addWorld.worldCreated:World {worldName} created with generator {generator} and storage {storage}.",
                    "server.commands.world.save.savingDone:World {world} saved.",
                    "server.universe.addWorld.alreadyExists:World {worldName} already exists.",
                    "hungergames.arena.enabled:Arena {arenaName} enabled",
                    "hungergames.arena.created:Arena {arenaName} has been created",
                    "hungergames.arena.disabled:Arena {arenaName} disabled",
                    "hungergames.arena.notPlaying:There is no arena you are playing on",
                    "hungergames.arena.alreadyIngame:Arena {arenaName} is already ingame",
                    "hungergames.death.playerBroadcast:Death player: {player}",
                    "hungergames.hud.lobby.welcome:Example.com",
                    "hungergames.hud.time:Time",
                    "hungergames.hud.playersLeft:Players left",
                    "hungergames.hud.arena:Arena",
                    "hungergames.hud.killFeed:Kill Feed",
                    "hungergames.hud.killFeedEmpty:-",
                    "hungergames.hud.yourKills:Your Kills",
                    "hungergames.command.onlyForPlayer:This command can only be used by a player",
                    "hungergames.command.arenaDoesNotExist:Arena '{arenaName}' does not exist",
                    "hungergames.command.cannotModifyActiveArena:<color=#FF0000>Cannot modify arena while it is active or in game</color>",
                    "hungergames.ui.arenaList.title:Arena list",
                    "hungergames.ui.arenaList.summary:Showing {count} arenas",
                    "hungergames.ui.arenaList.ingameLabel:ingame",
                    "hungergames.ui.arenaList.joinLabel:Join"
            };

    public String getTranslation(String key) {
        String s = messages.get(key);
        if (s == null) {
            return key;
        }
        return s;
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
            .append(new KeyedCodec<>("ShouldPrepareInventoryOnLobbyJoin", Codec.BOOLEAN), (config, f) -> config.shouldPrepareInventoryOnLobbyJoin = f, (config) -> config.shouldPrepareInventoryOnLobbyJoin).addValidator(Validators.nonNull()).documentation("shouldPrepareInventoryOnLobbyJoin").add()
            .append(new KeyedCodec<>("IsHudEnabled", Codec.BOOLEAN), (config, f) -> config.isHudEnabled = f, (config) -> config.isHudEnabled).addValidator(Validators.nonNull()).documentation("isHudEnabled").add()
            .append(new KeyedCodec<>("MinimumPlayersToStartArena", Codec.INTEGER), (config, f) -> config.minimumPlayersToStartArena = f, (config) -> config.minimumPlayersToStartArena).addValidator(Validators.nonNull()).documentation("minimumPlayersToStartArena").add()
            .append(new KeyedCodec<>("DeathmatchArenaSeconds", Codec.INTEGER), (config, f) -> config.deathmatchArenaSeconds = f, (config) -> config.deathmatchArenaSeconds).addValidator(Validators.nonNull()).documentation("deathmatchArenaSeconds").add()
            .append(new KeyedCodec<>("StartingArenaSeconds", Codec.INTEGER), (config, f) -> config.startingArenaSeconds = f, (config) -> config.startingArenaSeconds).addValidator(Validators.nonNull()).documentation("startingArenaSeconds").add()
            .append(new KeyedCodec<>("IngameArenaSeconds", Codec.INTEGER), (config, f) -> config.ingameArenaSeconds = f, (config) -> config.ingameArenaSeconds).addValidator(Validators.nonNull()).documentation("ingameArenaSeconds").add()
            .append(new KeyedCodec<>("Messages", Codec.STRING_ARRAY), (config, f) -> config.messages = parseMessages(f), (config) -> config.messagesConfigArray).addValidator(Validators.nonNull()).documentation("messages").add()
            .append(new KeyedCodec<>("ItemsToFillChest", Codec.STRING_ARRAY), (config, f) -> config.itemsToFillChest = f, (config) -> config.itemsToFillChest).addValidator(Validators.nonNull()).documentation("worldsWithClockEnabled").add()
            .build();

    private static <FieldType> HashMap<String, String> parseMessages(FieldType f) {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        for (String messageKey : ((String[]) f)) {
            int colonIndex = messageKey.indexOf(":");
            if (colonIndex == -1) continue;
            String key = messageKey.substring(0, colonIndex);
            String value = messageKey.substring(colonIndex + 1);
            stringStringHashMap.put(key, value);
        }
        return stringStringHashMap;
    }


    public int getMinimumPlayersToStartArena() {
        return minimumPlayersToStartArena;
    }

    public int getDeathmatchArenaSeconds() {
        return deathmatchArenaSeconds;
    }

    public int getStartingArenaSeconds() {
        return startingArenaSeconds;
    }

    public int getIngameArenaSeconds() {
        return ingameArenaSeconds;
    }

    public boolean isHudEnabled() {
        return isHudEnabled;
    }

    public boolean shouldPrepareInventoryOnLobbyJoin() {
        return shouldPrepareInventoryOnLobbyJoin;
    }
}
