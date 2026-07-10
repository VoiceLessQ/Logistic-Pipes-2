package logisticspipes.items;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.DummyLevelProvider;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemModule extends LogisticsItem {

	private static class Module {

		private final Supplier<? extends LogisticsModule> moduleConstructor;
		private final Class<? extends LogisticsModule> moduleClass;

		private Module(Supplier<? extends LogisticsModule> moduleConstructor) {
			this.moduleConstructor = moduleConstructor;
			this.moduleClass = moduleConstructor.get().getClass();
		}

		private LogisticsModule getILogisticsModule() {
			if (moduleConstructor == null) {
				return null;
			}
			return moduleConstructor.get();
		}

		private Class<? extends LogisticsModule> getILogisticsModuleClass() {
			return moduleClass;
		}

	}

	private final Module moduleType;

	public ItemModule(Module moduleType) {
		super();
		this.moduleType = moduleType;
	}

	/** Factory for use with DeferredRegister. */
	public static ItemModule of(@Nonnull Supplier<? extends LogisticsModule> moduleConstructor) {
		return new ItemModule(new Module(moduleConstructor));
	}

	@Nullable
	public static LogisticsModule getLogisticsModule(@Nonnull Player player, int invSlot) {
		ItemStack item = player.getInventory().items.get(invSlot);
		if (item.isEmpty() || !(item.getItem() instanceof ItemModule)) return null;
		LogisticsModule module = ((ItemModule) item.getItem()).getModuleForItem(
				item, null, new DummyLevelProvider(player.level()), null
		);
		if (module == null) return null;
		module.registerPosition(ModulePositionType.IN_HAND, invSlot);
		ItemModuleInformationManager.readInformation(item, module);
		return module;
	}

	private void openConfigGui(@Nonnull ItemStack stack, Player player, Level world) {
		LogisticsModule module = getModuleForItem(stack, null, new DummyLevelProvider(world), null);
		if (module instanceof Gui && !stack.isEmpty()) {
			module.registerPosition(ModulePositionType.IN_HAND, player.getInventory().selected);
			ItemModuleInformationManager.readInformation(stack, module);
			Gui.getInHandGuiProvider((Gui) module).open(player);
		}
	}

	@Override
	public boolean isFoil(@Nonnull ItemStack stack) {
		LogisticsModule module = getModuleForItem(stack, null, null, null);
		if (module != null) {
			if (stack.getCount() > 0) {
				return module.hasEffect();
			}
		}
		return false;
	}

	@Override
	@Nonnull
	public InteractionResultHolder<ItemStack> use(final Level world, final Player player,
			@Nonnull final InteractionHand hand) {
		if (MainProxy.isServer(player.level())) {
			openConfigGui(player.getItemInHand(hand), player, world);
		}
		return super.use(world, player, hand);
	}

	@Override
	@Nonnull
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
		Player player = context.getPlayer();
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		InteractionHand hand = context.getHand();
		if (player != null && MainProxy.isServer(player.level())) {
			BlockEntity tile = world.getBlockEntity(pos);
			if (tile instanceof LogisticsTileGenericPipe) {
				if (player.getDisplayName().getString()
						.equals("ComputerCraft")) { // Allow turtle to place modules in pipes.
					CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);
					if (LogisticsBlockGenericPipe.isValid(pipe)) {
						pipe.blockActivated(player);
					}
				}
				return InteractionResult.PASS;
			}
			openConfigGui(player.getItemInHand(hand), player, world);
		}
		return InteractionResult.PASS;
	}

	@Nullable
	public LogisticsModule getModule(
			@Nullable LogisticsModule currentModule,
			@Nullable IWorldProvider world,
			@Nullable IPipeServiceProvider service
	) {
		if (currentModule != null) {
			if (moduleType.getILogisticsModuleClass().equals(currentModule.getClass())) {
				return currentModule;
			}
		}
		LogisticsModule newModule = moduleType.getILogisticsModule();
		if (newModule == null) {
			return null;
		}
		newModule.registerHandler(world, service);
		return newModule;
	}

	@Nullable
	public LogisticsModule getModuleForItem(
			@Nonnull ItemStack itemStack,
			@Nullable LogisticsModule currentModule,
			@Nullable IWorldProvider world,
			@Nullable IPipeServiceProvider service
	) {

		if (itemStack.isEmpty()) {
			return null;
		}
		if (itemStack.getItem() != this) {
			return null;
		}
		return getModule(currentModule, world, service);
	}

	@Override
	public String getModelSubdir() {
		return "module";
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, java.util.List<Component> tooltip,
			TooltipFlag flagIn) {
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			assert nbt != null;

			if (nbt.contains("informationList")) {
				if (Screen.hasShiftDown()) {
					ListTag nbttaglist = nbt.getList("informationList", 8);
					for (int i = 0; i < nbttaglist.size(); i++) {
						net.minecraft.nbt.Tag nbtTag = nbttaglist.get(i);
						String data = ((StringTag) nbtTag).getAsString();
						if (data.equals("<inventory>") && i + 1 < nbttaglist.size()) {
							nbtTag = nbttaglist.get(i + 1);
							data = ((StringTag) nbtTag).getAsString();
							if (data.startsWith("<that>")) {
								String prefix = data.substring(6);
								CompoundTag module = nbt.getCompound("moduleInformation");
								int size = module.getList(prefix + "items", module.getId()).size();
								if (module.contains(prefix + "itemsCount")) {
									size = module.getInt(prefix + "itemsCount");
								}
								ItemIdentifierInventory inv = new ItemIdentifierInventory(size,
										"InformationTempInventory", Integer.MAX_VALUE);
								inv.readFromNBT(module, prefix);
								for (int pos = 0; pos < inv.getContainerSize(); pos++) {
									ItemIdentifierStack identStack = inv.getIDStackInSlot(pos);
									if (identStack != null) {
										if (identStack.getStackSize() > 1) {
											tooltip.add(Component.literal("  " + identStack.getStackSize() + "x " + identStack.getFriendlyName()));
										} else {
											tooltip.add(Component.literal("  " + identStack.getFriendlyName()));
										}
									}
								}
							}
							i++;
						} else {
							tooltip.add(Component.literal(data));
						}
					}
				} else {
					TextUtil.addTooltipInformation(stack, tooltip, false);
				}
			} else {
				TextUtil.addTooltipInformation(stack, tooltip, Screen.hasShiftDown());
			}
		} else {
			TextUtil.addTooltipInformation(stack, tooltip, Screen.hasShiftDown());
		}
	}
}
