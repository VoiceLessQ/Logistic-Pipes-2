package logisticspipes;

import net.minecraft.world.item.Item;

import net.neoforged.neoforge.registries.DeferredHolder;

import logisticspipes.items.ItemLogisticsPipe;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import logisticspipes.items.ItemBlankModule;
import logisticspipes.items.ItemDisk;
import logisticspipes.items.ItemHUDArmor;
import logisticspipes.items.ItemLogisticsChips;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemPipeController;
import logisticspipes.items.LogisticsBrokenItem;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.items.RemoteOrderer;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;

/**
 * Holds DeferredHolder references to all registered LP items.
 * Access via .get() — e.g. LPItems.pipeBasic.get()
 */
public class LPItems {

	// Logistics Pipes
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeUnrouted             = LPRegistries.PIPE_UNROUTED;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeBasic                = LPRegistries.PIPE_BASIC;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeRequest              = LPRegistries.PIPE_REQUEST;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeRequestMk2           = LPRegistries.PIPE_REQUEST_MK2;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeProvider             = LPRegistries.PIPE_PROVIDER;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeCrafting             = LPRegistries.PIPE_CRAFTING;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeSatellite            = LPRegistries.PIPE_SATELLITE;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeSupplier             = LPRegistries.PIPE_SUPPLIER;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeChassisMk1           = LPRegistries.PIPE_CHASSIS_MK1;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeChassisMk2           = LPRegistries.PIPE_CHASSIS_MK2;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeChassisMk3           = LPRegistries.PIPE_CHASSIS_MK3;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeChassisMk4           = LPRegistries.PIPE_CHASSIS_MK4;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeChassisMk5           = LPRegistries.PIPE_CHASSIS_MK5;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeInvSystemConnector   = LPRegistries.PIPE_INV_SYS_CONNECTOR;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeSystemEntrance       = LPRegistries.PIPE_SYSTEM_ENTRANCE;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeSystemDestination    = LPRegistries.PIPE_SYSTEM_DESTINATION;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFirewall             = LPRegistries.PIPE_FIREWALL;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeRemoteOrderer        = LPRegistries.PIPE_REMOTE_ORDERER;
	public static final DeferredHolder<?, ItemLogisticsPipe> requestTable             = LPRegistries.PIPE_REQUEST_TABLE;

	// Logistics Fluid Pipes
	// NOTE: pipeFluidBasic and pipeFluidTerminus have no corresponding pipe class (removed upstream).
	public static final DeferredHolder<?, ? extends Item>    pipeFluidBasic           = null; // unregistered — PipeFluidBasic removed
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidRequest         = LPRegistries.PIPE_FLUID_REQUEST;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidProvider        = LPRegistries.PIPE_FLUID_PROVIDER;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidSatellite       = LPRegistries.PIPE_FLUID_SATELLITE;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidSupplier        = LPRegistries.PIPE_FLUID_SUPPLIER;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidSupplierMk2     = LPRegistries.PIPE_FLUID_SUPPLIER_MK2;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidInsertion       = LPRegistries.PIPE_FLUID_INSERTION;
	public static final DeferredHolder<?, ItemLogisticsPipe> pipeFluidExtractor       = LPRegistries.PIPE_FLUID_EXTRACTOR;
	public static final DeferredHolder<?, ? extends Item>    pipeFluidTerminus        = null; // unregistered — PipeFluidTerminus removed

	// Modules / Upgrades
	public static final DeferredHolder<?, ItemBlankModule>      blankModule          = LPRegistries.MODULE_BLANK;
	public static BiMap<String, net.minecraft.resources.ResourceLocation> modules  = HashBiMap.create();
	public static BiMap<String, net.minecraft.resources.ResourceLocation> upgrades = HashBiMap.create();

	// Miscellaneous Items
	public static final DeferredHolder<?, ItemGuideBook>          itemGuideBook        = LPRegistries.GUIDE_BOOK;
	public static final DeferredHolder<?, RemoteOrderer>          remoteOrderer        = LPRegistries.REMOTE_ORDERER;
	public static final DeferredHolder<?, ItemDisk>               disk                 = LPRegistries.DISK;
	public static final DeferredHolder<?, LogisticsItemCard>      itemCard             = LPRegistries.ITEM_CARD;
	public static final DeferredHolder<?, ItemHUDArmor>           hudGlasses           = LPRegistries.HUD_GLASSES;
	public static final DeferredHolder<?, LogisticsFluidContainer> fluidContainer      = LPRegistries.FLUID_CONTAINER;
	public static final DeferredHolder<?, ItemPipeController>     pipeController       = LPRegistries.PIPE_CONTROLLER;
	public static final DeferredHolder<?, ItemLogisticsProgrammer> logisticsProgrammer = LPRegistries.LOGISTICS_PROGRAMMER;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipBasic            = LPRegistries.CHIP_BASIC;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipBasicRaw         = LPRegistries.CHIP_BASIC_RAW;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipAdvanced         = LPRegistries.CHIP_ADVANCED;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipAdvancedRaw      = LPRegistries.CHIP_ADVANCED_RAW;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipFPGA             = LPRegistries.CHIP_FPGA;
	public static final DeferredHolder<?, ItemLogisticsChips>     chipFPGARaw          = LPRegistries.CHIP_FPGA_RAW;
	public static final DeferredHolder<?, LogisticsBrokenItem>    brokenItem           = LPRegistries.BROKEN_ITEM;

	// Typed helpers for Kotlin callers
	public static ItemGuideBook getItemGuideBook() { return itemGuideBook.get(); }
	public static LogisticsBrokenItem getBrokenItem() { return brokenItem.get(); }

}
