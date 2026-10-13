package com.glodblock.github.extendedae.container.pattern;

import appeng.api.crafting.IPatternDetails;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.IdentityHashMap;

public class PatternGuiHandler {

    private static final Object2ObjectMap<Identifier, IContainerPattern> factory = new Object2ObjectOpenHashMap<>();
    private static final Object2ReferenceMap<Identifier, MenuType<?>> types = new Object2ReferenceOpenHashMap<>();
    private static final IdentityHashMap<Class<?>, Identifier> patternID = new IdentityHashMap<>();
    private static final BiMap<Integer, Identifier> internal = HashBiMap.create();
    private static int IDZ = 0;

    public static void addPatternHandler(Class<?> clazz, Identifier id) {
        patternID.put(clazz, id);
    }

    public static boolean open(Player player, IPatternDetails details, ItemStack pattern) {
        var cls = details.getClass();
        if (patternID.containsKey(cls)) {
            open(player, patternID.get(cls), pattern);
            return true;
        } else {
            return false;
        }
    }

    public static void open(Player player, Identifier id, ItemStack pattern) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        var title = Component.translatable("epp.pattern." + id);
        player.openMenu(new ExtendedMenuProvider<OpenData>() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int wnd, Inventory p, Player pl) {
                var f = factory.get(id);
                var t = types.get(id);
                return f.create(t, wnd, player.level(), pattern);
            }

            @Override
            public OpenData getScreenOpeningData(ServerPlayer player) {
                return new OpenData(internal.inverse().get(id), pattern);
            }
        });
    }

    private static AbstractContainerMenu from(int containerId, Inventory inv, OpenData data) {
        var world = inv.player.level();
        var f = factory.get(internal.get(data.id()));
        var t = types.get(internal.get(data.id()));
        return f.create(t, containerId, world, data.pattern());
    }

    private record OpenData(int id, ItemStack pattern) {
        static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenData::id,
                ItemStack.STREAM_CODEC, OpenData::pattern,
                OpenData::new);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerMenu> MenuType<T> register(Identifier id, IContainerPattern containerFactory) {
        factory.put(id, containerFactory);
        internal.put(IDZ, id);
        IDZ++;
        var type = new ExtendedMenuType<>(PatternGuiHandler::from, OpenData.STREAM_CODEC);
        types.put(id, type);
        return (MenuType<T>) type;
    }

    @FunctionalInterface
    public interface IContainerPattern {

        ContainerPattern create(MenuType<?> menuType, int id, Level world, ItemStack pattern);

    }

}
