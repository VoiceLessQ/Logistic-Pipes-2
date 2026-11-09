package logisticspipes.modplugins.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;

public class JEIPluginLoader {

    @Nullable
    private static IJeiRuntime jeiRuntime = null;

    public static void setRuntime(@Nonnull IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static void clearRuntime() {
        jeiRuntime = null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void showRecipe(@Nonnull ItemStack stack) {
        if (jeiRuntime == null || stack.isEmpty()) return;
        IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
        jeiRuntime.getRecipesGui().show(
                focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
    }

    /** JEI's tooltip plus mod name for the HUD block info panel (LP1 asked NEI); empty without JEI. */
    @OnlyIn(Dist.CLIENT)
    public static List<Component> getInfo(@Nonnull ItemStack stack) {
        if (jeiRuntime == null || stack.isEmpty()) return Collections.emptyList();
        IIngredientManager manager = jeiRuntime.getIngredientManager();
        List<Component> lines = manager.getIngredientRenderer(VanillaTypes.ITEM_STACK).getTooltip(stack, TooltipFlag.Default.NORMAL);
        return jeiRuntime.getJeiHelpers().getModIdHelper().addModNameToIngredientTooltip(new ArrayList<>(lines), stack, manager.getIngredientHelper(VanillaTypes.ITEM_STACK));
    }
}
