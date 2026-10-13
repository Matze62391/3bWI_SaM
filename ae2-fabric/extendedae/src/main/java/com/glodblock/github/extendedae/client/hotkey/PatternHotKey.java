package com.glodblock.github.extendedae.client.hotkey;

import appeng.crafting.pattern.EncodedPatternItem;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CPatternKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import java.util.List;

public class PatternHotKey {

    public static void onInit() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> hookTooltip(stack, lines));
    }

    private static void hookTooltip(ItemStack stack, List<Component> tooltip) {
        if (EAEHotKey.isKeyBound(EAEHotKey.VIEW_PATTERN) && stack.getItem() instanceof EncodedPatternItem) {
            tooltip.add(1, Component.translatable("pattern.tooltip", EAEHotKey.VIEW_PATTERN.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            if (EAEHotKey.pressed(EAEHotKey.VIEW_PATTERN)) {
                EAENetworkHandler.INSTANCE.sendToServer(new CPatternKey(stack));
            }
        }
    }

}
