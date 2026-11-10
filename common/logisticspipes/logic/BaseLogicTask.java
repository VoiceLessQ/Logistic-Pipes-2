package logisticspipes.logic;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;

public abstract class BaseLogicTask {

	//Graphical Interface
	@Getter
	protected int posX;
	@Getter
	protected int posY;
	@Getter
	protected String name = getTypeName();
	@Getter
	protected String comment = "";

	//Saving and Server/Client sync
	@Getter
	protected UUID uuid;

	public BaseLogicTask(CompoundTag nbt) {
		posX = nbt.getIntOr("posX", 0);
		posY = nbt.getIntOr("posY", 0);
		name = nbt.getStringOr("name", "");
		comment = nbt.getStringOr("comment", "");
		uuid = UUID.fromString(nbt.getStringOr("uuid", ""));
	}

	public BaseLogicTask(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
		uuid = UUID.randomUUID();
	}

	public final CompoundTag getCompoundTag() {
		CompoundTag nbt = new CompoundTag();
		addToNBT(nbt);
		return nbt;
	}

	protected void addToNBT(CompoundTag nbt) {
		nbt.putInt("posX", posX);
		nbt.putInt("posY", posY);
		nbt.putString("name", name);
		nbt.putString("comment", comment);
		nbt.putString("uuid", uuid.toString());
	}

	public abstract int getAmountOfInput();

	public abstract int getAmountOfOutput();

	public abstract LogicParameterType getInputParameterType(int i);

	public abstract LogicParameterType getOutputParameterType(int i);

	public abstract void setInputParameter(int i, Object value);

	public abstract boolean isCalculated();

	public abstract Object getResult(int i);

	public abstract void resetState();

	public abstract String getTypeName();

	public abstract void syncTick(BlockEntity tile);
}
