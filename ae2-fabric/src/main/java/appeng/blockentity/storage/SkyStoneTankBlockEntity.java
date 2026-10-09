package appeng.blockentity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;

import appeng.blockentity.AEBaseBlockEntity;

public class SkyStoneTankBlockEntity extends AEBaseBlockEntity {

    public static final int BUCKET_CAPACITY = 16;

    protected SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(
            FluidConstants.BUCKET * BUCKET_CAPACITY, () -> {
                SkyStoneTankBlockEntity.this.markForUpdate();
                SkyStoneTankBlockEntity.this.setChanged();
            });

    public SkyStoneTankBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Override
    public void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        tank.writeValue(data.child("tank"));
    }

    @Override
    public void loadTag(ValueInput data) {
        super.loadTag(data);
        tank.readValue(data.childOrEmpty("tank"));
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        return FluidStorageUtil.interactWithFluidStorage(tank, player, hand);
    }

    public SingleFluidStorage getFluidHandler() {
        return tank;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        var input = TagValueInput.create(ProblemReporter.DISCARDING, data.registryAccess(), data.readNbt());
        tank.readValue(input);
        return ret;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, data.registryAccess());
        tank.writeValue(output);
        data.writeNbt(output.buildResult());
    }
}
