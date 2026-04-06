package com.Morph.logisticspipes;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.modules.ModuleItemSink;
import com.Morph.logisticspipes.modules.ModuleProvider;
import com.Morph.logisticspipes.modules.ModuleRegistry;
import com.Morph.logisticspipes.pipes.PipeItemsBasicLogistics;
import com.Morph.logisticspipes.pipes.PipeItemsProviderLogistics;
import com.Morph.logisticspipes.pipes.PipeItemsRequestLogistics;
import com.Morph.logisticspipes.pipes.PipeItemsSupplierLogistics;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassisMk1;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassisMk2;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassisMk3;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassisMk4;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassisMk5;
import com.Morph.logisticspipes.pipes.PipeRegistry;

public final class LPItems {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(LPConstants.MOD_ID, Registries.ITEM);

    // Pipe items
    public static final RegistrySupplier<Item> PIPE_BASIC =
            ITEMS.register("pipe_basic", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_PROVIDER =
            ITEMS.register("pipe_provider", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_REQUEST =
            ITEMS.register("pipe_request", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_SUPPLIER =
            ITEMS.register("pipe_supplier", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_CHASSIS_MK1 =
            ITEMS.register("pipe_chassis_mk1", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_CHASSIS_MK2 =
            ITEMS.register("pipe_chassis_mk2", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_CHASSIS_MK3 =
            ITEMS.register("pipe_chassis_mk3", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_CHASSIS_MK4 =
            ITEMS.register("pipe_chassis_mk4", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PIPE_CHASSIS_MK5 =
            ITEMS.register("pipe_chassis_mk5", () -> new BlockItem(
                    LPBlocks.PIPE_BLOCK.get(), new Item.Properties()));

    // Module items (non-placeable)
    public static final RegistrySupplier<Item> MODULE_ITEM_SINK =
            ITEMS.register("module_item_sink", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MODULE_PROVIDER =
            ITEMS.register("module_provider", () -> new Item(new Item.Properties()));

    public static void register() {
        ITEMS.register();
    }

    /** Called after registry freeze to wire PipeRegistry and ModuleRegistry factories. */
    public static void registerPipeFactories() {
        PipeRegistry.register(PIPE_BASIC.get(),
                () -> new PipeItemsBasicLogistics(PIPE_BASIC.get()));
        PipeRegistry.register(PIPE_PROVIDER.get(),
                () -> new PipeItemsProviderLogistics(PIPE_PROVIDER.get()));
        PipeRegistry.register(PIPE_REQUEST.get(),
                () -> new PipeItemsRequestLogistics(PIPE_REQUEST.get()));
        PipeRegistry.register(PIPE_SUPPLIER.get(),
                () -> new PipeItemsSupplierLogistics(PIPE_SUPPLIER.get()));
        PipeRegistry.register(PIPE_CHASSIS_MK1.get(),
                () -> new PipeLogisticsChassisMk1(PIPE_CHASSIS_MK1.get()));
        PipeRegistry.register(PIPE_CHASSIS_MK2.get(),
                () -> new PipeLogisticsChassisMk2(PIPE_CHASSIS_MK2.get()));
        PipeRegistry.register(PIPE_CHASSIS_MK3.get(),
                () -> new PipeLogisticsChassisMk3(PIPE_CHASSIS_MK3.get()));
        PipeRegistry.register(PIPE_CHASSIS_MK4.get(),
                () -> new PipeLogisticsChassisMk4(PIPE_CHASSIS_MK4.get()));
        PipeRegistry.register(PIPE_CHASSIS_MK5.get(),
                () -> new PipeLogisticsChassisMk5(PIPE_CHASSIS_MK5.get()));

        ModuleRegistry.register(MODULE_ITEM_SINK.get(), ModuleItemSink::new);
        ModuleRegistry.register(MODULE_PROVIDER.get(), ModuleProvider::new);
    }
}
