package com.Morph.logisticspipes.platform;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Platform-agnostic registration facade. Replaces the per-category Architectury
 * {@code DeferredRegister} usage in common code.
 *
 * Implementations:
 *   - Fabric:    {@code com.Morph.fabric.platform.FabricRegistrar}
 *   - NeoForge:  {@code com.Morph.neoforge.platform.NeoForgeRegistrar}
 *
 * Each {@code register*} call returns a {@link Supplier} that resolves to the
 * registered instance after {@link #commit()} has been called (NeoForge needs
 * an event-driven flush; Fabric is immediate but the supplier indirection keeps
 * the call sites uniform).
 *
 * Usage pattern (common-side):
 * <pre>
 *   public static Supplier&lt;LogisticsPipeBlock&gt; PIPE_BLOCK;
 *
 *   public static void init(Registrar r) {
 *       PIPE_BLOCK = r.registerBlock("pipe", () -&gt; new LogisticsPipeBlock(props));
 *   }
 * </pre>
 *
 * The platform mod-init class instantiates its Registrar, runs every
 * {@code LP*.init(registrar)} (order matters where one references another's
 * supplier), then calls {@link #commit()} once at the end.
 */
public interface Registrar {

    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> factory);

    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory);

    /**
     * Register a block entity type. The factory + block-supplier shape is required
     * because Architectury's common compileClasspath does not expose
     * {@code BlockEntityType.Builder.of} (its SAM type
     * {@code BlockEntityType.BlockEntitySupplier} is hidden as platform-specific),
     * so the actual {@code BlockEntityType} has to be constructed by the platform impl.
     *
     * Pass the block as a {@link Supplier} so registration order doesn't matter —
     * the platform impl resolves it when actually building the type.
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
            String name, BlockEntityFactory<T> factory, Supplier<? extends Block> block);

    /** SAM mirror of {@code BlockEntityType.BlockEntitySupplier} for common-side type-naming. */
    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    /**
     * Registers a menu type that supports an extra {@link FriendlyByteBuf}
     * passed at open time (LP1's pattern for shipping context to a GUI without
     * an extra packet). The ctor receives the synced buf written by
     * {@link MenuOpener#openExtendedMenu}.
     */
    <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuExtended(
            String name, ExtendedMenuConstructor<T> ctor);

    Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> factory);

    /** Called once after all register* calls. Platforms with deferred registration flush here. */
    void commit();

    /** Factory that builds an extended-buf menu instance. */
    @FunctionalInterface
    interface ExtendedMenuConstructor<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inv, FriendlyByteBuf buf);
    }
}
