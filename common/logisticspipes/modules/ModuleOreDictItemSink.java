package logisticspipes.modules;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;



import logisticspipes.gui.hud.modules.HUDOreDictItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.OreDictItemSinkModuleInHand;
import logisticspipes.network.guis.module.inpipe.OreDictItemSinkModuleSlot;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.module.OreDictItemSinkList;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.StringListProperty;

public class ModuleOreDictItemSink extends LogisticsModule
		implements IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, Gui {

	public final StringListProperty oreList = new StringListProperty("");

	//map of Item:<set of damagevalues>, empty set if wildcard damage
	private Map<Item, Set<Integer>> oreItemIdMap;

	private final IHUDModuleRenderer HUD = new HUDOreDictItemSink(this);
	private List<ItemIdentifierStack> oreHudList;

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

	private SinkReply _sinkReply;

	public static String getName() {
		return "item_sink_oredict";
	}

	@Nonnull
	@Override
	public String getLPName() {
		return getName();
	}

	@Nonnull
	@Override
	public List<Property<?>> getProperties() {
		return Collections.singletonList(oreList);
	}

	@Override
	public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.OreDictItemSink,
				0,
				true,
				false,
				5,
				0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public SinkReply sinksItem(@Nonnull ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		if (bestPriority > _sinkReply.fixedPriority.ordinal() || (bestPriority == _sinkReply.fixedPriority.ordinal() && bestCustomPriority >= _sinkReply.customPriority)) {
			return null;
		}
		if (oreItemIdMap == null) {
			buildOreItemIdMap();
		}
		Set<Integer> damageSet = oreItemIdMap.get(item.item);
		if (damageSet == null) {
			return null;
		}
		if (damageSet.isEmpty() || damageSet.contains(item.itemDamage)) {
			return _sinkReply;
		}
		return null;
	}

	public List<ItemIdentifierStack> getHudItemList() {
		if (oreItemIdMap == null) {
			buildOreItemIdMap();
		}
		return oreHudList;
	}

	private void buildOreItemIdMap() {
		oreItemIdMap = new HashMap<>();
		oreHudList = new ArrayList<>(oreList.size());
		// In 1.20.1 each entry in oreList is a tag id ("forge:ingots/iron"); we resolve
		// it to every Item carrying that tag. Damage values no longer exist, so the
		// inner damage Set is always empty, which sinksItem treats as "match any".
		for (String orename : oreList) {
			ResourceLocation loc = ResourceLocation.tryParse(orename);
			ItemStack stackForHud = ItemStack.EMPTY;
			if (loc != null) {
				TagKey<Item> tag = TagKey.create(Registries.ITEM, loc);
				for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
					Item item = holder.value();
					oreItemIdMap.put(item, new TreeSet<>());
					if (stackForHud.isEmpty()) {
						stackForHud = new ItemStack(item);
					}
				}
			}
			if (!stackForHud.isEmpty()) {
				oreHudList.add(new ItemIdentifierStack(ItemIdentifier.get(stackForHud), 1));
			} else {
				oreHudList.add(new ItemIdentifierStack(ItemIdentifier.get(Items.AIR, 0, null), 1));
			}
		}
	}

	@Override
	public void readFromNBT(@Nonnull CompoundTag tag) {
		super.readFromNBT(tag);
		oreItemIdMap = null;
	}

	@Override
	public void tick() {}

	@Override
	public @Nonnull List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Ores: ");
		list.addAll(oreList);
		return list;
	}

	@Override
	public void startHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void stopHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		CompoundTag nbt = new CompoundTag();
		writeToNBT(nbt);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OreDictItemSinkList.class).setTag(nbt).setModulePos(this), player);
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	public void OreListChanged() {
		if (MainProxy.isServer(getWorld())) {
			CompoundTag nbt = new CompoundTag();
			writeToNBT(nbt);
			MainProxy.sendToPlayerList(PacketHandler.getPacket(OreDictItemSinkList.class).setTag(nbt).setModulePos(this), localModeWatchers);
		} else {
			CompoundTag nbt = new CompoundTag();
			writeToNBT(nbt);
			MainProxy.sendPacketToServer(PacketHandler.getPacket(OreDictItemSinkList.class).setTag(nbt).setModulePos(this));
		}
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return HUD;
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return false;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		CompoundTag nbt = new CompoundTag();
		writeToNBT(nbt);
		return NewGuiHandler.getGui(OreDictItemSinkModuleSlot.class).setNbt(nbt);
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(OreDictItemSinkModuleInHand.class);
	}

}
