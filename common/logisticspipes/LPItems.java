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
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeUnrouted             = LPRegistries.PIPE_UNROUTED;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeBasic                = LPRegistries.PIPE_BASIC;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeRequest              = LPRegistries.PIPE_REQUEST;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeRequestMk2           = LPRegistries.PIPE_REQUEST_MK2;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeProvider             = LPRegistries.PIPE_PROVIDER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeCrafting             = LPRegistries.PIPE_CRAFTING;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeSatellite            = LPRegistries.PIPE_SATELLITE;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeSupplier             = LPRegistries.PIPE_SUPPLIER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeChassisMk1           = LPRegistries.PIPE_CHASSIS_MK1;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeChassisMk2           = LPRegistries.PIPE_CHASSIS_MK2;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeChassisMk3           = LPRegistries.PIPE_CHASSIS_MK3;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeChassisMk4           = LPRegistries.PIPE_CHASSIS_MK4;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeChassisMk5           = LPRegistries.PIPE_CHASSIS_MK5;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeInvSystemConnector   = LPRegistries.PIPE_INV_SYS_CONNECTOR;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeSystemEntrance       = LPRegistries.PIPE_SYSTEM_ENTRANCE;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeSystemDestination    = LPRegistries.PIPE_SYSTEM_DESTINATION;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFirewall             = LPRegistries.PIPE_FIREWALL;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeRemoteOrderer        = LPRegistries.PIPE_REMOTE_ORDERER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> requestTable             = LPRegistries.PIPE_REQUEST_TABLE;

	// Logistics Fluid Pipes
	// NOTE: pipeFluidBasic and pipeFluidTerminus have no corresponding pipe class (removed upstream).
	public static final DeferredHolder<net.minecraft.world.item.Item, ? extends Item>    pipeFluidBasic           = null; // unregistered — PipeFluidBasic removed
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidRequest         = LPRegistries.PIPE_FLUID_REQUEST;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidProvider        = LPRegistries.PIPE_FLUID_PROVIDER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidSatellite       = LPRegistries.PIPE_FLUID_SATELLITE;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidSupplier        = LPRegistries.PIPE_FLUID_SUPPLIER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidSupplierMk2     = LPRegistries.PIPE_FLUID_SUPPLIER_MK2;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidInsertion       = LPRegistries.PIPE_FLUID_INSERTION;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsPipe> pipeFluidExtractor       = LPRegistries.PIPE_FLUID_EXTRACTOR;
	public static final DeferredHolder<net.minecraft.world.item.Item, ? extends Item>    pipeFluidTerminus        = null; // unregistered — PipeFluidTerminus removed

	// Modules / Upgrades
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemBlankModule>      blankModule          = LPRegistries.MODULE_BLANK;
	public static BiMap<String, net.minecraft.resources.ResourceLocation> modules  = HashBiMap.create();
	public static BiMap<String, net.minecraft.resources.ResourceLocation> upgrades = HashBiMap.create();

	// Miscellaneous Items
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemGuideBook>          itemGuideBook        = LPRegistries.GUIDE_BOOK;
	public static final DeferredHolder<net.minecraft.world.item.Item, RemoteOrderer>          remoteOrderer        = LPRegistries.REMOTE_ORDERER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemDisk>               disk                 = LPRegistries.DISK;
	public static final DeferredHolder<net.minecraft.world.item.Item, LogisticsItemCard>      itemCard             = LPRegistries.ITEM_CARD;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemHUDArmor>           hudGlasses           = LPRegistries.HUD_GLASSES;
	public static final DeferredHolder<net.minecraft.world.item.Item, LogisticsFluidContainer> fluidContainer      = LPRegistries.FLUID_CONTAINER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemPipeController>     pipeController       = LPRegistries.PIPE_CONTROLLER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsProgrammer> logisticsProgrammer = LPRegistries.LOGISTICS_PROGRAMMER;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipBasic            = LPRegistries.CHIP_BASIC;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipBasicRaw         = LPRegistries.CHIP_BASIC_RAW;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipAdvanced         = LPRegistries.CHIP_ADVANCED;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipAdvancedRaw      = LPRegistries.CHIP_ADVANCED_RAW;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipFPGA             = LPRegistries.CHIP_FPGA;
	public static final DeferredHolder<net.minecraft.world.item.Item, ItemLogisticsChips>     chipFPGARaw          = LPRegistries.CHIP_FPGA_RAW;
	public static final DeferredHolder<net.minecraft.world.item.Item, LogisticsBrokenItem>    brokenItem           = LPRegistries.BROKEN_ITEM;

	// Typed helpers for Kotlin callers
	public static ItemGuideBook getItemGuideBook() { return itemGuideBook.get(); }
	public static LogisticsBrokenItem getBrokenItem() { return brokenItem.get(); }

}
