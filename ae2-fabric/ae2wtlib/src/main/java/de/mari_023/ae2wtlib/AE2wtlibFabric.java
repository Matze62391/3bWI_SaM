package de.mari_023.ae2wtlib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import team.reborn.energy.api.EnergyStorage;

import appeng.items.tools.powered.powersink.PoweredItemCapabilities;

import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;
import de.mari_023.ae2wtlib.api.registration.UpgradeHelper;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.networking.AE2wtlibPacket;
import de.mari_023.ae2wtlib.networking.CycleTerminalPacket;
import de.mari_023.ae2wtlib.networking.RestockAmountPacket;
import de.mari_023.ae2wtlib.networking.TerminalSettingsPacket;
import de.mari_023.ae2wtlib.networking.UpdateRestockPacket;
import de.mari_023.ae2wtlib.networking.UpdateWUTPackage;

/**
 * Initializes ae2wtlib (and its API, which is a separate mod on NeoForge). Called by AE2 through its
 * {@code ae2:addon} entrypoint, once AE2's items are registered.
 */
public class AE2wtlibFabric implements ModInitializer {
    private static boolean terminalsRegistered;

    @Override
    public void onInitialize() {
        new AE2wtlibAPIImplementation();
        AE2wtlibConfig.load();

        // Data components first, items use them
        AE2wtlibAdditionalComponents.init();
        for (var entry : AE2wtlibComponents.DR.entrySet()) {
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, entry.getKey(), entry.getValue());
        }
        AE2wtlibItems.DR.register();
        AE2wtlib.registerMenus();
        AE2wtlib.registerTerminals();
        AE2wtlib.registerRecipes();
        AE2wtlib.registerHotkeyActions();
        AE2wtlibCreativeTab.init();
        AE2wtlib.registerGridLinkables();
        AE2wtlib.registerUpgrades();

        registerC2S(CycleTerminalPacket.ID, CycleTerminalPacket.STREAM_CODEC);
        registerC2S(TerminalSettingsPacket.ID, TerminalSettingsPacket.STREAM_CODEC);
        registerS2C(UpdateWUTPackage.ID, UpdateWUTPackage.STREAM_CODEC);
        registerS2C(UpdateRestockPacket.ID, UpdateRestockPacket.STREAM_CODEC);
        registerS2C(RestockAmountPacket.ID, RestockAmountPacket.STREAM_CODEC);

        registerPowerStorageItem(AE2wtlibItems.UNIVERSAL_TERMINAL.get());
        registerPowerStorageItem(AE2wtlibItems.PATTERN_ACCESS_TERMINAL.get());
        registerPowerStorageItem(AE2wtlibItems.PATTERN_ENCODING_TERMINAL.get());

        // Other addons add their terminals while they initialize, so the terminals are registered by the server and
        // client entrypoints, which run after all mods are initialized (NeoForge does it while registering items)

        // Restock (NeoForge: PlayerInteractEvent.RightClickBlock and EntityInteractSpecific)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
                var item = player.getItemInHand(hand);
                AE2wtlibEvents.restock(serverPlayer, item, item.getCount(),
                        stack -> player.setItemInHand(hand, stack));
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hitResult != null && player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
                var item = player.getItemInHand(hand);
                AE2wtlibEvents.restock(serverPlayer, item, item.getCount(),
                        stack -> player.setItemInHand(hand, stack));
            }
            return InteractionResult.PASS;
        });

    }

    /**
     * Runs the terminal registrations of ae2wtlib and the addons. Called once all mods are initialized.
     */
    public static synchronized void finishTerminalRegistration() {
        if (terminalsRegistered) {
            return;
        }
        terminalsRegistered = true;
        AddTerminalEvent.run();
        UpgradeHelper.addUpgrades();
    }

    private static <T extends AE2wtlibPacket> void registerC2S(CustomPacketPayload.Type<T> id,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        PayloadTypeRegistry.serverboundPlay().register(id, streamCodec);
        // Fabric runs play payload handlers on the server thread
        ServerPlayNetworking.registerGlobalReceiver(id, (packet, context) -> packet.processPacketData(context.player()));
    }

    private static <T extends AE2wtlibPacket> void registerS2C(CustomPacketPayload.Type<T> id,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        PayloadTypeRegistry.clientboundPlay().register(id, streamCodec);
        AE2wtlibFabricClientPackets.add(id);
    }

    private static void registerPowerStorageItem(ItemWT item) {
        EnergyStorage.ITEM.registerForItems((stack, context) -> new PoweredItemCapabilities(context, item, item), item);
    }
}
