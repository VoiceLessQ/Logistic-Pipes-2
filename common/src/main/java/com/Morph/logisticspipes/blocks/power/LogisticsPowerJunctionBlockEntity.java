package com.Morph.logisticspipes.blocks.power;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.api.ILogisticsPowerProvider;
import com.Morph.logisticspipes.config.LPConfig;

/**
 * Central LP power buffer block entity.
 *
 * Ported from LP1 LogisticsPowerJunctionTileEntity, stripped of:
 *   - IC2 / BC integration (mods absent on 1.21.1)
 *   - HUD watcher system / OpenComputers / ComputerCraft hooks (deferred)
 *   - GUI player tracking + sync packets (deferred)
 *   - POWER_USAGE_MULTIPLIER config (no config system yet — fixed 1.0 multiplier)
 *
 * Storage model carried from LP1:
 *   - internal LP units, maxStorage() = 2_000_000
 *   - RF→LP conversion: RF_DIVISOR = 2 (2 RF per 1 LP)
 *   - internalRFbuffer holds the fractional RF that hasn't yet ticked into a whole LP
 *
 * Capability/platform wiring (NeoForge IEnergyStorage, Fabric Team Reborn Energy)
 * is added separately in the platform modules using {@link #receiveEnergyRf},
 * {@link #getEnergyStoredRf}, and {@link #getMaxEnergyStoredRf}.
 */
public class LogisticsPowerJunctionBlockEntity extends BlockEntity implements ILogisticsPowerProvider {

    /** Default storage cap (LP units). Overridden by {@link LPConfig#POWER_MAX_STORAGE}. */
    public static final int MAX_STORAGE_DEFAULT = 2_000_000;
    public static final int RF_DIVISOR = 2;

    /** Live storage cap — reads the config each call so reloads are picked up. */
    private static int maxStorage() {
        int v = LPConfig.POWER_MAX_STORAGE;
        return v > 0 ? v : MAX_STORAGE_DEFAULT;
    }

    private int internalStorage = 0;
    private int internalRFbuffer = 0;
    private boolean needMorePowerTriggerCheck = true;

    public LogisticsPowerJunctionBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlocks.POWER_JUNCTION_BLOCK_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------------
    // ILogisticsPowerProvider
    // ---------------------------------------------------------------------

    @Override
    public int getPowerLevel() {
        return internalStorage;
    }

    @Override
    public BlockPos getProviderPos() {
        return getBlockPos();
    }

    @Override
    public boolean useEnergy(int amount) {
        return useEnergy(amount, null);
    }

    @Override
    public boolean canUseEnergy(int amount) {
        return canUseEnergy(amount, null);
    }

    @Override
    public boolean useEnergy(int amount, List<Object> providersToIgnore) {
        if (providersToIgnore != null && providersToIgnore.contains(this)) return false;
        if (LPConfig.POWER_USAGE_DISABLED) return true;
        int effective = (int) Math.round(amount * LPConfig.POWER_USAGE_MULTIPLIER);
        if (internalStorage < effective) return false;
        internalStorage -= effective;
        if (internalStorage < maxStorage() / 2) needMorePowerTriggerCheck = true;
        setChanged();
        return true;
    }

    @Override
    public boolean canUseEnergy(int amount, List<Object> providersToIgnore) {
        if (providersToIgnore != null && providersToIgnore.contains(this)) return false;
        if (LPConfig.POWER_USAGE_DISABLED) return true;
        return internalStorage >= (int) Math.round(amount * LPConfig.POWER_USAGE_MULTIPLIER);
    }

    // ---------------------------------------------------------------------
    // Energy I/O (used by the platform-side IEnergyStorage / TR-Energy adapter)
    // ---------------------------------------------------------------------

    /** Free LP-unit space remaining in storage. */
    public int freeSpace() {
        return maxStorage() - internalStorage;
    }

    /**
     * Receive RF and convert to LP. Mirrors LP1's IEnergyStorage.receiveEnergy.
     * Returns RF actually accepted.
     */
    public int receiveEnergyRf(int maxReceive, boolean simulate) {
        if (freeSpace() < 1) return 0;
        int rfSpace  = freeSpace() * RF_DIVISOR - internalRFbuffer;
        int rfToTake = Math.min(maxReceive, rfSpace);
        if (rfToTake <= 0) return 0;
        if (!simulate) {
            addEnergy(rfToTake / RF_DIVISOR);
            internalRFbuffer += rfToTake % RF_DIVISOR;
            if (internalRFbuffer >= RF_DIVISOR) {
                addEnergy(1);
                internalRFbuffer -= RF_DIVISOR;
            }
        }
        return rfToTake;
    }

    /** Current stored energy expressed in RF (for IEnergyStorage.getEnergyStored). */
    public int getEnergyStoredRf() {
        return internalStorage * RF_DIVISOR + internalRFbuffer;
    }

    /** Max storable energy expressed in RF (for IEnergyStorage.getMaxEnergyStored). */
    public int getMaxEnergyStoredRf() {
        return maxStorage() * RF_DIVISOR;
    }

    public void addEnergy(int amount) {
        if (level == null || level.isClientSide) return;
        internalStorage = Math.min(maxStorage(), internalStorage + amount);
        if (internalStorage == maxStorage()) needMorePowerTriggerCheck = false;
        setChanged();
    }

    // ---------------------------------------------------------------------
    // NBT
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("powerLevel", internalStorage);
        tag.putInt("rfBuffer", internalRFbuffer);
        tag.putBoolean("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("powerLevel")) internalStorage = tag.getInt("powerLevel");
        if (tag.contains("rfBuffer"))   internalRFbuffer = tag.getInt("rfBuffer");
        if (tag.contains("needMorePowerTriggerCheck")) {
            needMorePowerTriggerCheck = tag.getBoolean("needMorePowerTriggerCheck");
        }
    }
}
