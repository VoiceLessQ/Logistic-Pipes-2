package logisticspipes.request;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;


import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.ComponentList;
import logisticspipes.network.packets.orderer.MissingItems;
import logisticspipes.network.packets.orderer.OrdererContent;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree.ActiveRequestType;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class RequestHandler {

	public enum DisplayOptions {
		Both,
		SupplyOnly,
		CraftOnly
	}

	public static void request(final Player player, final ItemIdentifierStack stack, final CoreRoutedPipe pipe) {
		if (!pipe.useEnergy(5)) {
			player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return;
		}
		RequestTree.request(new ItemIdentifierStack(stack), pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(true), player);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {
				Collection<IResource> coll = new ArrayList<>(1);
				coll.add(item);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(coll).setFlag(false), player);
				if (pipe instanceof IRequestWatcher) {
					((IRequestWatcher) pipe).handleOrderList(item, parts);
				}
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {}
		}, null);
	}

	public static void simulate(final Player player, final ItemIdentifierStack stack, CoreRoutedPipe pipe) {
		final List<IResource> usedList = new ArrayList<>();
		final List<IResource> missingList = new ArrayList<>();
		RequestTree.simulate(new ItemIdentifierStack(stack), pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				missingList.addAll(resources);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {
				usedList.addAll(resources);
			}
		});
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ComponentList.class).setUsed(usedList).setMissing(missingList), player);
	}

	public static void refresh(Player player, CoreRoutedPipe pipe, DisplayOptions option) {
		Map<ItemIdentifier, Integer> _availableItems;
		LinkedList<ItemIdentifier> _craftableItems;

		if (option == DisplayOptions.SupplyOnly || option == DisplayOptions.Both) {
			_availableItems = SimpleServiceLocator.logisticsManager.getAvailableItems(pipe.getRouter().getIRoutersByCost());
		} else {
			_availableItems = new HashMap<>();
		}
		if (option == DisplayOptions.CraftOnly || option == DisplayOptions.Both) {
			_craftableItems = SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost());
		} else {
			_craftableItems = new LinkedList<>();
		}
		TreeSet<ItemIdentifierStack> _allItems = new TreeSet<>();

		for (Entry<ItemIdentifier, Integer> item : _availableItems.entrySet()) {
			ItemIdentifierStack newStack = item.getKey().makeStack(item.getValue());
			_allItems.add(newStack);
		}

		for (ItemIdentifier item : _craftableItems) {
			if (_availableItems.containsKey(item)) {
				continue;
			}
			_allItems.add(item.makeStack(0));
		}
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererContent.class).setIdentSet(_allItems), player);
	}

	public static void requestList(final Player player, final List<ItemIdentifierStack> list, final CoreRoutedPipe pipe) {
		if (!pipe.useEnergy(5)) {
			player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return;
		}
		RequestTree.request(list, pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(true), player);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(false), player);
				if (pipe instanceof IRequestWatcher) {
					((IRequestWatcher) pipe).handleOrderList(null, parts);
				}
			}
		}, RequestTree.defaultRequestFlags, null);
	}

	public static void requestMacrolist(CompoundTag itemlist, final CoreRoutedPipe requester, final Player player) {
		if (!requester.useEnergy(5)) {
			player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return;
		}
		ListTag list = itemlist.getListOrEmpty("inventar");
		final List<ItemIdentifierStack> transaction = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag itemnbt = list.getCompoundOrEmpty(i);
			CompoundTag itemNBTContent = itemnbt.getCompoundOrEmpty("nbt");
			if (!itemnbt.contains("nbt")) {
				itemNBTContent = null;
			}
			ItemIdentifierStack stack = ItemIdentifier.get(BuiltInRegistries.ITEM.byId(itemnbt.getIntOr("id", 0)), itemnbt.getIntOr("data", 0), itemNBTContent).makeStack(itemnbt.getIntOr("amount", 0));
			transaction.add(stack);
		}
		RequestTree.request(transaction, requester, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(true), player);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(false), player);
				if (requester instanceof IRequestWatcher) {
					((IRequestWatcher) requester).handleOrderList(null, parts);
				}
			}
		}, RequestTree.defaultRequestFlags, null);
	}

	public static Object[] computerRequest(final ItemIdentifierStack makeStack, final CoreRoutedPipe pipe, boolean craftingOnly) {

		EnumSet<ActiveRequestType> requestFlags;
		if (craftingOnly) {
			requestFlags = EnumSet.of(ActiveRequestType.Craft);
		} else {
			requestFlags = EnumSet.of(ActiveRequestType.Craft, ActiveRequestType.Provide);
		}
		if (!pipe.useEnergy(15)) {
			return new Object[] { "NO_POWER" };
		}
		final Object[] status = new Object[2];
		RequestTree.request(makeStack, pipe, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				status[0] = "MISSING";
				status[1] = resources;
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {
				status[0] = "DONE";
				List<IResource> itemList = new LinkedList<>();
				itemList.add(item);
				status[1] = itemList;
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {}
		}, false, false, true, false, requestFlags, null);
		return status;
	}

	public static void refreshFluid(Player player, CoreRoutedPipe pipe) {
		TreeSet<FluidIdentifierStack> _allItems = SimpleServiceLocator.logisticsFluidManager.getAvailableFluid(pipe.getRouter().getIRoutersByCost());
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererContent.class)
						.setIdentSet(
								_allItems.stream()
										.map(item -> new ItemIdentifierStack(item.getFluid().getItemIdentifier(), item.getAmount()))
										.collect(Collectors.toCollection(TreeSet::new))
						)
				, player);
	}

	public static void requestFluid(final Player player, final ItemIdentifierStack stack, CoreRoutedPipe pipe, IRequestFluid requester) {
		if (!pipe.useEnergy(10)) {
			player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return;
		}

		RequestTree.requestFluid(FluidIdentifier.get(stack.getItem()), stack.getStackSize(), requester, new RequestLog() {

			@Override
			public void handleMissingItems(List<IResource> resources) {
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(resources).setFlag(true), player);
			}

			@Override
			public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {
				Collection<IResource> coll = new ArrayList<>(1);
				coll.add(item);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(coll).setFlag(false), player);
			}

			@Override
			public void handleSucessfullRequestOfList(List<IResource> resources, LinkedLogisticsOrderList parts) {}
		});
	}
}
