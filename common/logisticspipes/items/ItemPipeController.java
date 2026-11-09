package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.LogisticsPlayerSettingsGuiProvider;
import logisticspipes.proxy.MainProxy;

public class ItemPipeController extends LogisticsItem {

	public ItemPipeController() {
		super();
	}

	@Nonnull
	@Override
	public InteractionResult use(Level world, Player player, @Nonnull InteractionHand handIn) {
		ItemStack stack = player.getItemInHand(handIn);
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		useItem(player, world);
		return InteractionResult.SUCCESS;
	}

	@Nonnull
	@Override
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		useItem(player, world);
		return InteractionResult.SUCCESS;
	}

	private void useItem(Player player, Level world) {
		NewGuiHandler.getGui(LogisticsPlayerSettingsGuiProvider.class).open(player);
	}
}
