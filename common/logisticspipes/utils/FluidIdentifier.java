package logisticspipes.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.nbt.CompoundTag;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import lombok.AllArgsConstructor;

import logisticspipes.asm.addinfo.IAddInfo;
import logisticspipes.asm.addinfo.IAddInfoProvider;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidIdentifier implements Comparable<FluidIdentifier>, ILPCCTypeHolder {

	private final Object[] ccTypeHolder = new Object[1];
	private final static ReadWriteLock dblock = new ReentrantReadWriteLock();
	private final static Lock rlock = FluidIdentifier.dblock.readLock();
	private final static Lock wlock = FluidIdentifier.dblock.writeLock();

	//map uniqueID -> FluidIdentifier
	private final static HashMap<Integer, FluidIdentifier> _fluidIdentifierIdCache = new HashMap<>(256, 0.5f);

	//for fluids with tags, map fluidID -> map tag -> FluidIdentifier
	private final static Map<String, HashMap<FinalCompoundTag, FluidIdentifier>> _fluidIdentifierTagCache = new HashMap<>(256);

	//for fluids without tags, map fluidID -> FluidIdentifier
	private final static Map<String, FluidIdentifier> _fluidIdentifierCache = new HashMap<>(256);

	public final String fluidID;
	public final String name;
	public final FinalCompoundTag tag;
	public final int uniqueID;

	@Override
	public int compareTo(FluidIdentifier o) {
		int c = fluidID.compareTo(o.fluidID);
		if (c != 0) {
			return c;
		}
		c = uniqueID - o.uniqueID;
		return c;
	}

	@AllArgsConstructor
	private static class FluidStackAddInfo implements IAddInfo {

		private final FluidIdentifier fluid;
	}

	@AllArgsConstructor
	private static class FluidAddInfo implements IAddInfo {

		private final FluidIdentifier fluid;
	}

	private FluidIdentifier(String fluidID, String name, FinalCompoundTag tag, int uniqueID) {
		this.fluidID = fluidID;
		this.name = name;
		this.tag = tag;
		this.uniqueID = uniqueID;
	}

	private static String getFluidName(Fluid fluid) {
		ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
		return key != null ? key.toString() : "unknown";
	}

	public static FluidIdentifier get(Fluid fluid, CompoundTag tag, FluidIdentifier proposal) {
		String fluidID = getFluidName(fluid);
		if (tag == null) {
			if (proposal != null) {
				if (proposal.fluidID.equals(fluidID) && proposal.tag == null) {
					return proposal;
				}
			}
			proposal = null;
			IAddInfoProvider prov = null;
			if (fluid instanceof IAddInfoProvider) {
				prov = (IAddInfoProvider) fluid;
				FluidAddInfo info = prov.getLogisticsPipesAddInfo(FluidAddInfo.class);
				if (info != null) {
					proposal = info.fluid;
				}
			}
			FluidIdentifier ident = getFluidIdentifierWithoutTag(fluid, fluidID, proposal);
			if (proposal != ident && prov != null) {
				prov.setLogisticsPipesAddInfo(new FluidAddInfo(ident));
			}
			return ident;
		} else {
			FluidIdentifier.rlock.lock();
			{
				HashMap<FinalCompoundTag, FluidIdentifier> fluidNBTList = FluidIdentifier._fluidIdentifierTagCache.get(fluidID);
				if (fluidNBTList != null) {
					FinalCompoundTag tagwithfixedname = new FinalCompoundTag(tag);
					FluidIdentifier unknownFluid = fluidNBTList.get(tagwithfixedname);
					if (unknownFluid != null) {
						FluidIdentifier.rlock.unlock();
						return unknownFluid;
					}
				}
			}
			FluidIdentifier.rlock.unlock();
			FluidIdentifier.wlock.lock();
			{
				HashMap<FinalCompoundTag, FluidIdentifier> fluidNBTList = FluidIdentifier._fluidIdentifierTagCache.get(fluidID);
				if (fluidNBTList != null) {
					FinalCompoundTag tagwithfixedname = new FinalCompoundTag(tag);
					FluidIdentifier unknownFluid = fluidNBTList.get(tagwithfixedname);
					if (unknownFluid != null) {
						FluidIdentifier.wlock.unlock();
						return unknownFluid;
					}
				}
			}
			HashMap<FinalCompoundTag, FluidIdentifier> fluidNBTList = FluidIdentifier._fluidIdentifierTagCache
					.computeIfAbsent(fluidID, k -> new HashMap<>(16, 0.5f));
			FinalCompoundTag finaltag = new FinalCompoundTag(tag);
			int id = FluidIdentifier.getUnusedId();
			FluidIdentifier unknownFluid = new FluidIdentifier(fluidID, getFluidName(fluid), finaltag, id);
			fluidNBTList.put(finaltag, unknownFluid);
			FluidIdentifier._fluidIdentifierIdCache.put(id, unknownFluid);
			FluidIdentifier.wlock.unlock();
			return (unknownFluid);
		}
	}

	private static FluidIdentifier getFluidIdentifierWithoutTag(Fluid fluid, String fluidID, FluidIdentifier proposal) {
		if (proposal != null) {
			if (proposal.fluidID.equals(fluidID) && proposal.tag == null) {
				return proposal;
			}
		}
		FluidIdentifier.rlock.lock();
		{
			FluidIdentifier unknownFluid = FluidIdentifier._fluidIdentifierCache.get(fluidID);
			if (unknownFluid != null) {
				FluidIdentifier.rlock.unlock();
				return unknownFluid;
			}
		}
		FluidIdentifier.rlock.unlock();
		FluidIdentifier.wlock.lock();
		{
			FluidIdentifier unknownFluid = FluidIdentifier._fluidIdentifierCache.get(fluidID);
			if (unknownFluid != null) {
				FluidIdentifier.wlock.unlock();
				return unknownFluid;
			}
		}
		int id = FluidIdentifier.getUnusedId();
		FluidIdentifier unknownFluid = new FluidIdentifier(fluidID, getFluidName(fluid), null, id);
		FluidIdentifier._fluidIdentifierCache.put(fluidID, unknownFluid);
		FluidIdentifier._fluidIdentifierIdCache.put(id, unknownFluid);
		FluidIdentifier.wlock.unlock();
		return (unknownFluid);
	}

	public static FluidIdentifier get(FluidStack stack) {
		if (stack == null) {
			return null;
		}
		FluidIdentifier proposal = null;
		IAddInfoProvider prov = null;
		if (stack instanceof IAddInfoProvider) {
			prov = (IAddInfoProvider) stack;
			FluidStackAddInfo info = prov.getLogisticsPipesAddInfo(FluidStackAddInfo.class);
			if (info != null) {
				proposal = info.fluid;
			}
		}
		FluidIdentifier ident = FluidIdentifier.get(stack.getFluid(), stack.getTag(), proposal);
		if (proposal != ident && stack.getTag() == null && prov != null) {
			prov.setLogisticsPipesAddInfo(new FluidStackAddInfo(ident));
		}
		return ident;
	}

	public static FluidIdentifier get(ItemIdentifier stack) {
		return FluidIdentifier.get(stack.makeStack(1));
	}

	public static FluidIdentifier get(@Nonnull ItemStack stack) {
		return FluidIdentifier.get(ItemIdentifierStack.getFromStack(stack));
	}

	public static FluidIdentifier get(ItemIdentifierStack stack) {
		FluidStack f = null;
		FluidIdentifierStack fstack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(stack);
		if (fstack != null) {
			f = fstack.makeFluidStack();
		}
		if (f == null) {
			ItemStack itemStack = stack.unsafeMakeNormalStack();
			IFluidHandlerItem capability = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
			if (capability != null) {
				{
					f = IntStream.range(0, capability.getTanks())
							.mapToObj(capability::getFluidInTank)
							.filter(s -> !s.isEmpty())
							.findFirst()
							.orElse(null);
				}
			}
		}
		if (f == null) {
			f = FluidUtil.getFluidContained(stack.unsafeMakeNormalStack()).orElse(null);
		}
		if (f == null) {
			return null;
		}
		return FluidIdentifier.get(f);
	}

	private static FluidIdentifier get(Fluid fluid) {
		return FluidIdentifier.get(fluid, null, null);
	}

	private static int getUnusedId() {
		int id = new Random().nextInt();
		while (FluidIdentifier.isIdUsed(id)) {
			id = new Random().nextInt();
		}
		return id;
	}

	private static boolean isIdUsed(int id) {
		return FluidIdentifier._fluidIdentifierIdCache.containsKey(id);
	}

	public String getName() {
		return name;
	}

	public FluidStack makeFluidStack(int amount) {
		// In 1.20, FluidStack(Fluid, int, CompoundTag) was removed — use setTag()
		FluidStack fs = new FluidStack(getFluid(), amount);
		if (tag != null) fs.setTag(tag.copy());
		return fs;
	}

	public FluidIdentifierStack makeFluidIdentifierStack(int amount) {
		//FluidStack constructor does the tag.copy(), so this is safe
		return new FluidIdentifierStack(this, amount);
	}

	public Fluid getFluid() {
		return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidID));
	}

	public int getFreeSpaceInsideTank(IFluidHandler tank) {
		FluidStack liquid = tank.getFluidInTank(0);
		if (liquid == null || liquid.getFluid() == null) {
			return tank.getTankCapacity(0);
		}
		if (FluidIdentifier.get(liquid).equals(this)) {
			return tank.getTankCapacity(0) - liquid.getAmount();
		}
		return 0;
	}

	private static boolean init = false;

	public static void initFromForge(boolean flag) {
		if (FluidIdentifier.init) {
			return;
		}
		net.minecraft.core.registries.BuiltInRegistries.FLUID.forEach(FluidIdentifier::get);
		if (flag) {
			FluidIdentifier.init = true;
		}
	}

	@Override
	public String toString() {
		String t = tag != null ? tag.toString() : "null";
		return name + "/" + fluidID + ":" + t;
	}

	public FluidIdentifier next() {
		FluidIdentifier.rlock.lock();
		boolean takeNext = false;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (takeNext && i != null) {
				FluidIdentifier.rlock.unlock();
				return i;
			}
			if (equals(i)) {
				takeNext = true;
			}
		}
		FluidIdentifier.rlock.unlock();
		return null;
	}

	public FluidIdentifier prev() {
		FluidIdentifier.rlock.lock();
		FluidIdentifier last = null;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (equals(i)) {
				FluidIdentifier.rlock.unlock();
				return last;
			}
			if (i != null) {
				last = i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return last;
	}

	public static FluidIdentifier first() {
		FluidIdentifier.rlock.lock();
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (i != null) {
				FluidIdentifier.rlock.unlock();
				return i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return null;
	}

	public static FluidIdentifier last() {
		FluidIdentifier.rlock.lock();
		FluidIdentifier last = null;
		for (FluidIdentifier i : FluidIdentifier._fluidIdentifierCache.values()) {
			if (i != null) {
				last = i;
			}
		}
		FluidIdentifier.rlock.unlock();
		return last;
	}

	public static Collection<FluidIdentifier> all() {
		FluidIdentifier.rlock.lock();
		Collection<FluidIdentifier> list = Collections.unmodifiableCollection(FluidIdentifier._fluidIdentifierCache.values());
		FluidIdentifier.rlock.unlock();
		return list;
	}

	public ItemIdentifier getItemIdentifier() {
		return SimpleServiceLocator.logisticsFluidManager.getFluidContainer(this.makeFluidIdentifierStack(1)).getItem();
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

}
