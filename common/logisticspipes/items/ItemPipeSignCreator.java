package logisticspipes.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.proxy.MainProxy;

public class ItemPipeSignCreator extends LogisticsItem {

	public static final List<Class<? extends IPipeSign>> signTypes = new ArrayList<>();

	//private TextureAtlasSprite[] itemIcon = new TextureAtlasSprite[2];

	public ItemPipeSignCreator() {
		super(new Item.Properties().stacksTo(1).durability(250));
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack) {
		return false;
	}

	@Nonnull
	@Override
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		BlockPos pos = _ctx.getClickedPos();
		Direction facing = _ctx.getClickedFace();
		if (MainProxy.isClient(world)) {
			return InteractionResult.FAIL;
		}
		ItemStack itemStack = player.getMainHandItem();
		if (itemStack.isEmpty() || itemStack.getDamageValue() > itemStack.getMaxDamage()) {
			return InteractionResult.FAIL;
		}
		BlockEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof LogisticsTileGenericPipe)) {
			return InteractionResult.FAIL;
		}

		CompoundTag itemTag = logisticspipes.utils.item.StackTag.hasTag(itemStack) ? logisticspipes.utils.item.StackTag.getTag(itemStack) : new CompoundTag();
		itemTag.putInt("PipeClicked", 0);
		logisticspipes.utils.item.StackTag.setTag(itemStack, itemTag);

		int mode = itemTag.getInt("CreatorMode");

		if (facing == null) {
			return InteractionResult.FAIL;
		}

		if (!(((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe)) {
			return InteractionResult.FAIL;
		}

		CoreRoutedPipe pipe = (CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe;
		if (pipe == null) {
			return InteractionResult.FAIL;
		}
		if (!player.isCrouching()) {
			if (pipe.hasPipeSign(facing)) {
				pipe.activatePipeSign(facing, player);
				return InteractionResult.SUCCESS;
			} else if (mode >= 0 && mode < ItemPipeSignCreator.signTypes.size()) {
				Class<? extends IPipeSign> signClass = ItemPipeSignCreator.signTypes.get(mode);
				try {
					IPipeSign sign = signClass.newInstance();
					if (sign.isAllowedFor(pipe)) {
						itemStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
						sign.addSignTo(pipe, facing, player);
						return InteractionResult.SUCCESS;
					} else {
						return InteractionResult.FAIL;
					}
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			if (pipe.hasPipeSign(facing)) {
				pipe.removePipeSign(facing, player);
				itemStack.hurtAndBreak(-1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			}
			return InteractionResult.SUCCESS;
		}
	}

	// getMetadata removed in 1.20.1 — item variants handled differently
	public int getMetadata(@Nonnull ItemStack stack) {
		if (stack.isEmpty() || !logisticspipes.utils.item.StackTag.hasTag(stack)) return 0;
		int mode = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack)).getInt("CreatorMode");
		return Math.min(mode, ItemPipeSignCreator.signTypes.size() - 1);
	}

	@Override
	public int getModelCount() {
		return signTypes.size();
	}

	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(final Level world, final Player player, @Nonnull final InteractionHand hand) {
		ItemStack stack = player.getMainHandItem();
		if (MainProxy.isClient(world)) {
			return InteractionResultHolder.pass(stack);
		}
		if (player.isCrouching()) {
			if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
				logisticspipes.utils.item.StackTag.setTag(stack, new CompoundTag());
			}
			CompoundTag cycleTag = logisticspipes.utils.item.StackTag.getTag(stack);
			if (cycleTag != null && !cycleTag.contains("PipeClicked")) {
				int mode = cycleTag.getInt("CreatorMode");
				mode++;
				if (mode >= ItemPipeSignCreator.signTypes.size()) {
					mode = 0;
				}
				cycleTag.putInt("CreatorMode", mode);
				logisticspipes.utils.item.StackTag.setTag(stack, cycleTag);
			}
		}
		if (logisticspipes.utils.item.StackTag.hasTag(stack)) {
			CompoundTag clickTag = logisticspipes.utils.item.StackTag.getTag(stack);
			clickTag.remove("PipeClicked");
			logisticspipes.utils.item.StackTag.setTag(stack, clickTag);
		}
		return InteractionResultHolder.success(stack);
	}

	public static void registerPipeSignTypes() {
		// Never change this order. It defines the id each signType has.
		ItemPipeSignCreator.signTypes.add(CraftingPipeSign.class);
		ItemPipeSignCreator.signTypes.add(ItemAmountPipeSign.class);
	}
}
