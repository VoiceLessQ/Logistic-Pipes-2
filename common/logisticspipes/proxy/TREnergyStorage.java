package logisticspipes.proxy;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import team.reborn.energy.api.EnergyStorage;

import logisticspipes.proxy.interfaces.IEnergyStorage;

/** Team Reborn Energy view of an LP FE storage (1 E = 1 FE); changes reach the LP storage when the outer transaction commits. */
public class TREnergyStorage extends SnapshotParticipant<TREnergyStorage.Pending> implements EnergyStorage {

	record Pending(int inserted, int extracted) {}

	private final IEnergyStorage handler;
	private int inserted;
	private int extracted;

	public TREnergyStorage(IEnergyStorage handler) {
		this.handler = handler;
	}

	@Override
	public boolean supportsInsertion() {
		return handler.canReceive();
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notNegative(maxAmount);
		if (!handler.canReceive()) return 0;
		int amount = (int) Math.min(maxAmount, Math.max(0, handler.receiveEnergy(Integer.MAX_VALUE, true) - inserted));
		if (amount > 0) {
			updateSnapshots(transaction);
			inserted += amount;
		}
		return amount;
	}

	@Override
	public boolean supportsExtraction() {
		return handler.canExtract();
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notNegative(maxAmount);
		if (!handler.canExtract()) return 0;
		int amount = (int) Math.min(maxAmount, Math.max(0, handler.extractEnergy(Integer.MAX_VALUE, true) - extracted));
		if (amount > 0) {
			updateSnapshots(transaction);
			extracted += amount;
		}
		return amount;
	}

	@Override
	public long getAmount() {
		return Math.max(0L, (long) handler.getEnergyStored() + inserted - extracted);
	}

	@Override
	public long getCapacity() {
		return handler.getMaxEnergyStored();
	}

	@Override
	protected Pending createSnapshot() {
		return new Pending(inserted, extracted);
	}

	@Override
	protected void readSnapshot(Pending snapshot) {
		inserted = snapshot.inserted();
		extracted = snapshot.extracted();
	}

	@Override
	protected void onFinalCommit() {
		if (inserted > 0) handler.receiveEnergy(inserted, false);
		if (extracted > 0) handler.extractEnergy(extracted, false);
		inserted = 0;
		extracted = 0;
	}
}
