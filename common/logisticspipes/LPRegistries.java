package logisticspipes;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import logisticspipes.proxy.TREnergyStorage;
import logisticspipes.proxy.interfaces.IEnergyStorage;

import team.reborn.energy.api.EnergyStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

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

	public static final java.util.List<RegistryObject<? extends Item>> ITEMS = new java.util.ArrayList<>();

	// 26.x item/block constructors need the registry key on their Properties; set while the factory runs.
	private static ResourceKey<Item> pendingItemKey;
	private static ResourceKey<Block> pendingBlockKey;

	public static Item.Properties itemProperties() {
		return new Item.Properties().setId(java.util.Objects.requireNonNull(pendingItemKey, "LP item constructed outside LPRegistries"));
	}

	public static Item.Properties blockItemProperties() {
		return itemProperties().useBlockDescriptionPrefix();
	}

	public static BlockBehaviour.Properties blockProperties() {
		return BlockBehaviour.Properties.of().setId(java.util.Objects.requireNonNull(pendingBlockKey, "LP block constructed outside LPRegistries"));
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, name);
	}

	private static <T> RegistryObject<T> register(Registry<? super T> registry, String name, java.util.function.Supplier<T> factory) {
		T value = factory.get();
		Registry.register(registry, id(name), value);
		return new RegistryObject<>(id(name), value);
	}

	public static <T extends Block> RegistryObject<T> registerBlock(String name, java.util.function.Supplier<T> factory) {
		pendingBlockKey = ResourceKey.create(Registries.BLOCK, id(name));
		try {
			return register(BuiltInRegistries.BLOCK, name, factory);
		} finally {
			pendingBlockKey = null;
		}
	}

	public static <T extends Item> RegistryObject<T> registerItem(String name, java.util.function.Supplier<T> factory) {
		pendingItemKey = ResourceKey.create(Registries.ITEM, id(name));
		try {
			RegistryObject<T> entry = register(BuiltInRegistries.ITEM, name, factory);
			ITEMS.add(entry);
			return entry;
		} finally {
			pendingItemKey = null;
		}
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(
			String name, BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
		return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, () -> new BlockEntityType<>(factory, java.util.Set.of(blocks)));
	}

	public static final RegistryObject<RecipeSerializer<logisticspipes.recipes.ShapelessResetRecipe>> RESET_RECIPE_SERIALIZER =
			register(BuiltInRegistries.RECIPE_SERIALIZER, "reset", () -> logisticspipes.recipes.ShapelessResetRecipe.SERIALIZER);
	public static final RegistryObject<RecipeSerializer<logisticspipes.recipes.OrdererDyeRecipe>> ORDERER_DYE_RECIPE_SERIALIZER =
			register(BuiltInRegistries.RECIPE_SERIALIZER, "orderer_dye", () -> logisticspipes.recipes.OrdererDyeRecipe.SERIALIZER);

	// ── Creative tab ─────────────────────────────────────────────────────────

	public static final RegistryObject<CreativeModeTab> LP_TAB = register(BuiltInRegistries.CREATIVE_MODE_TAB, "logistics_pipes", () ->
			FabricCreativeModeTab.builder()
					.icon(() -> new ItemStack(LPRegistries.PIPE_BASIC.get()))
					.title(Component.translatable("itemGroup.logisticspipes"))
					.displayItems((params, output) -> {
						// Exclude BC (MJ) and IC2 (EU) power items — those mods are not ported to 1.20.1.
						java.util.Set<String> hidden = java.util.Set.of(
								"power_provider_eu", "power_provider_mj",
								"power_supplier_mj",
								"power_supplier_eu_lv", "power_supplier_eu_mv",
								"power_supplier_eu_hv", "power_supplier_eu_ev");
						ITEMS.stream()
								.filter(reg -> !hidden.contains(reg.getId().getPath()))
								.forEach(reg -> {
									output.accept(new ItemStack(reg.get()));
									// LP1 listed all 17 orderer colours
									if (reg.get() instanceof RemoteOrderer) {
										for (int v = 1; v <= 16; v++) {
											ItemStack coloured = new ItemStack(reg.get());
											final int variant = v;
											CustomData.update(DataComponents.CUSTOM_DATA, coloured, tag -> tag.putInt(logisticspipes.recipes.OrdererDyeRecipe.COLOR_TAG, variant));
											output.accept(coloured);
										}
									}
								});
					})
					.build());

	// ── Blocks ────────────────────────────────────────────────────────────────

	public static final RegistryObject<LogisticsSolidBlock>           FRAME            = registerBlock("frame",            () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_JUNCTION   = registerBlock("power_junction",   () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_POWER_JUNCTION));
	public static final RegistryObject<LogisticsSolidBlock>           SECURITY_STATION = registerBlock("security_station", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_SECURITY_STATION));
	public static final RegistryObject<LogisticsSolidBlock>           CRAFTER          = registerBlock("crafting_table",   () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_AUTOCRAFTING_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           CRAFTER_FUZZY    = registerBlock("crafting_table_fuzzy", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_FUZZYCRAFTING_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           STATISTICS_TABLE = registerBlock("statistics_table", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_STATISTICS_TABLE));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_RF   = registerBlock("power_provider_rf",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_RF_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_EU   = registerBlock("power_provider_eu",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_IC2_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           POWER_PROVIDER_MJ   = registerBlock("power_provider_mj",  () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BC_POWERPROVIDER));
	public static final RegistryObject<LogisticsSolidBlock>           PROGRAM_COMPILER = registerBlock("program_compiler", () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_PROGRAM_COMPILER));
	public static final RegistryObject<logisticspipes.blocks.powertile.CreativePowerSourceBlock> CREATIVE_POWER_SOURCE = registerBlock("creative_power_source", logisticspipes.blocks.powertile.CreativePowerSourceBlock::new);
	public static final RegistryObject<LogisticsBlockGenericPipe>     PIPE             = registerBlock("pipe",             LogisticsBlockGenericPipe::new);
	public static final RegistryObject<LogisticsBlockGenericSubMultiBlock> SUB_MULTIBLOCK = registerBlock("sub_multiblock", LogisticsBlockGenericSubMultiBlock::new);

	// ── Items — misc ──────────────────────────────────────────────────────────

	public static final RegistryObject<LogisticsItemCard>          ITEM_CARD            = registerItem("item_card",            LogisticsItemCard::new);
	public static final RegistryObject<RemoteOrderer>              REMOTE_ORDERER       = registerItem("remote_orderer",       RemoteOrderer::new);
	public static final RegistryObject<ItemPipeSignCreator>        SIGN_CREATOR         = registerItem("sign_creator",         ItemPipeSignCreator::new);
	public static final RegistryObject<ItemHUDArmor>               HUD_GLASSES          = registerItem("hud_glasses",          ItemHUDArmor::new);
	public static final RegistryObject<ItemParts>                  PARTS                = registerItem("parts",                ItemParts::new);
	public static final RegistryObject<ItemBlankModule>            MODULE_BLANK         = registerItem("module_blank",         ItemBlankModule::new);
	public static final RegistryObject<ItemDisk>                   DISK                 = registerItem("disk",                 ItemDisk::new);
	public static final RegistryObject<LogisticsFluidContainer>    FLUID_CONTAINER      = registerItem("fluid_container",      LogisticsFluidContainer::new);
	public static final RegistryObject<LogisticsBrokenItem>        BROKEN_ITEM          = registerItem("broken_item",          LogisticsBrokenItem::new);
	public static final RegistryObject<ItemGuideBook>              GUIDE_BOOK           = registerItem("guide_book",           ItemGuideBook::new);
	public static final RegistryObject<ItemPipeController>         PIPE_CONTROLLER      = registerItem("pipe_controller",      ItemPipeController::new);
	public static final RegistryObject<ItemPipeManager>            PIPE_MANAGER         = registerItem("pipe_manager",         ItemPipeManager::new);
	public static final RegistryObject<ItemLogisticsProgrammer>    LOGISTICS_PROGRAMMER = registerItem("logistics_programmer", ItemLogisticsProgrammer::new);
	public static final RegistryObject<ItemLogisticsChips>         CHIP_BASIC           = registerItem("chip_basic",           () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_BASIC_RAW       = registerItem("chip_basic_raw",       () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC_RAW));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_ADVANCED        = registerItem("chip_advanced",        () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_ADVANCED_RAW    = registerItem("chip_advanced_raw",    () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED_RAW));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_FPGA            = registerItem("chip_fpga",            () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA));
	public static final RegistryObject<ItemLogisticsChips>         CHIP_FPGA_RAW        = registerItem("chip_fpga_raw",        () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA_RAW));

	// Items — block items (backed by their block)
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_FRAME             = registerItem("frame",             () -> new LogisticsSolidBlockItem(FRAME.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_JUNCTION    = registerItem("power_junction",    () -> new LogisticsSolidBlockItem(POWER_JUNCTION.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_SECURITY_STATION  = registerItem("security_station",  () -> new LogisticsSolidBlockItem(SECURITY_STATION.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_CRAFTER           = registerItem("crafting_table",    () -> new LogisticsSolidBlockItem(CRAFTER.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_CRAFTER_FUZZY     = registerItem("crafting_table_fuzzy", () -> new LogisticsSolidBlockItem(CRAFTER_FUZZY.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_STATISTICS_TABLE  = registerItem("statistics_table",  () -> new LogisticsSolidBlockItem(STATISTICS_TABLE.get()));
	public static final RegistryObject<Item> ITEM_CREATIVE_POWER_SOURCE = registerItem("creative_power_source", () -> new BlockItem(CREATIVE_POWER_SOURCE.get(), blockItemProperties()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_RF = registerItem("power_provider_rf", () -> new LogisticsSolidBlockItem(POWER_PROVIDER_RF.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_EU = registerItem("power_provider_eu", () -> new LogisticsSolidBlockItem(POWER_PROVIDER_EU.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_MJ = registerItem("power_provider_mj", () -> new LogisticsSolidBlockItem(POWER_PROVIDER_MJ.get()));
	public static final RegistryObject<LogisticsSolidBlockItem> ITEM_PROGRAM_COMPILER  = registerItem("program_compiler",  () -> new LogisticsSolidBlockItem(PROGRAM_COMPILER.get()));

	// Items — item pipes (declared before modules/upgrades to avoid circular static-init:
	// registerModule/registerUpgrade call LPItems.modules/upgrades.put() which triggers LPItems
	// class loading; LPItems copies these RegistryObject refs at init time, so they must be
	// set in LPRegistries before the first registerModule/registerUpgrade call executes.)
	public static final RegistryObject<ItemLogisticsPipe> PIPE_BASIC              = LogisticsBlockGenericPipe.registerPipe("basic",                   PipeItemsBasicLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST            = LogisticsBlockGenericPipe.registerPipe("request",                 PipeItemsRequestLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST_MK2        = LogisticsBlockGenericPipe.registerPipe("request_mk2",             PipeItemsRequestLogisticsMk2::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_PROVIDER           = LogisticsBlockGenericPipe.registerPipe("provider",                PipeItemsProviderLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CRAFTING           = LogisticsBlockGenericPipe.registerPipe("crafting",                PipeItemsCraftingLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SATELLITE          = LogisticsBlockGenericPipe.registerPipe("satellite",               PipeItemsSatelliteLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SUPPLIER           = LogisticsBlockGenericPipe.registerPipe("supplier",                PipeItemsSupplierLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK1        = LogisticsBlockGenericPipe.registerPipe("chassis_mk1",             PipeLogisticsChassisMk1::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK2        = LogisticsBlockGenericPipe.registerPipe("chassis_mk2",             PipeLogisticsChassisMk2::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK3        = LogisticsBlockGenericPipe.registerPipe("chassis_mk3",             PipeLogisticsChassisMk3::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK4        = LogisticsBlockGenericPipe.registerPipe("chassis_mk4",             PipeLogisticsChassisMk4::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_CHASSIS_MK5        = LogisticsBlockGenericPipe.registerPipe("chassis_mk5",             PipeLogisticsChassisMk5::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REMOTE_ORDERER     = LogisticsBlockGenericPipe.registerPipe("remote_orderer",          PipeItemsRemoteOrdererLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_INV_SYS_CONNECTOR  = LogisticsBlockGenericPipe.registerPipe("inventory_system_connector", PipeItemsInvSysConnector::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SYSTEM_ENTRANCE    = LogisticsBlockGenericPipe.registerPipe("system_entrance",         PipeItemsSystemEntranceLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_SYSTEM_DESTINATION = LogisticsBlockGenericPipe.registerPipe("system_destination",      PipeItemsSystemDestinationLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FIREWALL           = LogisticsBlockGenericPipe.registerPipe("firewall",                PipeItemsFirewall::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_REQUEST_TABLE      = LogisticsBlockGenericPipe.registerPipe("request_table",           PipeBlockRequestTable::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_UNROUTED           = LogisticsBlockGenericPipe.registerPipe("transport_basic",         PipeItemsBasicTransport::new);
	// Fluid pipes
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_BASIC        = LogisticsBlockGenericPipe.registerPipe("fluid_basic",             logisticspipes.pipes.PipeFluidBasic::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER     = LogisticsBlockGenericPipe.registerPipe("fluid_supplier",          PipeItemsFluidSupplier::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_TERMINUS     = LogisticsBlockGenericPipe.registerPipe("fluid_terminus",          logisticspipes.pipes.PipeFluidTerminus::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_INSERTION    = LogisticsBlockGenericPipe.registerPipe("fluid_insertion",         PipeFluidInsertion::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_PROVIDER     = LogisticsBlockGenericPipe.registerPipe("fluid_provider",          PipeFluidProvider::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_REQUEST      = LogisticsBlockGenericPipe.registerPipe("fluid_request",           PipeFluidRequestLogistics::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_EXTRACTOR    = LogisticsBlockGenericPipe.registerPipe("fluid_extractor",         PipeFluidExtractor::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SATELLITE    = LogisticsBlockGenericPipe.registerPipe("fluid_satellite",         PipeFluidSatellite::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER_MK2 = LogisticsBlockGenericPipe.registerPipe("fluid_supplier_mk2",      PipeFluidSupplierMk2::new);
	// High-speed tubes
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_CURVE   = LogisticsBlockGenericPipe.registerPipe("hs_curve",   HSTubeCurve::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_SPEEDUP = LogisticsBlockGenericPipe.registerPipe("hs_speedup", HSTubeSpeedup::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_S_CURVE = LogisticsBlockGenericPipe.registerPipe("hs_s_curve", HSTubeSCurve::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_LINE    = LogisticsBlockGenericPipe.registerPipe("hs_line",    HSTubeLine::new);
	public static final RegistryObject<ItemLogisticsPipe> PIPE_HS_GAIN    = LogisticsBlockGenericPipe.registerPipe("hs_gain",    HSTubeGain::new);

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
		LPItems.modules.put(name, id(regName));
		return registerItem(regName, () -> ItemModule.of(ctor));
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
		LPItems.upgrades.put(name, id(regName));
		return registerItem(regName, () -> ItemUpgrade.of(ctor));
	}

	// ── Block Entities ────────────────────────────────────────────────────────

	public static final RegistryObject<BlockEntityType<LogisticsPowerJunctionTileEntity>>     BE_POWER_JUNCTION    = registerBlockEntity("power_junction", LogisticsPowerJunctionTileEntity::new, POWER_JUNCTION.get());
	public static final RegistryObject<BlockEntityType<LogisticsRFPowerProviderTileEntity>>   BE_POWER_PROVIDER_RF = registerBlockEntity("power_provider_rf", LogisticsRFPowerProviderTileEntity::new, POWER_PROVIDER_RF.get());
	public static final RegistryObject<BlockEntityType<LogisticsIC2PowerProviderTileEntity>>  BE_POWER_PROVIDER_EU = registerBlockEntity("power_provider_ic2", LogisticsIC2PowerProviderTileEntity::new, POWER_PROVIDER_EU.get());
	public static final RegistryObject<BlockEntityType<LogisticsSecurityTileEntity>>          BE_SECURITY_STATION  = registerBlockEntity("security_station", LogisticsSecurityTileEntity::new, SECURITY_STATION.get());
	public static final RegistryObject<BlockEntityType<LogisticsCraftingTableTileEntity>>     BE_CRAFTING_TABLE    = registerBlockEntity("logistics_crafting_table", LogisticsCraftingTableTileEntity::new, CRAFTER.get(), CRAFTER_FUZZY.get());
	public static final RegistryObject<BlockEntityType<logisticspipes.blocks.powertile.CreativePowerSourceTileEntity>> BE_CREATIVE_POWER_SOURCE = registerBlockEntity("creative_power_source", logisticspipes.blocks.powertile.CreativePowerSourceTileEntity::new, CREATIVE_POWER_SOURCE.get());
	public static final RegistryObject<BlockEntityType<LogisticsTileGenericPipe>>             BE_PIPE              = registerBlockEntity("pipe", LogisticsTileGenericPipe::new, PIPE.get());
	public static final RegistryObject<BlockEntityType<LogisticsTileGenericSubMultiBlock>>    BE_SUB_PIPE          = registerBlockEntity("sub_pipe", LogisticsTileGenericSubMultiBlock::new, SUB_MULTIBLOCK.get());
	public static final RegistryObject<BlockEntityType<LogisticsStatisticsTileEntity>>        BE_STATISTICS_TABLE  = registerBlockEntity("statistics_table", LogisticsStatisticsTileEntity::new, STATISTICS_TABLE.get());
	public static final RegistryObject<BlockEntityType<LogisticsProgramCompilerTileEntity>>   BE_PROGRAM_COMPILER  = registerBlockEntity("program_compiler", LogisticsProgramCompilerTileEntity::new, PROGRAM_COMPILER.get());
	public static final RegistryObject<BlockEntityType<LogisticsFrameTileEntity>>             BE_FRAME             = registerBlockEntity("frame", LogisticsFrameTileEntity::new, FRAME.get());

	/** Touching this class registers everything; call from the main entrypoint. */
	public static void register() {
		ItemStorage.SIDED.registerForBlockEntity((pipe, side) -> pipe.getItemHandlerForSide(side), BE_PIPE.get());
		FluidStorage.SIDED.registerForBlockEntity((pipe, side) -> pipe.getFluidStorageForSide(side), BE_PIPE.get());
		IEnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyInterface(), BE_POWER_JUNCTION.get());
		IEnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyInterface(), BE_POWER_PROVIDER_RF.get());
		IEnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile, BE_CREATIVE_POWER_SOURCE.get());
		EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getTREnergy(), BE_POWER_JUNCTION.get());
		EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getTREnergy(), BE_POWER_PROVIDER_RF.get());
		EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getTREnergy(), BE_POWER_PROVIDER_EU.get());
		EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> new TREnergyStorage(tile), BE_CREATIVE_POWER_SOURCE.get());
		logisticspipes.utils.CraftingUtil.synchronizeRecipes();
	}
}
