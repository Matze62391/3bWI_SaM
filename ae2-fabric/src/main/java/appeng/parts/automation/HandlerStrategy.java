package appeng.parts.automation;

import javax.annotation.Nullable;

import com.google.common.primitives.Ints;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.me.storage.ExternalStorageFacade;
import appeng.util.Platform;

public abstract class HandlerStrategy<C, S> {
    private final AEKeyType keyType;

    public HandlerStrategy(AEKeyType keyType) {
        this.keyType = keyType;
    }

    public boolean isSupported(AEKey what) {
        return what.getType() == keyType;
    }

    public AEKeyType getKeyType() {
        return keyType;
    }

    public abstract ExternalStorageFacade getFacade(C handler);

    @Nullable
    public abstract S getStack(AEKey what, long amount);

    public abstract long insert(C handler, AEKey what, long amount, Actionable mode);

    public static final HandlerStrategy<Storage<ItemVariant>, ItemStack> ITEMS = new HandlerStrategy<>(
            AEKeyType.items()) {
        @Override
        public boolean isSupported(AEKey what) {
            return AEItemKey.is(what);
        }

        @Override
        public ExternalStorageFacade getFacade(Storage<ItemVariant> handler) {
            return ExternalStorageFacade.ofItemHandler(handler);
        }

        @Override
        public long insert(Storage<ItemVariant> handler, AEKey what, long amount, Actionable mode) {
            if (what instanceof AEItemKey itemKey && amount > 0) {
                var insertAmount = amount;

                try (var tx = Platform.openOrJoinTx()) {
                    var inserted = handler.insert(itemKey.toVariant(), insertAmount, tx);
                    if (!mode.isSimulate()) {
                        tx.commit();
                    }
                    return inserted;
                }
            }

            return 0;
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public ItemStack getStack(AEKey what, long amount) {
            if (what instanceof AEItemKey itemKey) {
                return itemKey.toStack(Ints.saturatedCast(amount));
            }
            return null;
        }
    };

    public static final HandlerStrategy<Storage<FluidVariant>, GenericStack> FLUIDS = new HandlerStrategy<>(
            AEKeyType.fluids()) {
        @Override
        public boolean isSupported(AEKey what) {
            return AEFluidKey.is(what);
        }

        @Override
        public ExternalStorageFacade getFacade(Storage<FluidVariant> handler) {
            return ExternalStorageFacade.ofFluidHandler(handler);
        }

        @Override
        public long insert(Storage<FluidVariant> handler, AEKey what, long amount, Actionable mode) {
            if (what instanceof AEFluidKey fluidKey && amount > 0) {
                var insertAmount = Ints.saturatedCast(amount);

                try (var tx = Platform.openOrJoinTx()) {
                    var inserted = handler.insert(fluidKey.toVariant(), insertAmount, tx);
                    if (!mode.isSimulate()) {
                        tx.commit();
                    }
                    return inserted;
                }
            }

            return 0;
        }

        @Override
        public GenericStack getStack(AEKey what, long amount) {
            if (what instanceof AEFluidKey fluidKey) {
                return new GenericStack(fluidKey, amount);
            }
            return null;
        }
    };

}
