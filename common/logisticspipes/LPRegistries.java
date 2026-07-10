package logisticspipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

import net.minecraft.world.item.crafting.RecipeSerializer;

import net.neoforged.bus.api.IEventBus;

import logisticspipes.blocks.LogisticsFrameTileEntity;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsIC2PowerProviderTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.items.ItemBlankModule;
import logisticspipes.items.ItemDisk;
import logisticspipes.items.ItemHUDArmor;
import logisticspipes.items.ItemLogisticsChips;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemParts;
import logisticspipes.items.ItemPipeController;
import logisticspipes.items.ItemPipeManager;
import logisticspipes.items.ItemPipeSignCreator;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsBrokenItem;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.items.LogisticsSolidBlockItem;
import logisticspipes.items.RemoteOrderer;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleEnchantmentSink;
import logisticspipes.modules.ModuleEnchantmentSinkMK2;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.upgrades.ActionSpeedUpgrade;
import logisticspipes.pipes.upgrades.AdvancedSatelliteUpgrade;
import logisticspipes.pipes.upgrades.CCRemoteControlUpgrade;
import logisticspipes.pipes.upgrades.CombinedSneakyUpgrade;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.pipes.upgrades.CraftingMonitoringUpgrade;
import logisticspipes.pipes.upgrades.FluidCraftingUpgrade;
import logisticspipes.pipes.upgrades.FuzzyUpgrade;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import logisticspipes.pipes.upgrades.LogicControllerUpgrade;
import logisticspipes.pipes.upgrades.OpaqueUpgrade;
import logisticspipes.pipes.upgrades.PatternUpgrade;
import logisticspipes.pipes.upgrades.PowerTransportationUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.pipes.upgrades.SpeedUpgrade;
import logisticspipes.pipes.upgrades.UpgradeModuleUpgrade;
import logisticspipes.pipes.upgrades.power.BCPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2EVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2HVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2LVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2MVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.RFPowerSupplierUpgrade;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.module.AsyncExtractorModule;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeFluidExtractor;
import logisticspipes.pipes.PipeFluidInsertion;
import logisticspipes.pipes.PipeFluidProvider;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.pipes.PipeLogisticsChassisMk1;
import logisticspipes.pipes.PipeLogisticsChassisMk2;
import logisticspipes.pipes.PipeLogisticsChassisMk3;
import logisticspipes.pipes.PipeLogisticsChassisMk4;
import logisticspipes.pipes.PipeLogisticsChassisMk5;
import logisticspipes.pipes.tubes.HSTubeCurve;
import logisticspipes.pipes.tubes.HSTubeGain;
import logisticspipes.pipes.tubes.HSTubeLine;
import logisticspipes.pipes.tubes.HSTubeSCurve;
import logisticspipes.pipes.tubes.HSTubeSpeedup;
import logisticspipes.pipes.unrouted.PipeItemsBasicTransport;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;

public final class LPRegistries {

