package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;


public class PlayerCollectionList {

	private List<EqualWeakReference<Player>> players = new ArrayList<>();
	private boolean checkingPlayers = false;

	public void checkPlayers() {
		checkingPlayers = true;
		Iterator<EqualWeakReference<Player>> iPlayers = players.iterator();
		while (iPlayers.hasNext()) {
			EqualWeakReference<Player> playerReference = iPlayers.next();
			boolean remove = false;
			if (playerReference.get() == null) {
				remove = true;
			} else if (playerReference.get().isDeadOrDying()) {
				remove = true;
			} else if (playerReference.get() instanceof ServerPlayer) {
				if (((ServerPlayer) playerReference.get()).hasDisconnected()) {
					remove = true;
				}
			}
			if (remove) {
				iPlayers.remove();
			}
		}
		checkingPlayers = false;
	}

	public Iterable<Player> players() {
		checkPlayers();
		return () -> new Itr(players.iterator());
	}

	public int size() {
		if (!checkingPlayers) {
			checkPlayers();
		}
		return players.size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean isEmptyWithoutCheck() {
		return players.size() == 0;
	}

	public void add(Player player) {
		players.add(new EqualWeakReference<>(player));
	}

	public boolean remove(Player player) {
		if (contains(player) && players.size() > 0) {
			return players.remove(new EqualWeakReference<>(player));
		} else {
			return false;
		}
	}

	public boolean contains(Player player) {
		checkPlayers();
		return players.contains(new EqualWeakReference<>(player));
	}

	private static class Itr implements Iterator<Player> {

		private final Iterator<EqualWeakReference<Player>> iterator;

		private Itr(Iterator<EqualWeakReference<Player>> source) {
			iterator = source;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Player next() {
			EqualWeakReference<Player> reference = iterator.next();
			return reference.get();
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}
}
