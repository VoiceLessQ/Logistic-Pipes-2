package com.Morph.logisticspipes;

import java.util.function.Supplier;

import net.minecraft.world.inventory.MenuType;

import com.Morph.logisticspipes.gui.ChassisPipeMenu;
import com.Morph.logisticspipes.gui.PowerJunctionMenu;
import com.Morph.logisticspipes.gui.RequestPipeMenu;
import com.Morph.logisticspipes.gui.SupplierPipeMenu;
import com.Morph.logisticspipes.platform.Registrar;

public final class LPMenuTypes {

    private LPMenuTypes() {}

    public static Supplier<MenuType<RequestPipeMenu>>   REQUEST_PIPE;
    public static Supplier<MenuType<ChassisPipeMenu>>   CHASSIS_PIPE;
    public static Supplier<MenuType<SupplierPipeMenu>>  SUPPLIER_PIPE;
    public static Supplier<MenuType<PowerJunctionMenu>> POWER_JUNCTION;

    public static void init(Registrar r) {
        REQUEST_PIPE   = r.registerMenuExtended("request_pipe",   RequestPipeMenu::new);
        CHASSIS_PIPE   = r.registerMenuExtended("chassis_pipe",   ChassisPipeMenu::new);
        SUPPLIER_PIPE  = r.registerMenuExtended("supplier_pipe",  SupplierPipeMenu::new);
        POWER_JUNCTION = r.registerMenuExtended("power_junction", PowerJunctionMenu::new);
    }
}