	private LPRegistries() {}

	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(Registries.BLOCK, LPConstants.LP_MOD_ID);

	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(Registries.ITEM, LPConstants.LP_MOD_ID);

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LPConstants.LP_MOD_ID);

	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LPConstants.LP_MOD_ID);

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, LPConstants.LP_MOD_ID);

	public static final RegistryObject<RecipeSerializer<logisticspipes.recipes.ShapelessResetRecipe>> RESET_RECIPE_SERIALIZER =
			RECIPE_SERIALIZERS.register("reset", () -> logisticspipes.recipes.ShapelessResetRecipe.SERIALIZER);

	// ── Creative tab ─────────────────────────────────────────────────────────

	public static final RegistryObject<CreativeModeTab> LP_TAB = CREATIVE_TABS.register("logistics_pipes", () ->
			CreativeModeTab.builder()
					.icon(() -> new ItemStack(LPRegistries.PIPE_BASIC.get()))
					.title(Component.translatable("itemGroup.logisticspipes"))
					.displayItems((params, output) -> {
						// Exclude BC (MJ) and IC2 (EU) power items — those mods are not ported to 1.20.1.
						java.util.Set<String> hidden = java.util.Set.of(
								"power_provider_eu", "power_provider_mj",
								"power_supplier_mj",
								"power_supplier_eu_lv", "power_supplier_eu_mv",
								"power_supplier_eu_hv", "power_supplier_eu_ev");
						ITEMS.getEntries().stream()
								.filter(reg -> !hidden.contains(reg.getId().getPath()))
								.forEach(reg -> output.accept(new ItemStack(reg.get())));
					})
					.build());

	// ── Blocks ────────────────────────────────────────────────────────────────

	public static final RegistryObject<LogisticsSolidBlock>           FRAME            = BLOCKS.register("frame",            () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_JUNCTION   = BLOCKS.register("power_junction",   () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_POWER_JUNCTION));
	public static final RegistryObject<LogisticsSolidBlock>           SECURITY_STATION = BLOCKS.register("security_station", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_SECURITY_STATION));
	public static final RegistryObject<LogisticsSolidBlock>           CRAFTER          = BLOCKS.register("crafting_table",   () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_AUTOCRAFTING_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           CRAFTER_FUZZY    = BLOCKS.register("crafting_table_fuzzy", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_FUZZYCRAFTING_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           STATISTICS_TABLE = BLOCKS.register("statistics_table", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_STATISTICS_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_RF   = BLOCKS.register("power_provider_rf",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_RF_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_EU   = BLOCKS.register("power_provider_eu",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_IC2_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_MJ   = BLOCKS.register("power_provider_mj",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BC_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           PROGRAM_COMPILER = BLOCKS.register("program_compiler", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_PROGRAM_COMPILER));
	public static final RegistryObject<LogisticsBlockGenericPipe>     PIPE             = BLOCKS.register("pipe",             LogisticsBlockGenericPipe::new);
	public static final RegistryObject<LogisticsBlockGenericSubMultiBlock> SUB_MULTIBLOCK = BLOCKS.register("sub_multiblock", LogisticsBlockGenericSubMultiBlock::new);

	// ── Items — misc ──────────────────────────────────────────────────────────

	public static final RegistryObject<LogisticsItemCard>          ITEM_CARD            = ITEMS.register("item_card",            LogisticsItemCard::new);
	public static final RegistryObject<RemoteOrderer>              REMOTE_ORDERER       = ITEMS.register("remote_orderer",       RemoteOrderer::new);
	public static final RegistryObject<ItemPipeSignCreator>        SIGN_CREATOR         = ITEMS.register("sign_creator",         ItemPipeSignCreator::new);
	public static final RegistryObject<ItemHUDArmor>               HUD_GLASSES          = ITEMS.register("hud_glasses",          ItemHUDArmor::new);
	public static final RegistryObject<ItemParts>                  PARTS                = ITEMS.register("parts",                ItemParts::new);
	public static final RegistryObject<ItemBlankModule>            MODULE_BLANK         = ITEMS.register("module_blank",         ItemBlankModule::new);
	public static final RegistryObject<ItemDisk>                   DISK                 = ITEMS.register("disk",                 ItemDisk::new);
	public static final RegistryObject<LogisticsFluidContainer>    FLUID_CONTAINER      = ITEMS.register("fluid_container",      LogisticsFluidContainer::new);
	public static final RegistryObject<LogisticsBrokenItem>        BROKEN_ITEM          = ITEMS.register("broken_item",          LogisticsBrokenItem::new);
	public static final RegistryObject<ItemGuideBook>              GUIDE_BOOK           = ITEMS.register("guide_book",           ItemGuideBook::new);
	public static final RegistryObject<ItemPipeController>         PIPE_CONTROLLER      = ITEMS.register("pipe_controller",      ItemPipeController::new);
	public static final RegistryObject<ItemPipeManager>            PIPE_MANAGER         = ITEMS.register("pipe_manager",         ItemPipeManager::new);
	public static final RegistryObject<ItemLogisticsProgrammer>    LOGISTICS_PROGRAMMER = ITEMS.register("logistics_programmer", ItemLogisticsProgrammer::new);
	public static final RegistryObject<ItemLogisticsChips>         CHIP_BASIC           = ITEMS.register("chip_basic",           () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_BASIC_RAW       = ITEMS.register("chip_basic_raw",       () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC_RAW));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_ADVANCED        = ITEMS.register("chip_advanced",        () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_ADVANCED_RAW    = ITEMS.register("chip_advanced_raw",    () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED_RAW));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_FPGA            = ITEMS.register("chip_fpga",            () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_FPGA_RAW        = ITEMS.register("chip_fpga_raw",        () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA_RAW));

	// Items — block items (backed by their block)
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_FRAME             = ITEMS.register("frame",             () -> new LogisticsSolidBlockItem(LPBlocks.frame.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_JUNCTION    = ITEMS.register("power_junction",    () -> new LogisticsSolidBlockItem(LPBlocks.powerJunction.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_SECURITY_STATION  = ITEMS.register("security_station",  () -> new LogisticsSolidBlockItem(LPBlocks.securityStation.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_CRAFTER           = ITEMS.register("crafting_table",    () -> new LogisticsSolidBlockItem(LPBlocks.crafter.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_CRAFTER_FUZZY     = ITEMS.register("crafting_table_fuzzy", () -> new LogisticsSolidBlockItem(LPBlocks.crafterFuzzy.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_STATISTICS_TABLE  = ITEMS.register("statistics_table",  () -> new LogisticsSolidBlockItem(LPBlocks.statisticsTable.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_RF = ITEMS.register("power_provider_rf", () -> new LogisticsSolidBlockItem(LPBlocks.powerProviderRF.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_EU = ITEMS.register("power_provider_eu", () -> new LogisticsSolidBlockItem(LPBlocks.powerProviderEU.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_MJ = ITEMS.register("power_provider_mj", () -> new LogisticsSolidBlockItem(LPBlocks.powerProviderMJ.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_PROGRAM_COMPILER  = ITEMS.register("program_compiler",  () -> new LogisticsSolidBlockItem(LPBlocks.programCompiler.get()));

	// Items — item pipes (declared before modules/upgrades to avoid circular static-init:
	// registerModule/registerUpgrade call LPItems.modules/upgrades.put() which triggers LPItems
	// class loading; LPItems copies these RegistryObject refs at init time, so they must be
	// set in LPRegistries before the first registerModule/registerUpgrade call executes.)
	public static final RegistryObject<ItemLogisticsPipe> PIPE_BASIC              = LogisticsBlockGenericPipe.registerPipe(ITEMS, "basic",                   PipeItemsBasicLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST            = LogisticsBlockGenericPipe.registerPipe(ITEMS, "request",                 PipeItemsRequestLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST_MK2        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "request_mk2",             PipeItemsRequestLogisticsMk2::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_PROVIDER           = LogisticsBlockGenericPipe.registerPipe(ITEMS, "provider",                PipeItemsProviderLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CRAFTING           = LogisticsBlockGenericPipe.registerPipe(ITEMS, "crafting",                PipeItemsCraftingLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SATELLITE          = LogisticsBlockGenericPipe.registerPipe(ITEMS, "satellite",               PipeItemsSatelliteLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SUPPLIER           = LogisticsBlockGenericPipe.registerPipe(ITEMS, "supplier",                PipeItemsSupplierLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK1        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "chassis_mk1",             PipeLogisticsChassisMk1::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK2        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "chassis_mk2",             PipeLogisticsChassisMk2::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK3        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "chassis_mk3",             PipeLogisticsChassisMk3::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK4        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "chassis_mk4",             PipeLogisticsChassisMk4::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK5        = LogisticsBlockGenericPipe.registerPipe(ITEMS, "chassis_mk5",             PipeLogisticsChassisMk5::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REMOTE_ORDERER     = LogisticsBlockGenericPipe.registerPipe(ITEMS, "remote_orderer",          PipeItemsRemoteOrdererLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_INV_SYS_CONNECTOR  = LogisticsBlockGenericPipe.registerPipe(ITEMS, "inventory_system_connector", PipeItemsInvSysConnector::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SYSTEM_ENTRANCE    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "system_entrance",         PipeItemsSystemEntranceLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SYSTEM_DESTINATION = LogisticsBlockGenericPipe.registerPipe(ITEMS, "system_destination",      PipeItemsSystemDestinationLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FIREWALL           = LogisticsBlockGenericPipe.registerPipe(ITEMS, "firewall",                PipeItemsFirewall::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST_TABLE      = LogisticsBlockGenericPipe.registerPipe(ITEMS, "request_table",           PipeBlockRequestTable::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_UNROUTED           = LogisticsBlockGenericPipe.registerPipe(ITEMS, "transport_basic",         PipeItemsBasicTransport::new);
	// Fluid pipes
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER     = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_supplier",          PipeItemsFluidSupplier::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_INSERTION    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_insertion",         PipeFluidInsertion::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_PROVIDER     = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_provider",          PipeFluidProvider::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_REQUEST      = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_request",           PipeFluidRequestLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_EXTRACTOR    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_extractor",         PipeFluidExtractor::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SATELLITE    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_satellite",         PipeFluidSatellite::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER_MK2 = LogisticsBlockGenericPipe.registerPipe(ITEMS, "fluid_supplier_mk2",      PipeFluidSupplierMk2::new);
	// High-speed tubes
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_CURVE   = LogisticsBlockGenericPipe.registerPipe(ITEMS, "hs_curve",   HSTubeCurve::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_SPEEDUP = LogisticsBlockGenericPipe.registerPipe(ITEMS, "hs_speedup", HSTubeSpeedup::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_S_CURVE = LogisticsBlockGenericPipe.registerPipe(ITEMS, "hs_s_curve", HSTubeSCurve::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_LINE    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "hs_line",    HSTubeLine::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_GAIN    = LogisticsBlockGenericPipe.registerPipe(ITEMS, "hs_gain",    HSTubeGain::new);

	// ── Modules ───────────────────────────────────────────────────────────────

	public static final RegistryObject<ItemModule> MODULE_ITEM_SINK           = registerModule(ModuleItemSink.getName(),                ModuleItemSink::new);
	public static final RegistryObject<ItemModule> MODULE_PASSIVE_SUPPLIER    = registerModule(ModulePassiveSupplier.getName(),         ModulePassiveSupplier::new);
	public static final RegistryObject<ItemModule> MODULE_EXTRACTOR           = registerModule(AsyncExtractorModule.getName(),          AsyncExtractorModule::new);
	public static final RegistryObject<ItemModule> MODULE_POLYMORPHIC_SINK    = registerModule(ModulePolymorphicItemSink.getName(),     ModulePolymorphicItemSink::new);
	public static final RegistryObject<ItemModule> MODULE_QUICKSORT           = registerModule(AsyncQuicksortModule.getName(),          AsyncQuicksortModule::new);
	public static final RegistryObject<ItemModule> MODULE_TERMINUS            = registerModule(ModuleTerminus.getName(),                ModuleTerminus::new);
	public static final RegistryObject<ItemModule> MODULE_EXTRACTOR_ADVANCED  = registerModule(AsyncAdvancedExtractor.getName(),        AsyncAdvancedExtractor::new);
	public static final RegistryObject<ItemModule> MODULE_PROVIDER            = registerModule(ModuleProvider.getName(),                ModuleProvider::new);
	public static final RegistryObject<ItemModule> MODULE_MOD_SINK            = registerModule(ModuleModBasedItemSink.getName(),        ModuleModBasedItemSink::new);
	public static final RegistryObject<ItemModule> MODULE_OREDICT_SINK        = registerModule(ModuleOreDictItemSink.getName(),         ModuleOreDictItemSink::new);
	public static final RegistryObject<ItemModule> MODULE_ENCHANTMENT_SINK    = registerModule(ModuleEnchantmentSink.getName(),         ModuleEnchantmentSink::new);
	public static final RegistryObject<ItemModule> MODULE_ENCHANTMENT_SINK_MK2 = registerModule(ModuleEnchantmentSinkMK2.getName(),    ModuleEnchantmentSinkMK2::new);
	public static final RegistryObject<ItemModule> MODULE_CRAFTER             = registerModule(ModuleCrafter.getName(),                ModuleCrafter::new);
	public static final RegistryObject<ItemModule> MODULE_ACTIVE_SUPPLIER     = registerModule(ModuleActiveSupplier.getName(),         ModuleActiveSupplier::new);
	public static final RegistryObject<ItemModule> MODULE_CREATIVETAB_SINK    = registerModule(ModuleCreativeTabBasedItemSink.getName(), ModuleCreativeTabBasedItemSink::new);

	private static RegistryObject<ItemModule> registerModule(String name, java.util.function.Supplier<? extends logisticspipes.modules.LogisticsModule> ctor) {
		String regName = "module_" + name;
		LPItems.modules.put(name, new net.minecraft.resources.ResourceLocation(LPConstants.LP_MOD_ID, regName));
		return ITEMS.register(regName, () -> ItemModule.of(ctor));
	}

	// ── Upgrades ──────────────────────────────────────────────────────────────

	public static final RegistryObject<ItemUpgrade> UPGRADE_SNEAKY_COMBINATION  = registerUpgrade(CombinedSneakyUpgrade.getName(),      CombinedSneakyUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_SNEAKY              = registerUpgrade(SneakyUpgradeConfig.getName(),         SneakyUpgradeConfig::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_SPEED               = registerUpgrade(SpeedUpgrade.getName(),               SpeedUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_DISCONNECTION       = registerUpgrade(ConnectionUpgradeConfig.getName(),     ConnectionUpgradeConfig::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_SATELLITE_ADVANCED  = registerUpgrade(AdvancedSatelliteUpgrade.getName(),    AdvancedSatelliteUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_FLUID_CRAFTING      = registerUpgrade(FluidCraftingUpgrade.getName(),        FluidCraftingUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_CRAFTING_BYPRODUCT  = registerUpgrade(CraftingByproductUpgrade.getName(),    CraftingByproductUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_PATTERN             = registerUpgrade(PatternUpgrade.getName(),              PatternUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_FUZZY               = registerUpgrade(FuzzyUpgrade.getName(),               FuzzyUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_TRANSPORTATION = registerUpgrade(PowerTransportationUpgrade.getName(), PowerTransportationUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_MJ            = registerUpgrade(BCPowerSupplierUpgrade.getName(),      BCPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_RF            = registerUpgrade(RFPowerSupplierUpgrade.getName(),      RFPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_EU_LV         = registerUpgrade(IC2LVPowerSupplierUpgrade.getName(),   IC2LVPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_EU_MV         = registerUpgrade(IC2MVPowerSupplierUpgrade.getName(),   IC2MVPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_EU_HV         = registerUpgrade(IC2HVPowerSupplierUpgrade.getName(),   IC2HVPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_POWER_EU_EV         = registerUpgrade(IC2EVPowerSupplierUpgrade.getName(),   IC2EVPowerSupplierUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_CC_REMOTE_CONTROL   = registerUpgrade(CCRemoteControlUpgrade.getName(),      CCRemoteControlUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_CRAFTING_MONITORING  = registerUpgrade(CraftingMonitoringUpgrade.getName(),  CraftingMonitoringUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_OPAQUE              = registerUpgrade(OpaqueUpgrade.getName(),              OpaqueUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_CRAFTING_CLEANUP    = registerUpgrade(CraftingCleanupUpgrade.getName(),      CraftingCleanupUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_LOGIC_CONTROLLER    = registerUpgrade(LogicControllerUpgrade.getName(),      LogicControllerUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_MODULE_UPGRADE      = registerUpgrade(UpgradeModuleUpgrade.getName(),        UpgradeModuleUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_ACTION_SPEED        = registerUpgrade(ActionSpeedUpgrade.getName(),          ActionSpeedUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_ITEM_EXTRACTION     = registerUpgrade(ItemExtractionUpgrade.getName(),       ItemExtractionUpgrade::new);
	public static final RegistryObject<ItemUpgrade> UPGRADE_ITEM_STACK_EXTRACTION = registerUpgrade(ItemStackExtractionUpgrade.getName(), ItemStackExtractionUpgrade::new);

	private static RegistryObject<ItemUpgrade> registerUpgrade(String name, java.util.function.Supplier<? extends logisticspipes.pipes.upgrades.IPipeUpgrade> ctor) {
		String regName = "upgrade_" + name;
		LPItems.upgrades.put(name, new net.minecraft.resources.ResourceLocation(LPConstants.LP_MOD_ID, regName));
		return ITEMS.register(regName, () -> ItemUpgrade.of(ctor));
	}

	// ── Block Entities ────────────────────────────────────────────────────────
	// NOTE: BlockEntity constructors must be migrated to (BlockPos, BlockState) before
	// these suppliers will compile. Stubs use placeholder suppliers for now.

	public static final RegistryObject<BlockEntityType<LogisticsPowerJunctionTileEntity>>     BE_POWER_JUNCTION    = BLOCK_ENTITIES.register("power_junction",
			() -> BlockEntityType.Builder.of(LogisticsPowerJunctionTileEntity::new,    LPBlocks.powerJunction.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsRFPowerProviderTileEntity>>   BE_POWER_PROVIDER_RF = BLOCK_ENTITIES.register("power_provider_rf",
			() -> BlockEntityType.Builder.of(LogisticsRFPowerProviderTileEntity::new,  LPBlocks.powerProviderRF.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsIC2PowerProviderTileEntity>>  BE_POWER_PROVIDER_EU = BLOCK_ENTITIES.register("power_provider_ic2",
			() -> BlockEntityType.Builder.of(LogisticsIC2PowerProviderTileEntity::new, LPBlocks.powerProviderEU.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsSecurityTileEntity>>          BE_SECURITY_STATION  = BLOCK_ENTITIES.register("security_station",
			() -> BlockEntityType.Builder.of(LogisticsSecurityTileEntity::new,         LPBlocks.securityStation.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsCraftingTableTileEntity>>     BE_CRAFTING_TABLE    = BLOCK_ENTITIES.register("logistics_crafting_table",
			() -> BlockEntityType.Builder.of(LogisticsCraftingTableTileEntity::new,    LPBlocks.crafter.get(), LPBlocks.crafterFuzzy.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsTileGenericPipe>>             BE_PIPE              = BLOCK_ENTITIES.register("pipe",
			() -> BlockEntityType.Builder.of(LogisticsTileGenericPipe::new,            LPBlocks.pipe.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsTileGenericSubMultiBlock>>    BE_SUB_PIPE          = BLOCK_ENTITIES.register("sub_pipe",
			() -> BlockEntityType.Builder.of(LogisticsTileGenericSubMultiBlock::new,   LPBlocks.subMultiblock.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsStatisticsTileEntity>>        BE_STATISTICS_TABLE  = BLOCK_ENTITIES.register("statistics_table",
			() -> BlockEntityType.Builder.of(LogisticsStatisticsTileEntity::new,       LPBlocks.statisticsTable.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsProgramCompilerTileEntity>>   BE_PROGRAM_COMPILER  = BLOCK_ENTITIES.register("program_compiler",
			() -> BlockEntityType.Builder.of(LogisticsProgramCompilerTileEntity::new,  LPBlocks.programCompiler.get()).build(null));
	public static final RegistryObject<BlockEntityType<LogisticsFrameTileEntity>>             BE_FRAME             = BLOCK_ENTITIES.register("frame",
			() -> BlockEntityType.Builder.of(LogisticsFrameTileEntity::new,            LPBlocks.frame.get()).build(null));

	/** Call this from the mod constructor to register all deferred registers with the mod event bus. */
	public static void register(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);
		RECIPE_SERIALIZERS.register(modEventBus);
		// Capability exposure: each BlockEntity overrides getCapability() directly (NeoForge 47.x pattern).
		// LogisticsTileGenericPipe: ITEM_HANDLER + FLUID_HANDLER wired.
		// LogisticsRFPowerProviderTileEntity: ENERGY wired.
		// IC2/BC/MJ power providers: disabled (mods not ported to 1.20.1).
	}
}
