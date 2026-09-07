package logisticspipes.recipes.condition;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;

import logisticspipes.config.Configs;

/** True when betaUpgradeRecipes is set: swaps in the circuit-only upgrade recipes. */
public class BetaRecipesCondition implements ICondition {

	public static final MapCodec<BetaRecipesCondition> CODEC = MapCodec.unit(BetaRecipesCondition::new);

	@Override
	public boolean test(IContext context) {
		return Configs.getBetaUpgradeRecipes();
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
