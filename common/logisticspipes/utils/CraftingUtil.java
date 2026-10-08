package logisticspipes.utils;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CraftingUtil {

    /** Uses the level's recipe manager so remote clients see the synced recipes too. */
    public static Collection<Recipe<?>> getRecipeList(@Nullable Level level) {
        RecipeManager manager;
        if (level != null) {
            manager = level.getRecipeManager();
        } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return Collections.emptyList();
            manager = server.getRecipeManager();
        }
        return manager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(holder -> (Recipe<?>) holder.value())
                .collect(java.util.stream.Collectors.toList());
    }
}
