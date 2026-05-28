package com.Morph.neoforge.platform;

import java.util.function.Supplier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.platform.Registrar;

/**
 * NeoForge {@link Registrar} — backs each category with a {@link DeferredRegister}
 * whose contents flush during the {@code RegisterEvent} fired by the mod event bus.
 * The platform mod-init class must call {@link #attachTo(IEventBus)} before any
 * {@code register*} calls so the DeferredRegisters are bound to the right bus.
 *
 * {@link #commit()} is a no-op — NeoForge flushes on its own event lifecycle once
 * the bus has fired the {@code RegisterEvent} pass.
 */
public final class NeoForgeRegistrar implements Registrar {

    private final DeferredRegister<Block>                 BLOCKS   = DeferredRegister.create(BuiltInRegistries.BLOCK,             ExampleMod.MOD_ID);
    private final DeferredRegister<Item>                  ITEMS    = DeferredRegister.create(BuiltInRegistries.ITEM,              ExampleMod.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>>    BES      = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ExampleMod.MOD_ID);
    private final DeferredRegister<MenuType<?>>           MENUS    = DeferredRegister.create(BuiltInRegistries.MENU,              ExampleMod.MOD_ID);
    private final DeferredRegister<CreativeModeTab>       TABS     = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,         ExampleMod.MOD_ID);

    /** Attach all DeferredRegisters to the supplied mod event bus. Call before any register*. */
    public void attachTo(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> factory) {
        DeferredHolder<Block, T> h = BLOCKS.register(name, factory);
        return h::get;
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
        DeferredHolder<Item, T> h = ITEMS.register(name, factory);
        return h::get;
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
            String name, BlockEntityFactory<T> factory, Supplier<? extends Block> block) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> h =
                (DeferredHolder) BES.register(name, () ->
                        BlockEntityType.Builder.<T>of(factory::create, block.get()).build(null));
        return h::get;
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuExtended(
            String name, ExtendedMenuConstructor<T> ctor) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        DeferredHolder<MenuType<?>, MenuType<T>> h = (DeferredHolder) MENUS.register(name,
                () -> IMenuTypeExtension.create((id, inv, buf) -> ctor.create(id, inv, buf)));
        return h::get;
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> factory) {
        DeferredHolder<CreativeModeTab, CreativeModeTab> h = TABS.register(name, factory);
        return h::get;
    }

    @Override
    public void commit() {
        // No-op — NeoForge flushes on RegisterEvent dispatch after mod ctor returns.
    }
}
