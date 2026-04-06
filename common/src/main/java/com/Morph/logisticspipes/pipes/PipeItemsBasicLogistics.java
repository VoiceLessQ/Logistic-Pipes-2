package com.Morph.logisticspipes.pipes;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Basic logistics pipe — relays items, accepts items as a default sink.
 * Ported from LP 1.12.2 PipeItemsBasicLogistics.
 */
public class PipeItemsBasicLogistics extends CoreRoutedPipe {

    private static final SinkReply DEFAULT_SINK = new SinkReply(
            SinkReply.FixedPriority.DefaultRoute, 0, false, true, Integer.MAX_VALUE);

    public PipeItemsBasicLogistics(Item item) {
        super(new PipeTransportLogistics(), item);
    }

    /**
     * The basic pipe accepts any item as a default route.
     * Returns a low-priority SinkReply so higher-priority pipes (item sink, provider, etc.)
     * take precedence.
     */
    public SinkReply canSink(ItemIdentifier item) {
        return DEFAULT_SINK;
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) {
        return 0; // texture index — Phase 6 rendering
    }
}
