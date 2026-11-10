package logisticspipes.modplugins.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import network.rs485.logisticspipes.gui.widget.GhostSlot;
import network.rs485.logisticspipes.gui.widget.Item;
import network.rs485.logisticspipes.gui.widget.Unmodifiable;

/**
 * Enables dragging items from the JEI ingredient panel into LP ghost/filter slots.
 */
@OnlyIn(Dist.CLIENT)
public class GhostIngredientHandler implements IGhostIngredientHandler<AbstractContainerScreen<?>> {

    @Override
    @SuppressWarnings("unchecked")
    public <I> List<IGhostIngredientHandler.Target<I>> getTargetsTyped(
            @Nonnull AbstractContainerScreen<?> gui,
            @Nonnull ITypedIngredient<I> typedIngredient,
            boolean doStart) {

        Optional<ItemStack> itemStackOpt = typedIngredient.getItemStack();
        if (itemStackOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<IGhostIngredientHandler.Target<ItemStack>> targets = new ArrayList<>();
        AbstractContainerMenu menu = gui.getMenu();

        for (Slot slot : menu.slots) {
            if (!(slot instanceof GhostSlot)) continue;
            if (slot instanceof Unmodifiable) continue;
            if (!(slot instanceof Item)) continue;

            final Slot ghostSlot = slot;
            targets.add(new IGhostIngredientHandler.Target<ItemStack>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(
                            gui.getGuiLeft() + ghostSlot.x,
                            gui.getGuiTop() + ghostSlot.y,
                            16, 16);
                }

                @Override
                public void accept(ItemStack ingredient) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.gameMode == null) return;

                    ItemStack copy = ingredient.copy();
                    copy.setCount(1);

                    // Place the item into the ghost slot by simulating a left-click
                    // with the ingredient as the carried item.
                    ItemStack prev = mc.player.containerMenu.getCarried();
                    mc.player.containerMenu.setCarried(copy);
                    mc.gameMode.handleInventoryMouseClick(
                            mc.player.containerMenu.containerId,
                            ghostSlot.index,
                            0,
                            ClickType.PICKUP,
                            mc.player);
                    mc.player.containerMenu.setCarried(prev);
                }
            });
        }

        return (List<IGhostIngredientHandler.Target<I>>) (List<?>) targets;
    }

    @Override
    public void onComplete() {
        // nothing to clean up
    }
}
