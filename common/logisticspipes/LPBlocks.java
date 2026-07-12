package logisticspipes;

import net.neoforged.neoforge.registries.DeferredHolder;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;

/**
 * Holds RegistryObject references to all registered LP blocks.
 * Access via .get() — e.g. LPBlocks.pipe.get()
 *
 * NOTE: Call sites previously used plain fields (e.g. LPBlocks.pipe) — all updated
 * to LPBlocks.pipe.get() as part of task #10.
 */
public class LPBlocks {

	public static final DeferredHolder<?, LogisticsSolidBlock>           frame           = LPRegistries.FRAME;
	public static final DeferredHolder<?, LogisticsSolidBlock>           powerJunction   = LPRegistries.POWER_JUNCTION;
	public static final DeferredHolder<?, LogisticsSolidBlock>           securityStation = LPRegistries.SECURITY_STATION;
	public static final DeferredHolder<?, LogisticsSolidBlock>           crafter         = LPRegistries.CRAFTER;
	public static final DeferredHolder<?, LogisticsSolidBlock>           crafterFuzzy    = LPRegistries.CRAFTER_FUZZY;
	public static final DeferredHolder<?, LogisticsSolidBlock>           statisticsTable = LPRegistries.STATISTICS_TABLE;
	public static final DeferredHolder<?, LogisticsSolidBlock>           powerProviderRF = LPRegistries.POWER_PROVIDER_RF;
	public static final DeferredHolder<?, LogisticsSolidBlock>           powerProviderEU = LPRegistries.POWER_PROVIDER_EU;
	public static final DeferredHolder<?, LogisticsSolidBlock>           powerProviderMJ = LPRegistries.POWER_PROVIDER_MJ;
	public static final DeferredHolder<?, LogisticsSolidBlock>           programCompiler = LPRegistries.PROGRAM_COMPILER;
	public static final DeferredHolder<?, LogisticsBlockGenericPipe>          pipe           = LPRegistries.PIPE;
	public static final DeferredHolder<?, LogisticsBlockGenericSubMultiBlock> subMultiblock  = LPRegistries.SUB_MULTIBLOCK;

}
