package logisticspipes.proxy.side;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import logisticspipes.LogisticsPipes;
import logisticspipes.utils.CraftingUtil;

/** Dev-only ({@code -Dlp.devtest=true}): logs what the client-side crafting preview sees, 100 ticks after joining a world. */
public final class DevTestClient {

	private static int ticks;

	private DevTestClient() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level == null) {
				ticks = 0;
				return;
			}
			if (++ticks != 100) return;
			List<Recipe<?>> recipes = List.copyOf(CraftingUtil.getRecipeList(mc.level));
			long lp = recipes.stream().filter(r -> mc.level.recipeAccess().getSynchronizedRecipes().recipes().stream()
					.anyMatch(h -> h.value() == r && h.id().identifier().getNamespace().equals("logisticspipes"))).count();
			ItemStack p = new ItemStack(Items.OAK_PLANKS);
			CraftingInput sticks = CraftingInput.of(1, 2, List.of(p, p));
			String preview = recipes.stream().filter(r -> ((Recipe) r).matches(sticks, mc.level)).findFirst()
					.map(r -> ((Recipe) r).assemble(sticks).toString()).orElse("none");
			LogisticsPipes.log.info("[lp.devtest] client crafting recipes={} lp={} planks-column preview={} singleplayer={}",
					recipes.size(), lp, preview, Minecraft.getInstance().hasSingleplayerServer());
		});
	}
}
