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
		super(block, logisticspipes.LPRegistries.blockItemProperties());
		type = block.getType();
		updateItemMap.put(type.getMeta(), this);
	}

}
