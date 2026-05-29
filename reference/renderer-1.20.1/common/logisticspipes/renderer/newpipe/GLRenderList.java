package logisticspipes.renderer.newpipe;

// import net.minecraft.client.renderer.GLAllocation; // removed — display lists not available



public class GLRenderList {

	private final int listID = 0; // TODO: GLAllocation removed in 1.20.1 — display lists not available; replace with vertex buffers
	public boolean isValid = true;
	private long lastUsed = System.currentTimeMillis();
	private boolean isFilled = false;

	public int getID() {
		return listID;
	}

	public void startListCompile() {
		// no-op: GL display lists removed in 1.20.1; replace with vertex buffers when rendering is ported
	}

	public void stopCompile() {
		// no-op: GL display lists removed in 1.20.1
		isFilled = true;
	}

	public void render() {
		// no-op: GL display lists removed in 1.20.1; replace with vertex buffers when rendering is ported
		lastUsed = System.currentTimeMillis();
	}

	public boolean check() {
		if (!isValid) {
			return true;
		}
		if (lastUsed + 1000 * 60 * 1 < System.currentTimeMillis()) {
			isValid = false;
			return false;
		}
		return true;
	}

	public boolean isInvalid() {
		return !isValid;
	}

	public boolean isFilled() {
		return isFilled;
	}
}
