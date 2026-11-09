package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import logisticspipes.interfaces.ILPItemAcceptor;

/** Insert-only item storage exposed on each pipe side; items enter the pipe when the outer transaction commits. */
public class ItemInsertionHandler extends SnapshotParticipant<List<ItemStack>> implements InsertionOnlyStorage<ItemVariant> {

	public static final List<ILPItemAcceptor> ACCEPTORS = new ArrayList<>();

	private final LogisticsTileGenericPipe pipe;
	private final Direction dir;
	private List<ItemStack> pending = new ArrayList<>();

	public ItemInsertionHandler(LogisticsTileGenericPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		int amount = (int) Math.min(maxAmount, resource.getItem().getDefaultMaxStackSize());
		if (amount <= 0) return 0;
		updateSnapshots(transaction);
		pending.add(resource.toStack(amount));
		return amount;
	}

	@Override
	protected List<ItemStack> createSnapshot() {
		return new ArrayList<>(pending);
	}

	@Override
	protected void readSnapshot(List<ItemStack> snapshot) {
		pending = snapshot;
	}

	@Override
	protected void onFinalCommit() {
		List<ItemStack> stacks = pending;
		pending = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack left = handleItemInsetion(pipe, dir, stack);
			if (!left.isEmpty() && pipe.getLevel() != null) {
				Containers.dropItemStack(pipe.getLevel(), pipe.getX() + 0.5, pipe.getY() + 0.5, pipe.getZ() + 0.5, left);
			}
		}
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
