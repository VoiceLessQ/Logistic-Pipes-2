package logisticspipes.modplugins.jei;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import logisticspipes.LPConstants;
import network.rs485.logisticspipes.gui.BaseGuiContainer;

@JeiPlugin
@OnlyIn(Dist.CLIENT)
public class LPJEIPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(LPConstants.LP_MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Register extra-area handler for all LP container screens
        registration.addGenericGuiContainerHandler(BaseGuiContainer.class, new AdvancedGuiHandler());
        // Ghost ingredient handler: registered on AbstractContainerScreen so JEI calls us for any
        // LP screen; the handler itself checks for GhostSlots in the open menu.
        registration.addGhostIngredientHandler(
                (Class) AbstractContainerScreen.class, new GhostIngredientHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JEIPluginLoader.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JEIPluginLoader.clearRuntime();
    }
}
