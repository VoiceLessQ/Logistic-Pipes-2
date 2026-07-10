package logisticspipes.utils;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CraftingUtil {

    @SuppressWarnings("unchecked")
    public static Collection<Recipe<?>> getRecipeList() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Collections.emptyList();
        return (Collection<Recipe<?>>) (Collection<?>) server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
    }
}
