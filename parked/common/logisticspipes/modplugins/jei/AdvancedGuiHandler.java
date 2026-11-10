package logisticspipes.modplugins.jei;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;

import network.rs485.logisticspipes.gui.BaseGuiContainer;
import network.rs485.logisticspipes.util.IRectangle;

/**
 * Exposes extra GUI areas (outside the main container window) to JEI
 * so it knows to move its ingredient panel out of the way.
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        if (!(containerScreen instanceof BaseGuiContainer)) {
            return List.of();
        }
        BaseGuiContainer gui = (BaseGuiContainer) containerScreen;
        return gui.getExtraGuiAreas().stream()
                .map(r -> new Rect2i(r.getRoundedX(), r.getRoundedY(), r.getRoundedWidth(), r.getRoundedHeight()))
                .collect(Collectors.toList());
    }
}
