package logisticspipes.modplugins.jei;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
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
}
