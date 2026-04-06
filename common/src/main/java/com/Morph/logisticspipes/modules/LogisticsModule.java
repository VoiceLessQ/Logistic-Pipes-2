package com.Morph.logisticspipes.modules;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

public abstract class LogisticsModule {

    @Nullable
    protected CoreRoutedPipe _service;

    public void setService(CoreRoutedPipe service) {
        this._service = service;
    }

    public abstract void tick();

    @Nullable
    public abstract SinkReply sinksItem(ItemIdentifier item);

    public void saveToNBT(CompoundTag tag, String prefix) {}

    public void loadFromNBT(CompoundTag tag, String prefix) {}
}
