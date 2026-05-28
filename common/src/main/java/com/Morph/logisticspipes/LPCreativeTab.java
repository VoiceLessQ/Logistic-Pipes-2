package com.Morph.logisticspipes;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class LPCreativeTab {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(LPConstants.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> LP_TAB = TABS.register("lp_tab", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + LPConstants.MOD_ID))
                    .icon(() -> new ItemStack(LPItems.PIPE_BASIC.get()))
                    .displayItems((params, output) -> {
                        output.accept(LPItems.PIPE_BASIC.get());
                        output.accept(LPItems.PIPE_PROVIDER.get());
                        output.accept(LPItems.PIPE_REQUEST.get());
                        output.accept(LPItems.PIPE_SUPPLIER.get());
                        output.accept(LPItems.PIPE_CHASSIS_MK1.get());
                        output.accept(LPItems.PIPE_CHASSIS_MK2.get());
                        output.accept(LPItems.PIPE_CHASSIS_MK3.get());
                        output.accept(LPItems.PIPE_CHASSIS_MK4.get());
                        output.accept(LPItems.PIPE_CHASSIS_MK5.get());
                        output.accept(LPItems.MODULE_ITEM_SINK.get());
                        output.accept(LPItems.MODULE_PROVIDER.get());
                        output.accept(LPItems.POWER_JUNCTION.get());
                    })
                    .build());

    public static void register() {
        TABS.register();
    }
}
