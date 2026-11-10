package logisticspipes.utils;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import logisticspipes.interfaces.ITankUtil;

/** {@link ITankUtil} over a Transfer API fluid storage; LP millibuckets become droplets here. */
public class TankUtil implements ITankUtil {

	private final Storage<FluidVariant> storage;

	public TankUtil(Storage<FluidVariant> storage) {
		this.storage = storage;
	}

	@Override
	public boolean containsTanks() {
		return storage.supportsInsertion() || storage.supportsExtraction();
	}

	@Override
	public int fill(FluidIdentifierStack stack, boolean doFill) {
		if (stack == null || stack.getAmount() <= 0) return 0;
		try (Transaction tx = Transaction.openOuter()) {
			long inserted = storage.insert(stack.makeFluidVariant(), stack.getDroplets(), tx);
			int mb = LPFluidTank.toMb(inserted);
			if (doFill) {
				if (inserted != LPFluidTank.toDroplets(mb)) {
					tx.abort();
					return fillExact(stack.makeFluidVariant(), mb);
				}
				tx.commit();
			}
			return mb;
		}
	}

	private int fillExact(FluidVariant variant, int mb) {
		if (mb <= 0) return 0;
		try (Transaction tx = Transaction.openOuter()) {
			long inserted = storage.insert(variant, LPFluidTank.toDroplets(mb), tx);
			tx.commit();
			return LPFluidTank.toMb(inserted);
		}
	}

	@Override
	public FluidIdentifierStack drain(FluidIdentifierStack stack, boolean doDrain) {
		if (stack == null || stack.getAmount() <= 0) return null;
		return extract(stack.makeFluidVariant(), stack.getAmount(), doDrain);
	}

	@Override
	public FluidIdentifierStack drain(int amount, boolean doDrain) {
		for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
			FluidIdentifierStack drained = extract(view.getResource(), amount, doDrain);
			if (drained != null) return drained;
		}
		return null;
	}

	private FluidIdentifierStack extract(FluidVariant variant, int mb, boolean doDrain) {
		try (Transaction tx = Transaction.openOuter()) {
			long extracted = storage.extract(variant, LPFluidTank.toDroplets(mb), tx);
			int extractedMb = LPFluidTank.toMb(extracted);
			if (extractedMb <= 0) return null;
			if (doDrain) {
				if (extracted != LPFluidTank.toDroplets(extractedMb)) {
					tx.abort();
					try (Transaction exact = Transaction.openOuter()) {
						extractedMb = LPFluidTank.toMb(storage.extract(variant, LPFluidTank.toDroplets(extractedMb), exact));
						exact.commit();
					}
				} else {
					tx.commit();
				}
			}
			return FluidIdentifierStack.getFromVariant(variant, LPFluidTank.toDroplets(extractedMb));
		}
	}

	@Override
	public Stream<FluidIdentifierStack> tanks() {
		return StreamSupport.stream(storage.nonEmptyViews().spliterator(), false)
				.map(view -> FluidIdentifierStack.getFromVariant(view.getResource(), view.getAmount()))
				.filter(stack -> stack != null && stack.getAmount() > 0);
	}

	@Override
	public boolean canDrain(FluidIdentifier fluid) {
		try (Transaction tx = Transaction.openOuter()) {
			return storage.extract(fluid.makeFluidVariant(), 1, tx) > 0;
		}
	}

	@Override
	public int getFreeSpaceInsideTank(FluidIdentifier type) {
		try (Transaction tx = Transaction.openOuter()) {
			return LPFluidTank.toMb(storage.insert(type.makeFluidVariant(), Long.MAX_VALUE / 2, tx));
		}
	}
}
