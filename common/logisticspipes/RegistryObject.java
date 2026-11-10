package logisticspipes;

import java.util.function.Supplier;

import net.minecraft.resources.Identifier;

public final class RegistryObject<T> implements Supplier<T> {

	private final Identifier id;
	private final T value;

	RegistryObject(Identifier id, T value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public T get() {
		return value;
	}

	public Identifier getId() {
		return id;
	}
}
