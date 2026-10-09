package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.interfaces.ILogisticsItem;

public class LogisticsSolidBlockItem extends BlockItem implements ILogisticsItem {

	/** meta → item map used by the legacy {@link logisticspipes.datafixer.DataFixerSolidBlockItems}. */
	public static final java.util.Map<Integer, LogisticsSolidBlockItem> updateItemMap = new java.util.HashMap<>();

	@Getter
	private final LogisticsSolidBlock.Type type;

	public LogisticsSolidBlockItem(LogisticsSolidBlock block) {
		super(block, new net.minecraft.world.item.Item.Properties());
		type = block.getType();
		updateItemMap.put(type.getMeta(), this);
	}

	@Override
	public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
		consumer.accept(ClientExtensionsHolder.EXTENSIONS);
	}

	/** Client-only BEWLR holder, loaded lazily so dedicated servers don't touch it. */
	private static final class ClientExtensionsHolder {
		static final net.minecraftforge.client.extensions.common.IClientItemExtensions EXTENSIONS =
			new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
				@Override
				public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
					return logisticspipes.renderer.LogisticsSolidBlockItemRenderer.instance();
				}
			};
	}
}
