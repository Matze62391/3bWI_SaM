package appeng.server.testplots;

import net.minecraft.core.BlockPos;

import appeng.core.definitions.AEBlocks;
import appeng.init.InitVillager;
import appeng.server.testworld.PlotBuilder;

@TestPlotClass
public final class VillagerTestPlots {
    private VillagerTestPlots() {
    }

    /**
     * The charger is the job site of the fluix researcher.
     */
    @TestPlot("fluix_researcher_workstation")
    public static void fluixResearcherWorkstation(PlotBuilder plot) {
        plot.block(BlockPos.ZERO, AEBlocks.CHARGER);
        plot.test(helper -> helper.startSequence()
                .thenWaitUntil(() -> {
                    var type = helper.getLevel().getPoiManager().getType(helper.absolutePos(BlockPos.ZERO));
                    helper.check(type.isPresent() && type.get().is(InitVillager.POI_KEY),
                            "charger is not a point of interest for the fluix researcher", BlockPos.ZERO);
                })
                .thenSucceed());
    }
}
