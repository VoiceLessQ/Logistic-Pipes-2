package com.Morph.fabric.platform;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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
 * Fabric {@link Registrar} — registers via vanilla {@link Registry#register} directly.
 *
 * Extended menus use {@link ExtendedScreenHandlerType} with a passthrough
 * {@link FriendlyByteBuf} codec, mirroring Architectury {@code MenuRegistry.ofExtended}
 * semantics: any bytes written on the server side are handed to the menu ctor on the
 * client side. Per-menu typed codecs are a future cleanup.
 */
public final class FabricRegistrar implements Registrar {

    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, name);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> factory) {
        T value = Registry.register(BuiltInRegistries.BLOCK, rl(name), factory.get());
        return () -> value;
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
        T value = Registry.register(BuiltInRegistries.ITEM, rl(name), factory.get());
        return () -> value;
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
            String name, BlockEntityFactory<T> factory, Supplier<? extends Block> block) {
        BlockEntityType<T> type = BlockEntityType.Builder.<T>of(factory::create, block.get()).build(null);
        BlockEntityType<T> registered = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl(name), type);
        return () -> registered;
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuExtended(
            String name, ExtendedMenuConstructor<T> ctor) {
        ExtendedScreenHandlerType<T, FriendlyByteBuf> type =
                new ExtendedScreenHandlerType<>(
                        (id, inv, data) -> ctor.create(id, inv, data),
                        PASSTHROUGH_BUF_CODEC);
        @SuppressWarnings("unchecked")
        MenuType<T> registered = (MenuType<T>) Registry.register(BuiltInRegistries.MENU, rl(name), type);
        return () -> registered;
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> factory) {
        CreativeModeTab value = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, rl(name), factory.get());
        return () -> value;
    }

    @Override
    public void commit() {
        // Vanilla registry calls are immediate — nothing to flush.
    }

    /** Passthrough codec — writes/reads the raw bytes of a FriendlyByteBuf. */
    private static final StreamCodec<RegistryFriendlyByteBuf, FriendlyByteBuf> PASSTHROUGH_BUF_CODEC =
            StreamCodec.of(
                    (target, data) -> {
                        int len = data.readableBytes();
                        target.writeVarInt(len);
                        target.writeBytes(data, data.readerIndex(), len);
                    },
                    source -> {
                        int len = source.readVarInt();
                        byte[] bytes = new byte[len];
                        source.readBytes(bytes);
                        return new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
                    });
}
