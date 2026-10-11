package com.glodblock.github.appflux.common.parts;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartAdjacentApi;
import appeng.util.SettingsFrom;
import com.glodblock.github.appflux.api.EnergyIO;
import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.common.caps.NetworkFEPower;
import com.glodblock.github.appflux.common.me.energy.EnergyCapCache;
import com.glodblock.github.appflux.common.me.energy.UniversalEnergyHandler;
import com.glodblock.github.appflux.common.me.energy.EnergyTickRecord;
import com.glodblock.github.appflux.common.me.service.EnergyDistributeService;
import com.glodblock.github.appflux.common.me.service.IEnergyDistributor;
import com.glodblock.github.appflux.config.AFConfig;
import com.glodblock.github.appflux.container.ContainerFluxAccessor;
import com.glodblock.github.appflux.util.IOSignal;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.EnergyStorage;
import org.jetbrains.annotations.Nullable;

public class PartFluxAccessor extends AEBasePart implements IEnergyDistributor {

    /**
     * NeoForge notifies the ticker when a neighbor's energy capability changes. Fabric has no such event, so sides
     * without an energy accepter are retried every {@value #RETRY_TICKS} ticks.
     */
    private static final int RETRY_TICKS = 40;


    private EnergyCapCache cacheApi;
    private boolean blocked = false;
    private boolean fast = false;
    private EnergyIO io = EnergyIO.BOTH;
    private EnergyTickRecord lastTick = new EnergyTickRecord();
    private final IActionSource source = IActionSource.ofMachine(this);

    public PartFluxAccessor(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getMainNode().setIdlePowerUsage(1.0).addService(IEnergyDistributor.class, this);
    }

    @Override
    public boolean isActive() {
        return this.getMainNode().isActive();
    }

    private void initCache() {
        this.cacheApi = new EnergyCapCache((ServerLevel) this.getLevel(), this.getBlockEntity().getBlockPos(), this::getGrid);
    }

    private IGrid getGrid() {
        if (this.getGridNode() == null) {
            return null;
        }
        return this.getGridNode().getGrid();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2.0, 2.0, 14.0, 14.0, 14.0, 16.0);
        bch.addBox(4.0, 4.0, 12.0, 12.0, 12.0, 14.0);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 2.0F;
    }

    public IStorageService getStorage() {
        if (this.getGridNode() != null) {
            return this.getGridNode().getGrid().getStorageService();
        }
        return null;
    }

    public EnergyStorage getEnergyStorage() {
        if (this.getStorage() != null) {
            return new NetworkFEPower(this.getStorage(), this.source, IOSignal.of(this::getIOMode));
        } else {
            return EnergyStorage.EMPTY;
        }
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(ContainerFluxAccessor.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (input.has(AFSingletons.FAST_MODE)) {
            this.fast = input.getOrDefault(AFSingletons.FAST_MODE, false);
            this.io = input.getOrDefault(AFSingletons.IO_MODE, EnergyIO.BOTH);
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder output) {
        super.exportSettings(mode, output);
        if (mode == SettingsFrom.MEMORY_CARD) {
            output.set(AFSingletons.FAST_MODE, this.fast);
            output.set(AFSingletons.IO_MODE, this.io);
        }
    }

    @Override
    public void readFromNBT(ValueInput input) {
        super.readFromNBT(input);
        this.fast = input.getBooleanOr("fast", false);
        this.io = input.read("io_mode", EnergyIO.CODEC).orElse(EnergyIO.BOTH);
    }

    @Override
    public void writeToNBT(ValueOutput output) {
        super.writeToNBT(output);
        output.putBoolean("fast", this.fast);
        output.store("io_mode", EnergyIO.CODEC, this.io);
    }

    @Override
    public void distribute(long ticks) {
        if (this.io.isOutput()) {
            if (this.getLevel() == null) {
                return;
            }
            if (this.cacheApi == null) {
                this.initCache();
            }
            if (ticks % RETRY_TICKS == 0) {
                this.blocked = false;
            }
            var storage = this.getStorage();
            var d = this.getSide();
            var gird = this.getGrid();
            if (storage != null && d != null) {
                if (!this.blocked) {
                    if (this.isFastMode() || this.lastTick.needTick(ticks)) {
                        long sent = UniversalEnergyHandler.send(this.cacheApi, d, storage, this.source);
                        if (sent == -1) {
                            this.blocked = true;
                        } else {
                            this.lastTick.sent(sent);
                        }
                    }
                }
                if (AFConfig.selfCharge() && gird != null) {
                    UniversalEnergyHandler.chargeNetwork(gird.getService(IEnergyService.class), storage, this.source);
                }
            }
        }
    }

    @Override
    public void setServiceHost(@Nullable EnergyDistributeService service) {
        if (service != null) {
            service.wake(this);
            this.blocked = false;
            this.lastTick = new EnergyTickRecord();
        }
    }

    @Override
    public boolean isFastMode() {
        return this.fast;
    }

    @Override
    public void setFastMode(boolean mode) {
        this.fast = mode;
    }

    @Override
    public EnergyIO getIOMode() {
        return this.io;
    }

    @Override
    public void setIOMode(EnergyIO mode) {
        this.io = mode;
    }

}
