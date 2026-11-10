package logisticspipes.utils;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;

import logisticspipes.recipes.OrdererDyeRecipe;
import logisticspipes.recipes.ShapelessResetRecipe;

public class CraftingUtil {

    /** Fabric's synchronized recipe view: every recipe on the server, the serializers from {@link #synchronizeRecipes()} on clients. */
    public static Collection<Recipe<?>> getRecipeList(@Nullable Level level) {
        if (level == null) return Collections.emptyList();
        Collection<RecipeHolder<CraftingRecipe>> holders = level.recipeAccess().getSynchronizedRecipes().getAllOfType(RecipeType.CRAFTING);
        return holders.stream().<Recipe<?>>map(RecipeHolder::value).toList();
    }

    /** 26.x no longer sends recipes to clients; LP's crafting table and request table previews need the crafting ones. */
    public static void synchronizeRecipes() {
        RecipeSynchronization.synchronizeRecipeSerializer(ShapedRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(TransmuteRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessResetRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(OrdererDyeRecipe.SERIALIZER);
    }
}
