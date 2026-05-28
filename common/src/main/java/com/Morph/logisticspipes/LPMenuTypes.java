package com.Morph.logisticspipes;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import com.Morph.logisticspipes.gui.ChassisPipeMenu;
import com.Morph.logisticspipes.gui.PowerJunctionMenu;
import com.Morph.logisticspipes.gui.RequestPipeMenu;
import com.Morph.logisticspipes.gui.SupplierPipeMenu;

public final class LPMenuTypes {

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(LPConstants.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<RequestPipeMenu>> REQUEST_PIPE =
            MENU_TYPES.register("request_pipe",
                    () -> MenuRegistry.ofExtended(
                            (id, inv, buf) -> new RequestPipeMenu(id, inv, buf)));

    public static final RegistrySupplier<MenuType<ChassisPipeMenu>> CHASSIS_PIPE =
            MENU_TYPES.register("chassis_pipe",
                    () -> MenuRegistry.ofExtended(
                            (id, inv, buf) -> new ChassisPipeMenu(id, inv, buf)));

    public static final RegistrySupplier<MenuType<SupplierPipeMenu>> SUPPLIER_PIPE =
            MENU_TYPES.register("supplier_pipe",
                    () -> MenuRegistry.ofExtended(
                            (id, inv, buf) -> new SupplierPipeMenu(id, inv, buf)));

    public static final RegistrySupplier<MenuType<PowerJunctionMenu>> POWER_JUNCTION =
            MENU_TYPES.register("power_junction",
                    () -> MenuRegistry.ofExtended(
                            (id, inv, buf) -> new PowerJunctionMenu(id, inv, buf)));

    public static void register() {
        MENU_TYPES.register();
    }
}
