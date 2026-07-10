package logisticspipes;

import net.neoforged.neoforge.registries.RegistryObject;

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

	public static final RegistryObject<LogisticsSolidBlock>           frame           = LPRegistries.FRAME;
	public static final RegistryObject<LogisticsSolidBlock>           powerJunction   = LPRegistries.POWER_JUNCTION;
	public static final RegistryObject<LogisticsSolidBlock>           securityStation = LPRegistries.SECURITY_STATION;
	public static final RegistryObject<LogisticsSolidBlock>           crafter         = LPRegistries.CRAFTER;
	public static final RegistryObject<LogisticsSolidBlock>           crafterFuzzy    = LPRegistries.CRAFTER_FUZZY;
	public static final RegistryObject<LogisticsSolidBlock>           statisticsTable = LPRegistries.STATISTICS_TABLE;
	public static final RegistryObject<LogisticsSolidBlock>           powerProviderRF = LPRegistries.POWER_PROVIDER_RF;
	public static final RegistryObject<LogisticsSolidBlock>           powerProviderEU = LPRegistries.POWER_PROVIDER_EU;
	public static final RegistryObject<LogisticsSolidBlock>           powerProviderMJ = LPRegistries.POWER_PROVIDER_MJ;
	public static final RegistryObject<LogisticsSolidBlock>           programCompiler = LPRegistries.PROGRAM_COMPILER;
	public static final RegistryObject<LogisticsBlockGenericPipe>          pipe           = LPRegistries.PIPE;
	public static final RegistryObject<LogisticsBlockGenericSubMultiBlock> subMultiblock  = LPRegistries.SUB_MULTIBLOCK;

}
