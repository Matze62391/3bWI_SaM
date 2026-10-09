package appeng.server.testplots;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.misc.CrankBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.server.testworld.PlotBuilder;

/**
 * The first steps of AE2 progression in survival: growing certus quartz, charging it and turning it into fluix.
 */
@TestPlotClass
public final class SurvivalTestPlots {
    private SurvivalTestPlots() {
    }

    /**
     * Charged certus quartz, redstone and nether quartz thrown into water turn into fluix crystals.
     */
    @TestPlot(value = "fluix_crystals_in_water", maxTicks = 10 * 20)
    public static void fluixCrystalsInWater(PlotBuilder plot) {
        plot.block("[-1,1] -1 [-1,1]", Blocks.GLASS);
        plot.block("[-1,1] 0 [-1,1]", Blocks.GLASS);
        plot.block("0 0 0", Blocks.WATER);
        plot.test(helper -> {
            var center = Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)).add(0, 0.2, 0);
            for (var item : new Item[] { AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem(),
                    Items.REDSTONE, Items.QUARTZ }) {
                var entity = new ItemEntity(helper.getLevel(), center.x, center.y,
                        center.z, new ItemStack(item));
                entity.setDeltaMovement(Vec3.ZERO);
                helper.getLevel().addFreshEntity(entity);
            }
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertItemEntityPresent(AEItems.FLUIX_CRYSTAL.asItem(), BlockPos.ZERO,
                            2))
                    .thenSucceed();
        });
    }

    /**
     * Budding certus quartz grows quartz buds on its sides.
     */
    @TestPlot("certus_quartz_growth")
    public static void certusQuartzGrowth(PlotBuilder plot) {
        // Leave room for the buds to grow
        plot.block("[-1,1] [-1,1] [-1,1]", Blocks.AIR);
        plot.block(BlockPos.ZERO, AEBlocks.FLAWLESS_BUDDING_QUARTZ);
        plot.test(helper -> helper.startSequence()
                .thenExecute(() -> {
                    var level = helper.getLevel();
                    var pos = helper.absolutePos(BlockPos.ZERO);
                    // Speed up random ticks
                    for (var i = 0; i < 2000; i++) {
                        level.getBlockState(pos).randomTick(level, pos, level.getRandom());
                    }
                    var grown = false;
                    var neighbors = new StringBuilder();
                    for (var side : Direction.values()) {
                        var state = level.getBlockState(pos.relative(side));
                        neighbors.append(side).append('=').append(state).append(' ');
                        grown |= state.is(AEBlocks.SMALL_QUARTZ_BUD.block())
                                || state.is(AEBlocks.MEDIUM_QUARTZ_BUD.block())
                                || state.is(AEBlocks.LARGE_QUARTZ_BUD.block())
                                || state.is(AEBlocks.QUARTZ_CLUSTER.block());
                    }
                    helper.check(grown, "no quartz bud grew on the budding certus quartz: " + level.getBlockState(pos) + " " + neighbors, BlockPos.ZERO);
                })
                .thenSucceed());
    }

    /**
     * Turning a crank on top of a charger charges certus quartz without any other power source.
     */
    @TestPlot(value = "crank_charges_certus_quartz", maxTicks = 30 * 20)
    public static void crankChargesCertusQuartz(PlotBuilder plot) {
        plot.blockEntity(BlockPos.ZERO, AEBlocks.CHARGER, charger -> {
            charger.getInternalInventory().insertItem(0, AEItems.CERTUS_QUARTZ_CRYSTAL.stack(), false);
        });
        plot.block(BlockPos.ZERO.above(), Blocks.AIR);
        plot.test(helper -> helper.startSequence()
                // Attach the crank once the charger exists, like a player would
                .thenExecute(() -> helper.setBlock(BlockPos.ZERO.above(), AEBlocks.CRANK.block().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.UP)))
                .thenWaitUntil(() -> {
                    // Turn it about once per second like a player. Turning it while the charger can't take more
                    // power breaks the crank.
                    if (helper.getLevel().getGameTime() % 20 == 0) {
                        helper.getBlockEntity(BlockPos.ZERO.above(), CrankBlockEntity.class).power();
                    }
                    var content = helper.countContainerContentAt(BlockPos.ZERO);
                    helper.assertEquals(BlockPos.ZERO, 1L,
                            content.get(AEItemKey.of(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED)));
                })
                .thenSucceed());
    }
}
