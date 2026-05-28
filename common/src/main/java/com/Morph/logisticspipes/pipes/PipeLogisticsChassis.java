package com.Morph.logisticspipes.pipes;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.modules.LogisticsModule;
import com.Morph.logisticspipes.modules.ModuleRegistry;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Base class for chassis pipe types (Mk1-5).
 * Hosts module slots backed by a SimpleContainer; delegates sink/provide to installed modules.
 * Ported from LP 1.12.2 PipeLogisticsChassis — simplified for Phase 5/6.
 */
public abstract class PipeLogisticsChassis extends CoreRoutedPipe implements IProvideItems {

    /** Live module logic instances — derived from moduleContainer via listener. */
    protected final LogisticsModule[] modules;

    /** Item storage for GUI; changing items here installs/removes modules. */
    public final SimpleContainer moduleContainer;

    @Nullable
    private Direction pointedDirection = null;

    public PipeLogisticsChassis(Item item) {
        super(new PipeTransportLogistics(), item);
        int size = getChassisSize();
        modules = new LogisticsModule[size];
        moduleContainer = new SimpleContainer(size);
        moduleContainer.addListener(c -> syncModulesFromContainer());
    }

    public abstract int getChassisSize();

    // -------------------------------------------------------------------------
    // Module management
    // -------------------------------------------------------------------------

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

    private void syncModulesFromContainer() {
        for (int i = 0; i < getChassisSize(); i++) {
            ItemStack stack = moduleContainer.getItem(i);
            if (stack.isEmpty()) {
                if (modules[i] != null) installModule(i, null);
            } else {
                LogisticsModule existing = modules[i];
                LogisticsModule created = ModuleRegistry.createFor(stack.getItem());
                if (created != null
                        && (existing == null || !existing.getClass().equals(created.getClass()))) {
                    installModule(i, created);
                } else if (created == null && existing != null) {
                    installModule(i, null);
                }
            }
        }
    }

    @Nullable
    public Direction getPointedDirection() { return pointedDirection; }

    public void setPointedDirection(@Nullable Direction dir) { this.pointedDirection = dir; }

    // -------------------------------------------------------------------------
    // Sink query
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
    // IProvideItems
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
    // Tick
    // -------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        super.updateEntity();
        for (LogisticsModule module : modules) {
            if (module != null) module.tick();
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    public void saveExtra(CompoundTag tag) {
        if (pointedDirection != null) tag.putString("pointedDir", pointedDirection.getName());

        ListTag moduleItems = new ListTag();
        for (int i = 0; i < getChassisSize(); i++) {
            ItemStack stack = moduleContainer.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag slot = new CompoundTag();
            slot.putInt("slot", i);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null) slot.putString("item", key.toString());
            moduleItems.add(slot);
        }
        tag.put("moduleItems", moduleItems);

        for (int i = 0; i < getChassisSize(); i++) {
            LogisticsModule m = modules[i];
            if (m == null) continue;
            m.saveToNBT(tag, "module_" + i + "_");
        }
    }

    @Override
    public void loadExtra(CompoundTag tag) {
        if (tag.contains("pointedDir")) {
            pointedDirection = Direction.byName(tag.getString("pointedDir"));
        }
        if (tag.contains("moduleItems")) {
            ListTag list = tag.getList("moduleItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag slot = list.getCompound(i);
                int slotIndex = slot.getInt("slot");
                if (slotIndex < getChassisSize() && slot.contains("item")) {
                    Item item = BuiltInRegistries.ITEM.getOptional(
                            ResourceLocation.parse(slot.getString("item"))).orElse(null);
                    if (item != null) moduleContainer.setItem(slotIndex, new ItemStack(item));
                }
            }
        }
        for (int i = 0; i < getChassisSize(); i++) {
            LogisticsModule m = modules[i];
            if (m != null) m.loadFromNBT(tag, "module_" + i + "_");
        }
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) { return 3; }
}
