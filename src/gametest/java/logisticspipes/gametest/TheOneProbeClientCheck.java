package logisticspipes.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.ITheOneProbe;

import network.rs485.logisticspipes.compat.TheOneProbeIntegration;

/** Client half of the TOP checks; only loaded when The One Probe is present. */
final class TheOneProbeClientCheck {

	private TheOneProbeClientCheck() {}

	/** Draws an LPText element directly; fails when RenderHelper.renderText was not resolved or drawing throws. */
	static String drawElement(Minecraft mc) throws ReflectiveOperationException {
		List<IElementFactory> factories = new ArrayList<>();
		ITheOneProbe probe = (ITheOneProbe) Proxy.newProxyInstance(ITheOneProbe.class.getClassLoader(), new Class<?>[] { ITheOneProbe.class },
				(proxy, method, args) -> {
					if (method.getName().equals("registerElementFactory")) factories.add((IElementFactory) args[0]);
					return null;
				});
		TheOneProbeIntegration integration = new TheOneProbeIntegration();
		integration.apply(probe);
		Field renderText = TheOneProbeIntegration.class.getDeclaredField("renderText");
		renderText.setAccessible(true);
		if (renderText.get(integration) == null) return "renderText not resolved";
		TheOneProbeIntegration.LPText text = integration.new LPText("top.logisticspipes.general.no_upgrades");
		GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
		text.render(graphics, 10, 10);
		graphics.flush();
		return text.getWidth() > 0 ? null : "width " + text.getWidth();
	}

	/** Stands the player on top of {@code pipe}, sneaking (extended mode), looking down, probe in hand, so TOP's own overlay shows the LP lines. */
	static void lookAtPipe(Minecraft mc, BlockPos pipe) {
		mc.options.keyShift.setDown(true);
		ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("theoneprobe", "probe")));
		double x = pipe.getX() + 0.5, y = pipe.getY() + 1, z = pipe.getZ() + 0.5;
		mc.getSingleplayerServer().execute(() -> {
			ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
			sp.setItemInHand(InteractionHand.MAIN_HAND, probe.copy());
			sp.connection.teleport(x, y, z, 0F, 90F);
		});
	}
}
