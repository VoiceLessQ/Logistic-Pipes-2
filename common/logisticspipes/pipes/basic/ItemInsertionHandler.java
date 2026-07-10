package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;

import net.neoforged.neoforge.items.IItemHandler;

import logisticspipes.interfaces.ILPItemAcceptor;

public class ItemInsertionHandler implements IItemHandler {

	public static final List<ILPItemAcceptor> ACCEPTORS = new ArrayList<>();

	private final LogisticsTileGenericPipe pipe;
	private final Direction dir;

	public ItemInsertionHandler(LogisticsTileGenericPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) { // getItem → getStackInSlot in IItemHandler 1.20.1
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return true;
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (!simulate) {
			return handleItemInsetion(pipe, dir, stack);
		}
		return ItemStack.EMPTY;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Nonnull
	public static ItemStack handleItemInsetion(LogisticsTileGenericPipe pipe, Direction from, @Nonnull ItemStack stack) {
		for (ILPItemAcceptor acceptor : ACCEPTORS) {
			if (acceptor.accept(pipe, from, stack)) {
				return ItemStack.EMPTY;
			}
		}
		return pipe.insertItem(from, stack);
	}
}
