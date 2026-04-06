package com.Morph.logisticspipes.pipes;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.modules.LogisticsModule;
import com.Morph.logisticspipes.modules.ModuleItemSink;
import com.Morph.logisticspipes.modules.ModuleProvider;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Base class for all chassis pipe types (Mk1-5).
 * Hosts module slots; delegates item sinking and providing to installed modules.
 * Ported from LP 1.12.2 PipeLogisticsChassis — simplified for Phase 5 (no upgrade manager, no CC).
 */
public abstract class PipeLogisticsChassis extends CoreRoutedPipe implements IProvideItems {

    protected final LogisticsModule[] modules;

    @Nullable
    private Direction pointedDirection = null;

    public PipeLogisticsChassis(Item item) {
        super(new PipeTransportLogistics(), item);
        modules = new LogisticsModule[getChassisSize()];
    }

    public abstract int getChassisSize();

    public void installModule(int slot, @Nullable LogisticsModule module) {
        if (slot < 0 || slot >= getChassisSize()) return;
        modules[slot] = module;
        if (module != null) module.setService(this);
    }

    @Nullable
    public LogisticsModule getModule(int slot) {
        if (slot < 0 || slot >= getChassisSize()) return null;
        return modules[slot];
    }

    @Nullable
    public Direction getPointedDirection() { return pointedDirection; }

    public void setPointedDirection(@Nullable Direction dir) { this.pointedDirection = dir; }

    // -------------------------------------------------------------------------
    // Sink query — used by routing to check if items can be delivered here
    // -------------------------------------------------------------------------

    @Nullable
    public SinkReply canSink(ItemIdentifier item) {
        SinkReply best = null;
        for (LogisticsModule module : modules) {
            if (module == null) continue;
            SinkReply reply = module.sinksItem(item);
            if (reply == null) continue;
            if (best == null
                    || reply.fixedPriority.ordinal() > best.fixedPriority.ordinal()
                    || (reply.fixedPriority == best.fixedPriority
                            && reply.customPriority > best.customPriority)) {
                best = reply;
            }
        }
        return best;
    }

    // -------------------------------------------------------------------------
    // IProvideItems — delegates to ModuleProvider modules
    // -------------------------------------------------------------------------

    @Override
    public void getAllItems(Map<ItemIdentifier, Integer> list) {
        for (LogisticsModule module : modules) {
            if (module instanceof IProvideItems provider) {
                provider.getAllItems(list);
            }
        }
    }

    @Override
    public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                                   IAdditionalTargetInformation info) {
        for (LogisticsModule module : modules) {
            if (module instanceof IProvideItems provider) {
                LogisticsOrder order = provider.fullFill(promise, destination, info);
                if (order != null) return order;
            }
        }
        return null;
    }

    @Override
    public IRouter getRouter() { return super.getRouter(); }

    // -------------------------------------------------------------------------
    // Tick — delegates to each module
    // -------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        super.updateEntity();
        for (LogisticsModule module : modules) {
            if (module != null) module.tick();
        }
    }

    // -------------------------------------------------------------------------
    // NBT — pipe type and module state
    // -------------------------------------------------------------------------

    @Override
    public void saveExtra(CompoundTag tag) {
        if (pointedDirection != null) {
            tag.putString("pointedDir", pointedDirection.getName());
        }
        for (int i = 0; i < modules.length; i++) {
            LogisticsModule m = modules[i];
            if (m == null) continue;
            tag.putString("module_" + i + "_type", moduleTypeName(m));
            m.saveToNBT(tag, "module_" + i + "_");
        }
    }

    @Override
    public void loadExtra(CompoundTag tag) {
        if (tag.contains("pointedDir")) {
            pointedDirection = Direction.byName(tag.getString("pointedDir"));
        }
        for (int i = 0; i < modules.length; i++) {
            String key = "module_" + i + "_type";
            if (!tag.contains(key)) continue;
            LogisticsModule m = createModuleByType(tag.getString(key));
            if (m != null) {
                installModule(i, m);
                m.loadFromNBT(tag, "module_" + i + "_");
            }
        }
    }

    private static String moduleTypeName(LogisticsModule m) {
        if (m instanceof ModuleItemSink) return "ModuleItemSink";
        if (m instanceof ModuleProvider) return "ModuleProvider";
        return m.getClass().getSimpleName();
    }

    @Nullable
    private static LogisticsModule createModuleByType(String type) {
        return switch (type) {
            case "ModuleItemSink" -> new ModuleItemSink();
            case "ModuleProvider" -> new ModuleProvider();
            default -> null;
        };
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) {
        return 3;
    }
}
