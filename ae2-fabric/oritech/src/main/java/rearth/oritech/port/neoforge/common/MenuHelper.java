package rearth.oritech.port.neoforge.common;

import java.util.OptionalInt;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import rearth.oritech.port.neoforge.common.extensions.IMenuProviderExtension;

/**
 * Opens menus whose type was created through {@code IMenuTypeExtension}: the extra data and the provider's
 * {@link IMenuProviderExtension#writeClientSideData} output are sent to the client as the screen opening data.
 */
public final class MenuHelper {
    private MenuHelper() {}

    public static OptionalInt openMenu(Player player, @Nullable MenuProvider provider) {
        return openMenu(player, provider, (Consumer<RegistryFriendlyByteBuf>) null);
    }

    public static OptionalInt openMenu(Player player, @Nullable MenuProvider provider, BlockPos pos) {
        return openMenu(player, provider, buf -> buf.writeBlockPos(pos));
    }

    public static OptionalInt openMenu(Player player, @Nullable MenuProvider provider,
            @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        if (provider == null || !(player instanceof ServerPlayer serverPlayer)) {
            return OptionalInt.empty();
        }
        return serverPlayer.openMenu(new Wrapper(provider, extraDataWriter));
    }

    private static final class Wrapper implements ExtendedMenuProvider<byte[]> {
        private final MenuProvider delegate;
        @Nullable
        private final Consumer<RegistryFriendlyByteBuf> extraDataWriter;
        @Nullable
        private AbstractContainerMenu menu;

        private Wrapper(MenuProvider delegate, @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
            this.delegate = delegate;
            this.extraDataWriter = extraDataWriter;
        }

        @Override
        public Component getDisplayName() {
            return delegate.getDisplayName();
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            menu = delegate.createMenu(containerId, inventory, player);
            return menu;
        }

        @Override
        public boolean shouldCloseCurrentScreen() {
            return !(delegate instanceof IMenuProviderExtension extension)
                    || extension.shouldTriggerClientSideContainerClosingOnOpen();
        }

        @Override
        public byte[] getScreenOpeningData(ServerPlayer player) {
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            try {
                if (extraDataWriter != null) {
                    extraDataWriter.accept(buf);
                }
                if (delegate instanceof IMenuProviderExtension extension && menu != null) {
                    extension.writeClientSideData(menu, buf);
                }
                var data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return data;
            } finally {
                buf.release();
            }
        }
    }
}
