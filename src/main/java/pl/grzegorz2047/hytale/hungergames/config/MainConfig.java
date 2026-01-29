package pl.grzegorz2047.hytale.hungergames.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.Arrays;

public class MainConfig {

    private String[] itemsToFillChest = new String[]{"Soil_Grass_Full:6"};


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
            .append(new KeyedCodec<>("ItemsToFillChest", Codec.STRING_ARRAY), (config, f) -> config.itemsToFillChest = f, (config) -> config.itemsToFillChest).addValidator(Validators.nonNull()).documentation("worldsWithClockEnabled").add()
            .build();


}
