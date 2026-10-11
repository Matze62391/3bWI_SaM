package net.pedroksl.ae2addonlib.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.color.item.ItemTintSources;
import net.pedroksl.ae2addonlib.client.screens.OutputDirectionScreen;
import net.pedroksl.ae2addonlib.client.screens.SetAmountScreen;
import net.pedroksl.ae2addonlib.registry.helpers.LibMenus;
import net.pedroksl.ae2addonlib.util.ColoredItemTintSource;

import appeng.client.InitScreens;

public class AE2AddonLibClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InitScreens.register(
                LibMenus.OUTPUT_DIRECTION.get(), OutputDirectionScreen::new, "/screens/output_direction.json");
        InitScreens.register(LibMenus.SET_AMOUNT.get(), SetAmountScreen::new, "/screens/set_amount.json");

        ItemTintSources.ID_MAPPER.put(ColoredItemTintSource.ID, ColoredItemTintSource.MAP_CODEC);

        new LibClientNetworkHandler().registerPackets(new RegisterClientPayloadHandlersEvent());
    }
}
