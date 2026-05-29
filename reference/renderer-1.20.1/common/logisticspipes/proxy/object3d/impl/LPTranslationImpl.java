package logisticspipes.proxy.object3d.impl;

import logisticspipes.proxy.object3d.interfaces.ITranslation;

/**
 * NeoForge 1.20.1 native replacement for CCL's Translation transform.
 */
public final class LPTranslationImpl implements ITranslation {

	public final double dx;
	public final double dy;
	public final double dz;

	public LPTranslationImpl(double dx, double dy, double dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
	}

	@Override
	public ITranslation inverse() {
		return new LPTranslationImpl(-dx, -dy, -dz);
	}

	@Override
	public Object getOriginal() {
		return this;
	}
}
