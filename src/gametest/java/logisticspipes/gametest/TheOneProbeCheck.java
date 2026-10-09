package logisticspipes.gametest;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;

import io.netty.buffer.Unpooled;
import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;

import network.rs485.logisticspipes.compat.TheOneProbeIntegration;

/** Only loaded when The One Probe is present. */
final class TheOneProbeCheck {

	private TheOneProbeCheck() {}

	/** Probes the pipe at {@code abs} and returns each element's key after a network round trip. */
	static List<String> probeKeys(GameTestHelper helper, BlockPos abs) {
		List<IProbeInfoProvider> providers = new ArrayList<>();
		List<IElementFactory> factories = new ArrayList<>();
		ITheOneProbe probe = (ITheOneProbe) Proxy.newProxyInstance(ITheOneProbe.class.getClassLoader(), new Class<?>[] { ITheOneProbe.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "registerProvider" -> providers.add((IProbeInfoProvider) args[0]);
						case "registerElementFactory" -> factories.add((IElementFactory) args[0]);
						default -> {}
					}
					return null;
				});
		new TheOneProbeIntegration().apply(probe);
		helper.assertTrue(providers.size() == 1 && factories.size() == 1, "registered " + providers + " " + factories);

		List<IElement> elements = new ArrayList<>();
		IProbeInfo info = (IProbeInfo) Proxy.newProxyInstance(IProbeInfo.class.getClassLoader(), new Class<?>[] { IProbeInfo.class },
				(proxy, method, args) -> {
					if (method.getName().equals("element")) elements.add((IElement) args[0]);
					return method.getReturnType() == IProbeInfo.class ? proxy : null;
				});
		IProbeHitData hit = (IProbeHitData) Proxy.newProxyInstance(IProbeHitData.class.getClassLoader(), new Class<?>[] { IProbeHitData.class },
				(proxy, method, args) -> method.getName().equals("getPos") ? abs : null);
		providers.get(0).addProbeInfo(ProbeMode.EXTENDED, info, null, helper.getLevel(), helper.getLevel().getBlockState(abs), hit);

		List<String> keys = new ArrayList<>();
		for (IElement element : elements) {
			helper.assertTrue(element.getID().equals(factories.get(0).getId()), "element id " + element.getID());
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(),
					net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
			element.toBytes(buf);
			IElement read = factories.get(0).createElement(buf);
			helper.assertTrue(buf.readableBytes() == 0, buf.readableBytes() + " bytes left after reading " + element);
			keys.add(((TheOneProbeIntegration.LPText) read).getKey());
		}
		return keys;
	}
}
