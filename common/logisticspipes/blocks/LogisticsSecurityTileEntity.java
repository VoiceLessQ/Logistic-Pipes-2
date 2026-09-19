package logisticspipes.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nonnull;

import net.minecraft.CrashReportCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;


import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.SecurityStationGui;
import logisticspipes.network.packets.block.SecurityStationAutoDestroy;
import logisticspipes.network.packets.block.SecurityStationCC;
import logisticspipes.network.packets.block.SecurityStationCCIDs;
import logisticspipes.network.packets.block.SecurityStationId;
import logisticspipes.network.packets.block.SecurityStationOpenPlayer;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class LogisticsSecurityTileEntity extends LogisticsSolidTileEntity implements IGuiOpenControler, ISecurityProvider, IGuiTileEntity {

	public LogisticsSecurityTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_SECURITY_STATION.get(), pos, state);
	}

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "ID Slots", 64);
	private PlayerCollectionList listener = new PlayerCollectionList();
	private UUID secId = null;
	private Map<String, SecuritySettings> settingsList = new HashMap<>();
	public List<Integer> excludedCC = new ArrayList<>();
	public boolean allowCC = false;
	public boolean allowAutoDestroy = false;

	public static PlayerCollectionList byPassed = new PlayerCollectionList();
	public static final SecuritySettings allowAll = new SecuritySettings("");

	static {
		LogisticsSecurityTileEntity.allowAll.openGui = true;
		LogisticsSecurityTileEntity.allowAll.openRequest = true;
		LogisticsSecurityTileEntity.allowAll.openUpgrades = true;
		LogisticsSecurityTileEntity.allowAll.openNetworkMonitor = true;
		LogisticsSecurityTileEntity.allowAll.removePipes = true;
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.remove(this);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (MainProxy.isServer(getWorld())) {
			SimpleServiceLocator.securityStationManager.add(this);
		}
	}

	// onChunkUnload removed in 1.20.1 — setRemoved() covers this case

	public void deauthorizeStation() {
		SimpleServiceLocator.securityStationManager.deauthorizeUUID(getSecId());
	}

	public void authorizeStation() {
		SimpleServiceLocator.securityStationManager.authorizeUUID(getSecId());
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCC.class).putInt(allowCC ? 1 : 0).setBlockPos(getBlockPos()), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationAutoDestroy.class).putInt(allowAutoDestroy ? 1 : 0).setBlockPos(getBlockPos()), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationId.class).setUuid(getSecId()).setBlockPos(getBlockPos()), player);
		SimpleServiceLocator.securityStationManager.sendClientAuthorizationList();
		listener.add(player);
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		listener.remove(player);
	}

	public UUID getSecId() {
		if (MainProxy.isServer(getWorld())) {
			if (secId == null) {
				secId = UUID.randomUUID();
			}
		}
		return secId;
	}

	public void setClientUUID(UUID id) {
		if (MainProxy.isClient(getWorld())) {
			secId = id;
		}
	}

	public void setClientCC(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowCC = flag;
		}
	}

	public void setClientDestroy(boolean flag) {
		if (MainProxy.isClient(getWorld())) {
			allowAutoDestroy = flag;
		}
	}

	@Override
	protected void loadAdditional(CompoundTag par1nbtTagCompound, net.minecraft.core.HolderLookup.Provider registries) {
		super.loadAdditional(par1nbtTagCompound, registries);
		if (par1nbtTagCompound.contains("UUID")) {
			secId = UUID.fromString(par1nbtTagCompound.getString("UUID"));
		}
		allowCC = par1nbtTagCompound.getBoolean("allowCC");
		allowAutoDestroy = par1nbtTagCompound.getBoolean("allowAutoDestroy");
		inv.readFromNBT(par1nbtTagCompound);
		settingsList.clear();
		ListTag list = par1nbtTagCompound.getList("settings", 10);
		while (list.size() > 0) {
			net.minecraft.nbt.Tag base = list.remove(0);
			String name = ((CompoundTag) base).getString("name");
			CompoundTag value = ((CompoundTag) base).getCompound("content");
			SecuritySettings settings = new SecuritySettings(name);
			settings.readFromNBT(value);
			settingsList.put(name, settings);
		}
		excludedCC.clear();
		list = par1nbtTagCompound.getList("excludedCC", 3);
		while (list.size() > 0) {
			net.minecraft.nbt.Tag base = list.remove(0);
			excludedCC.add(((IntTag) base).getAsInt());
		}
	}

	@Override
	protected void saveAdditional(CompoundTag par1nbtTagCompound, net.minecraft.core.HolderLookup.Provider registries) {
		super.saveAdditional(par1nbtTagCompound, registries);
		par1nbtTagCompound.putString("UUID", getSecId().toString());
		par1nbtTagCompound.putBoolean("allowCC", allowCC);
		par1nbtTagCompound.putBoolean("allowAutoDestroy", allowAutoDestroy);
		inv.writeToNBT(par1nbtTagCompound);
		ListTag list = new ListTag();
		for (Entry<String, SecuritySettings> entry : settingsList.entrySet()) {
			CompoundTag nbt = new CompoundTag();
			nbt.putString("name", entry.getKey());
			CompoundTag value = new CompoundTag();
			entry.getValue().writeToNBT(value);
			nbt.put("content", value);
			list.add(nbt);
		}
		par1nbtTagCompound.put("settings", list);
		list = new ListTag();
		for (Integer i : excludedCC) {
			list.add(IntTag.valueOf(i));
		}
		par1nbtTagCompound.put("excludedCC", list);
	}

	public void buttonFreqCard(int integer, Player player) {
		switch (integer) {
			case 0: //--
				inv.setItem(0, ItemStack.EMPTY);
				break;
			case 1: //-
				inv.removeItem(0, 1);
				break;
			case 2: //+
				if (!useEnergy(10)) {
					player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
					return;
				}
				if (inv.getIDStackInSlot(0) == null) {
					ItemStack stack = new ItemStack(LPItems.itemCard.get(), 1);
					CompoundTag secTag = new CompoundTag();
					secTag.putString("UUID", getSecId().toString());
					logisticspipes.utils.item.StackTag.setTag(stack, secTag);
					inv.setItem(0, stack);
				} else {
					ItemStack slot = inv.getItem(0);
					if (slot.getCount() < 64) {
						slot.grow(1);
						CompoundTag secTag = new CompoundTag();
						secTag.putString("UUID", getSecId().toString());
						logisticspipes.utils.item.StackTag.setTag(slot, secTag);
						inv.setItem(0, slot);
					}
				}
				break;
			case 3: //++
				if (!useEnergy(640)) {
					player.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
					return;
				}
				ItemStack stack = new ItemStack(LPItems.itemCard.get(), 64);
				CompoundTag secTag = new CompoundTag();
				secTag.putString("UUID", getSecId().toString());
				logisticspipes.utils.item.StackTag.setTag(stack, secTag);
				inv.setItem(0, stack);
				break;
		}
	}

	public void handleOpenSecurityPlayer(Player player, @Nonnull String string) {
		SecuritySettings setting = settingsList.get(string);
		if (setting == null) {
			if (string.isEmpty()) return;
			setting = new SecuritySettings(string);
			settingsList.put(string, setting);
		}
		CompoundTag nbt = new CompoundTag();
		setting.writeToNBT(nbt);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationOpenPlayer.class).put(nbt), player);
	}

	public void saveNewSecuritySettings(CompoundTag tag) {
		SecuritySettings setting = settingsList.get(tag.getString("name"));
		if (setting == null) {
			setting = new SecuritySettings(tag.getString("name"));
			settingsList.put(tag.getString("name"), setting);
		}
		setting.readFromNBT(tag);
	}

	public SecuritySettings getSecuritySettingsForPlayer(Player entityplayer, boolean usePower) {
		if (LogisticsSecurityTileEntity.byPassed.contains(entityplayer)) {
			return LogisticsSecurityTileEntity.allowAll;
		}
		if (usePower && !useEnergy(10)) {
			entityplayer.sendSystemMessage(Component.translatable("lp.misc.noenergy"));
			return new SecuritySettings("No Energy");
		}
		SecuritySettings setting = settingsList.get(entityplayer.getName().getString());
		//TODO Change to GameProfile based Authentication
		if (setting == null) {
			setting = new SecuritySettings(entityplayer.getName().getString());
			settingsList.put(entityplayer.getName().getString(), setting);
		}
		return setting;
	}

	public void changeCC() {
		allowCC = !allowCC;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationCC.class).putInt(allowCC ? 1 : 0).setBlockPos(getBlockPos()), listener);
	}

	public void changeDestroy() {
		allowAutoDestroy = !allowAutoDestroy;
		MainProxy.sendToPlayerList(PacketHandler.getPacket(SecurityStationAutoDestroy.class).putInt(allowAutoDestroy ? 1 : 0).setBlockPos(getBlockPos()), listener);
	}

	public void addCCToList(Integer id) {
		if (!excludedCC.contains(id)) {
			excludedCC.add(id);
		}
		Collections.sort(excludedCC);
	}

	public void removeCCFromList(Integer id) {
		excludedCC.remove(id);
	}

	public void requestList(Player player) {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		for (Integer i : excludedCC) {
			list.add(IntTag.valueOf(i));
		}
		tag.put("list", list);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationCCIDs.class).put(tag).setBlockPos(getBlockPos()), player);
	}

	public void handleListPacket(CompoundTag tag) {
		excludedCC.clear();
		ListTag list = tag.getList("list", 3);
		while (list.size() > 0) {
			net.minecraft.nbt.Tag base = list.remove(0);
			excludedCC.add(((IntTag) base).getAsInt());
		}
	}

	@Override
	public boolean getAllowCC(int id) {
		if (!useEnergy(10)) {
			return false;
		}
		return allowCC != excludedCC.contains(id);
	}

	@Override
	public boolean canAutomatedDestroy() {
		if (!useEnergy(10)) {
			return false;
		}
		return allowAutoDestroy;
	}

	private boolean useEnergy(int amount) {
		for (int i = 0; i < 4; i++) {
			BlockEntity tile = getWorld().getBlockEntity(getBlockPos().relative(Direction.values()[i + 2]));
			if (tile instanceof IRoutedPowerProvider) {
				if (((IRoutedPowerProvider) tile).useEnergy(amount)) {
					return true;
				}
			}
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof IRoutedPowerProvider) {
					if (((IRoutedPowerProvider) ((LogisticsTileGenericPipe) tile).pipe).useEnergy(amount)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void fillCrashReportCategory(CrashReportCategory par1CrashReportCategory) {
		super.fillCrashReportCategory(par1CrashReportCategory);
		par1CrashReportCategory.setDetail("LP-Version", LogisticsPipes.getVersionString());
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(SecurityStationGui.class);
	}
}
