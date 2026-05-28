package com.Morph.logisticspipes;

import java.util.function.Supplier;

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
import com.Morph.logisticspipes.platform.Registrar;

public final class LPItems {

    private LPItems() {}

    public static Supplier<Item> PIPE_BASIC;
    public static Supplier<Item> PIPE_PROVIDER;
    public static Supplier<Item> PIPE_REQUEST;
    public static Supplier<Item> PIPE_SUPPLIER;
    public static Supplier<Item> PIPE_CHASSIS_MK1;
    public static Supplier<Item> PIPE_CHASSIS_MK2;
    public static Supplier<Item> PIPE_CHASSIS_MK3;
    public static Supplier<Item> PIPE_CHASSIS_MK4;
    public static Supplier<Item> PIPE_CHASSIS_MK5;
    public static Supplier<Item> POWER_JUNCTION;
    public static Supplier<Item> MODULE_ITEM_SINK;
    public static Supplier<Item> MODULE_PROVIDER;

    public static void init(Registrar r) {
        PIPE_BASIC       = r.registerItem("pipe_basic",       () -> pipeItem("pipe_basic"));
        PIPE_PROVIDER    = r.registerItem("pipe_provider",    () -> pipeItem("pipe_provider"));
        PIPE_REQUEST     = r.registerItem("pipe_request",     () -> pipeItem("pipe_request"));
        PIPE_SUPPLIER    = r.registerItem("pipe_supplier",    () -> pipeItem("pipe_supplier"));
        PIPE_CHASSIS_MK1 = r.registerItem("pipe_chassis_mk1", () -> pipeItem("pipe_chassis_mk1"));
        PIPE_CHASSIS_MK2 = r.registerItem("pipe_chassis_mk2", () -> pipeItem("pipe_chassis_mk2"));
        PIPE_CHASSIS_MK3 = r.registerItem("pipe_chassis_mk3", () -> pipeItem("pipe_chassis_mk3"));
        PIPE_CHASSIS_MK4 = r.registerItem("pipe_chassis_mk4", () -> pipeItem("pipe_chassis_mk4"));
        PIPE_CHASSIS_MK5 = r.registerItem("pipe_chassis_mk5", () -> pipeItem("pipe_chassis_mk5"));

        POWER_JUNCTION   = r.registerItem("power_junction",
                () -> new BlockItem(LPBlocks.POWER_JUNCTION_BLOCK.get(), new Item.Properties()));

        MODULE_ITEM_SINK = r.registerItem("module_item_sink", () -> new Item(new Item.Properties()));
        MODULE_PROVIDER  = r.registerItem("module_provider",  () -> new Item(new Item.Properties()));
    }

    /** BlockItem whose display name comes from its own item translation key, not the shared block key. */
    private static BlockItem pipeItem(String key) {
        String descId = "item." + LPConstants.MOD_ID + "." + key;
        return new BlockItem(LPBlocks.PIPE_BLOCK.get(), new Item.Properties()) {
            @Override public String getDescriptionId() { return descId; }
        };
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
