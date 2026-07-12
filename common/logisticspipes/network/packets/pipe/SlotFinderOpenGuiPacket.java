package logisticspipes.network.packets.pipe;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SlotFinderOpenGuiPacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int slot;

	public SlotFinderOpenGuiPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		//hack to avoid wrenching blocks
		int savedEquipped = player.getInventory().selected;
		boolean foundSlot = false;
		//try to find a empty slot
		for (int i = 0; i < 9; i++) {
			if (player.getInventory().getItem(i).isEmpty()) {
				foundSlot = true;
				player.getInventory().selected = i;
				break;
			}
		}
		//okay, anything that's a block?
		if (!foundSlot) {
			for (int i = 0; i < 9; i++) {
				ItemStack is = player.getInventory().getItem(i);
				if (!is.isEmpty() && is.getItem() instanceof BlockItem) {
					foundSlot = true;
					player.getInventory().selected = i;
					break;
				}
			}
		}
		//give up and select whatever is right of the current slot
		if (!foundSlot) {
			player.getInventory().selected = (player.getInventory().selected + 1) % 9;
		}

		boolean openedGui = false;
		final LogisticsTileGenericPipe genericPipe = getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (genericPipe.isRoutingPipe()) {
			openedGui = genericPipe.getRoutingPipe().getAvailableAdjacent().inventories().stream()
					.filter(neighbor -> LPNeighborTileEntityKt.getInventoryUtil(neighbor) instanceof ISpecialInsertion)
					.anyMatch(neighbor -> {
						for (ICraftingRecipeProvider provider : SimpleServiceLocator.craftingRecipeProviders) {
							if (provider.canOpenGui(neighbor.getTileEntity())) {
								return true;
							}
						}

						final BlockPos blockPos = neighbor.getTileEntity().getBlockPos();
						Block block = player.level().getBlockState(blockPos).getBlock();
						final BlockState blockState = player.level().getBlockState(blockPos);
						if (!blockState.isAir()) {
							int xCoord = blockPos.getX();
							int yCoord = blockPos.getY();
							int zCoord = blockPos.getZ();

							// EnderStorage check removed — no 1.20.1 port (former dummy isEnderChestBlock was always false).

							net.minecraft.world.phys.BlockHitResult blockHit = new net.minecraft.world.phys.BlockHitResult(
									new net.minecraft.world.phys.Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5),
									Direction.UP, blockPos, false);
							if (blockState.useWithoutItem(player.level(), player, blockHit) != net.minecraft.world.InteractionResult.PASS) {
								MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SlotFinderActivatePacket.class)
										.setTargetPosX(xCoord)
										.setTargetPosY(yCoord)
										.setTargetPosZ(zCoord)
										.setSlot(getSlot())
										.setPacketPos(this), player);
								return true;
							}
						}

						return false;
					});
		}

		if (!openedGui) {
			LogisticsPipes.log.warn("Ignored SlotFinderOpenGuiPacket from " + player.toString() + ", because of failing preconditions");
		}

		player.getInventory().selected = savedEquipped;
	}

	@Override
	public ModernPacket template() {
		return new SlotFinderOpenGuiPacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(slot);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		slot = input.readInt();
	}
}
