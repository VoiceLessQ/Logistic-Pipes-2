package logisticspipes.renderer.state;

import java.util.Arrays;

import net.minecraft.core.Direction;

import lombok.Getter;

import logisticspipes.config.Configs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class TextureMatrix {

	//Old Pipe Renderer
	private final int[] iconIndexes = new int[7];

	//New Pipe Renderer
	@Getter
	private int textureIndex;
	@Getter
	private boolean isRouted;
	private boolean[] isRoutedInDir = new boolean[6];
	private boolean[] isSubPowerInDir = new boolean[6];
	@Getter
	private boolean hasPowerUpgrade;
	@Getter
	private boolean hasPower;
	@Getter
	private boolean isFluid;
	@Getter
	private Direction pointedOrientation;

	private boolean dirty = true;

	public int getTextureIndex(Direction direction) {
		return iconIndexes[direction.ordinal()];
	}

	public void setIconIndex(Direction direction, int value) {
		if (iconIndexes[direction.ordinal()] != value) {
			iconIndexes[direction.ordinal()] = value;
			dirty = true;
		}
	}

	public void refreshStates(CoreUnroutedPipe pipe) {
		if (textureIndex != pipe.getTextureIndex()) {
			dirty = true;
		}
		textureIndex = pipe.getTextureIndex();
		if (isRouted != pipe.isRoutedPipe()) {
			dirty = true;
		}
		isRouted = pipe.isRoutedPipe();
		if (isRouted) {
			CoreRoutedPipe cPipe = (CoreRoutedPipe) pipe;
			for (int i = 0; i < 6; i++) {
				if (isRoutedInDir[i] != cPipe.getRouter().isRoutedExit(Direction.from3DDataValue(i))) {
					dirty = true;
				}
				isRoutedInDir[i] = cPipe.getRouter().isRoutedExit(Direction.from3DDataValue(i));
			}
			for (int i = 0; i < 6; i++) {
				if (isSubPowerInDir[i] != cPipe.getRouter().isSubPoweredExit(Direction.from3DDataValue(i))) {
					dirty = true;
				}
				isSubPowerInDir[i] = cPipe.getRouter().isSubPoweredExit(Direction.from3DDataValue(i));
			}
			if (hasPowerUpgrade != (cPipe.getUpgradeManager().hasRFPowerSupplierUpgrade() || cPipe.getUpgradeManager().getIC2PowerLevel() > 0)) {
				dirty = true;
			}
			hasPowerUpgrade = cPipe.getUpgradeManager().hasRFPowerSupplierUpgrade() || cPipe.getUpgradeManager().getIC2PowerLevel() > 0;
			if (hasPower != (cPipe._textureBufferPowered || Configs.LOGISTICS_POWER_USAGE_DISABLED)) {
				dirty = true;
			}
			hasPower = cPipe._textureBufferPowered || Configs.LOGISTICS_POWER_USAGE_DISABLED;
			if (isFluid != cPipe.isFluidPipe()) {
				dirty = true;
			}
			isFluid = cPipe.isFluidPipe();
			if (pointedOrientation != cPipe.getPointedOrientation()) {
				dirty = true;
			}
			pointedOrientation = cPipe.getPointedOrientation();
		} else {
			isRoutedInDir = new boolean[6];
		}
	}

	/** Item-renderer entry: only sets textureIndex (no router/world access). */
	public void refreshStatesForItem(CoreUnroutedPipe pipe) {
		textureIndex = pipe.getTextureIndex();
		isRouted = pipe.isRoutedPipe();
		isRoutedInDir = new boolean[6];
		isSubPowerInDir = new boolean[6];
		hasPowerUpgrade = false;
		hasPower = true;
		dirty = true;
	}

	public boolean isRoutedInDir(Direction dir) {
		if (dir == null) {
			return false;
		}
		return isRoutedInDir[dir.ordinal()];
	}

	public boolean isSubPowerInDir(Direction dir) {
		if (dir == null) {
			return false;
		}
		return isSubPowerInDir[dir.ordinal()];
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clean() {
		dirty = false;
	}

	public void writeData(LPDataOutput output) {
		for (int iconIndexe : iconIndexes) {
			output.writeByte(iconIndexe);
		}
		output.writeInt(textureIndex);
		output.writeBoolean(isRouted);
		output.writeBooleanArray(isRoutedInDir);
		output.writeBooleanArray(isSubPowerInDir);
		output.writeBoolean(hasPowerUpgrade);
		output.writeBoolean(hasPower);
		output.writeBoolean(isFluid);
		output.writeFacing(pointedOrientation);
	}

	public void readData(LPDataInput input) {
		for (int i = 0; i < iconIndexes.length; i++) {
			int icon = input.readByte();
			if (iconIndexes[i] != icon) {
				iconIndexes[i] = icon;
				dirty = true;
			}
		}
		int newTextureIndex = input.readInt();
		if (newTextureIndex != textureIndex) {
			textureIndex = newTextureIndex;
			dirty = true;
		}
		boolean newIsRouted = input.readBoolean();
		boolean[] newIsRoutedInDir = input.readBooleanArray();
		boolean[] newIsSubPowerInDir = input.readBooleanArray();
		boolean newHasPowerUpgrade = input.readBoolean();
		boolean newHasPower = input.readBoolean();
		boolean newIsFluid = input.readBoolean();
		Direction newPointedOrientation = input.readFacing();
		// the pipe model is cached until dirty: power and routing overlays must invalidate it too
		if (newIsRouted != isRouted || !Arrays.equals(newIsRoutedInDir, isRoutedInDir) || !Arrays.equals(newIsSubPowerInDir, isSubPowerInDir)
				|| newHasPowerUpgrade != hasPowerUpgrade || newHasPower != hasPower || newIsFluid != isFluid || newPointedOrientation != pointedOrientation) {
			dirty = true;
		}
		isRouted = newIsRouted;
		isRoutedInDir = newIsRoutedInDir;
		isSubPowerInDir = newIsSubPowerInDir;
		hasPowerUpgrade = newHasPowerUpgrade;
		hasPower = newHasPower;
		isFluid = newIsFluid;
		pointedOrientation = newPointedOrientation;
	}
}
