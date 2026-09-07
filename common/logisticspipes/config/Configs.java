package logisticspipes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

//@formatter:off
//CHECKSTYLE:OFF

public class Configs {

	public static final String CATEGORY_MULTITHREAD = "multithread";
	public static final String CATEGORY_PERFORMANCE = "performance";

	// ── ModConfigSpec ────────────────────────────────────────────────────────
	public static final ModConfigSpec SPEC;

	// Value holders (package-private, read in load())
	static final ModConfigSpec.IntValue     DETECTION_LENGTH_V;
	static final ModConfigSpec.IntValue     DETECTION_COUNT_V;
	static final ModConfigSpec.IntValue     DETECTION_FREQUENCY_V;
	static final ModConfigSpec.BooleanValue ORDERER_COUNT_INVERT_V;
	static final ModConfigSpec.BooleanValue ORDERER_PAGE_INVERT_V;
	static final ModConfigSpec.BooleanValue DISPLAY_POPUP_V;
	static final ModConfigSpec.IntValue     MAX_UNROUTED_CONNECTIONS_V;
	static final ModConfigSpec.IntValue     HUD_RENDER_DISTANCE_V;
	static final ModConfigSpec.DoubleValue  PIPE_DURABILITY_V;
	static final ModConfigSpec.BooleanValue POWER_USAGE_DISABLED_V;
	static final ModConfigSpec.DoubleValue  POWER_USAGE_MULTIPLIER_V;
	static final ModConfigSpec.DoubleValue  COMPILER_SPEED_V;
	static final ModConfigSpec.BooleanValue ENABLE_RESEARCH_SYSTEM_V;
	static final ModConfigSpec.IntValue     CRAFTING_TABLE_POWER_USAGE_V;
	static final ModConfigSpec.BooleanValue TOOLTIP_INFO_V;
	static final ModConfigSpec.BooleanValue ENABLE_PARTICLE_FX_V;
	static final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES_V;
	static final ModConfigSpec.BooleanValue EASTER_EGGS_V;
	static final ModConfigSpec.BooleanValue OPAQUE_V;
	static final ModConfigSpec.IntValue     MAX_ROBOT_DISTANCE_V;
	static final ModConfigSpec.IntValue     MULTI_THREAD_NUMBER_V;
	static final ModConfigSpec.IntValue     MULTI_THREAD_PRIORITY_V;
	static final ModConfigSpec.BooleanValue DISABLE_ASYNC_WORK_V;
	static final ModConfigSpec.IntValue     MIN_SLOT_ACCESS_V;
	static final ModConfigSpec.IntValue     MAX_SLOT_ACCESS_V;
	static final ModConfigSpec.IntValue     MIN_JOB_TICK_LENGTH_V;
	static final ModConfigSpec.EnumValue<PowerSourceMode> POWER_SOURCE_MODE_V;
	static final ModConfigSpec.BooleanValue BETA_UPGRADE_RECIPES_V;

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();

		b.comment("Pipe network detection settings").push("detection");
		DETECTION_LENGTH_V         = b.comment("Max detection length for pipe network scan").defineInRange("detectionLength", 50, 1, 1000);
		DETECTION_COUNT_V          = b.comment("Max pipes counted during detection").defineInRange("detectionCount", 100, 1, 10000);
		DETECTION_FREQUENCY_V      = b.comment("Detection frequency in ticks").defineInRange("detectionFrequency", 20 * 30, 1, Integer.MAX_VALUE);
		MAX_UNROUTED_CONNECTIONS_V = b.comment("Max unrouted connections per pipe").defineInRange("maxUnroutedConnections", 32, 1, 512);
		b.pop();

		b.comment("Orderer GUI settings").push("orderer");
		ORDERER_COUNT_INVERT_V = b.comment("Invert mouse wheel for item count in orderer").define("invertCountWheel", false);
		ORDERER_PAGE_INVERT_V  = b.comment("Invert mouse wheel for page navigation in orderer").define("invertPageWheel", false);
		DISPLAY_POPUP_V        = b.comment("Set the default configuration for the popup of the Orderer Gui. Should it be used?").define("displayPopup", true);
		b.pop();

		b.comment("HUD settings").push("hud");
		HUD_RENDER_DISTANCE_V = b.comment("HUD render distance in blocks").defineInRange("hudRenderDistance", 15, 1, 256);
		TOOLTIP_INFO_V        = b.comment("Show extra tooltip info").define("tooltipInfo", false);
		OPAQUE_V              = b.comment("Make pipes opaque").define("opaque", false);
		b.pop();

		b.comment("Power settings").push("power");
		PIPE_DURABILITY_V            = b.comment("Pipe block durability").defineInRange("pipeDurability", 0.25, 0.0, 1.0);
		POWER_USAGE_DISABLED_V       = b.comment("Disable power usage entirely").define("powerUsageDisabled", false);
		POWER_USAGE_MULTIPLIER_V     = b.comment("Power usage multiplier").defineInRange("powerUsageMultiplier", 1.0, 0.0, 100.0);
		CRAFTING_TABLE_POWER_USAGE_V = b.comment("Power used per crafting operation (RF)").defineInRange("craftingTablePowerUsage", 250, 0, Integer.MAX_VALUE);
		POWER_SOURCE_MODE_V          = b.comment("How the RF power junction acquires FE. ADJACENT: pulls from any neighbouring IEnergyStorage each tick. CABLE: passive — FE cables push into the junction.").defineEnum("powerSourceMode", PowerSourceMode.ADJACENT);
		b.pop();

		b.comment("Logistics system settings").push("logistics");
		COMPILER_SPEED_V         = b.comment("Program compiler speed multiplier").defineInRange("compilerSpeed", 1.0, 0.01, 100.0);
		ENABLE_RESEARCH_SYSTEM_V = b.comment("Enable research system").define("enableResearchSystem", false);
		BETA_UPGRADE_RECIPES_V   = b.comment("Craft module upgrades from a chip only, skipping the Logistics Programmer requirement (pre-1.8 beta style)").define("betaUpgradeRecipes", false);
		ENABLE_PARTICLE_FX_V     = b.comment("Enable particle effects").define("enableParticleFx", true);
		CHECK_FOR_UPDATES_V      = b.comment("Check for mod updates on startup").define("checkForUpdates", true);
		EASTER_EGGS_V            = b.comment("Enable easter eggs").define("easterEggs", true);
		MAX_ROBOT_DISTANCE_V     = b.comment("Max robot operation distance in blocks").defineInRange("maxRobotDistance", 64, 1, 512);
		b.pop();

		b.comment("Multithreading settings").push(CATEGORY_MULTITHREAD);
		MULTI_THREAD_NUMBER_V   = b.comment("Number of routing worker threads").defineInRange("threadCount", 4, 1, 32);
		MULTI_THREAD_PRIORITY_V = b.comment("Worker thread priority").defineInRange("threadPriority", Thread.NORM_PRIORITY, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
		b.pop();

		b.comment("Performance settings").push(CATEGORY_PERFORMANCE);
		DISABLE_ASYNC_WORK_V = b.comment("Disable async work processing (use main thread only)").define("disableAsyncWork", false);
		MIN_SLOT_ACCESS_V    = b.comment("Minimum inventory slot accesses per tick").defineInRange("minSlotAccess", 10, 1, Integer.MAX_VALUE);
		MAX_SLOT_ACCESS_V    = b.comment("Maximum inventory slot accesses per tick (0 = unlimited)").defineInRange("maxSlotAccess", 0, 0, Integer.MAX_VALUE);
		MIN_JOB_TICK_LENGTH_V = b.comment("Minimum ticks between async jobs").defineInRange("minJobTickLength", 1, 1, Integer.MAX_VALUE);
		b.pop();

		SPEC = b.build();
	}

	// ── Public static fields (populated from spec in load()) ──────────────────
	public static int LOGISTICS_DETECTION_LENGTH = 50;
	public static int LOGISTICS_DETECTION_COUNT = 100;
	public static int LOGISTICS_DETECTION_FREQUENCY = 20 * 30;
	public static boolean LOGISTICS_ORDERER_COUNT_INVERTWHEEL = false;
	public static boolean LOGISTICS_ORDERER_PAGE_INVERTWHEEL = false;
	public static final float LOGISTICS_ROUTED_SPEED_MULTIPLIER = 20F;
	public static final float LOGISTICS_DEFAULTROUTED_SPEED_MULTIPLIER = 10F;
	public static int MAX_UNROUTED_CONNECTIONS = 32;

	public static int LOGISTICS_HUD_RENDER_DISTANCE = 15;

	public static float pipeDurability = 0.25F;

	public static boolean LOGISTICS_POWER_USAGE_DISABLED = false;
	public static double POWER_USAGE_MULTIPLIER = 1;
	public static PowerSourceMode POWER_SOURCE_MODE = PowerSourceMode.ADJACENT;
	public static double COMPILER_SPEED = 1.0;
	public static boolean ENABLE_RESEARCH_SYSTEM = false;
	public static boolean BETA_UPGRADE_RECIPES = false;

	public static int LOGISTICS_CRAFTING_TABLE_POWER_USAGE = 250;

	public static boolean TOOLTIP_INFO = false;
	public static boolean ENABLE_PARTICLE_FX = true;

	public static int[] CHASSIS_SLOTS_ARRAY = {1, 2, 3, 4, 8};

	// GuiOrderer Popup setting — persisted to config file (see savePopupState())
	public static boolean DISPLAY_POPUP = true;

	// MultiThread
	public static int MULTI_THREAD_NUMBER = 4;
	public static int MULTI_THREAD_PRIORITY = Thread.NORM_PRIORITY;

	// Performance
	public static boolean DISABLE_ASYNC_WORK = false;
	public static int MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = 10;
	public static int MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = 0;
	public static int MINIMUM_JOB_TICK_LENGTH = 1;

	public static boolean CHECK_FOR_UPDATES = true;

	public static boolean EASTER_EGGS = true;

	public static boolean OPAQUE = false;

	public static int MAX_ROBOT_DISTANCE = 64;

	private static boolean loaded = false;

	public static void load() {
		if (Configs.loaded) {
			return;
		}
		Configs.loaded = true;
		LOGISTICS_DETECTION_LENGTH             = DETECTION_LENGTH_V.get();
		LOGISTICS_DETECTION_COUNT              = DETECTION_COUNT_V.get();
		LOGISTICS_DETECTION_FREQUENCY          = DETECTION_FREQUENCY_V.get();
		LOGISTICS_ORDERER_COUNT_INVERTWHEEL    = ORDERER_COUNT_INVERT_V.get();
		LOGISTICS_ORDERER_PAGE_INVERTWHEEL     = ORDERER_PAGE_INVERT_V.get();
		DISPLAY_POPUP                          = DISPLAY_POPUP_V.get();
		MAX_UNROUTED_CONNECTIONS               = MAX_UNROUTED_CONNECTIONS_V.get();
		LOGISTICS_HUD_RENDER_DISTANCE          = HUD_RENDER_DISTANCE_V.get();
		pipeDurability                         = PIPE_DURABILITY_V.get().floatValue();
		LOGISTICS_POWER_USAGE_DISABLED         = POWER_USAGE_DISABLED_V.get();
		POWER_USAGE_MULTIPLIER                 = POWER_USAGE_MULTIPLIER_V.get();
		POWER_SOURCE_MODE                      = POWER_SOURCE_MODE_V.get();
		COMPILER_SPEED                         = COMPILER_SPEED_V.get();
		ENABLE_RESEARCH_SYSTEM                 = ENABLE_RESEARCH_SYSTEM_V.get();
		BETA_UPGRADE_RECIPES                   = BETA_UPGRADE_RECIPES_V.get();
		LOGISTICS_CRAFTING_TABLE_POWER_USAGE   = CRAFTING_TABLE_POWER_USAGE_V.get();
		TOOLTIP_INFO                           = TOOLTIP_INFO_V.get();
		ENABLE_PARTICLE_FX                     = ENABLE_PARTICLE_FX_V.get();
		CHECK_FOR_UPDATES                      = CHECK_FOR_UPDATES_V.get();
		EASTER_EGGS                            = EASTER_EGGS_V.get();
		OPAQUE                                 = OPAQUE_V.get();
		MAX_ROBOT_DISTANCE                     = MAX_ROBOT_DISTANCE_V.get();
		MULTI_THREAD_NUMBER                    = MULTI_THREAD_NUMBER_V.get();
		MULTI_THREAD_PRIORITY                  = MULTI_THREAD_PRIORITY_V.get();
		DISABLE_ASYNC_WORK                     = DISABLE_ASYNC_WORK_V.get();
		MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = MIN_SLOT_ACCESS_V.get();
		MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = MAX_SLOT_ACCESS_V.get();
		MINIMUM_JOB_TICK_LENGTH                = MIN_JOB_TICK_LENGTH_V.get();
	}

	public static void savePopupState() {
		// Mirror LP1: write the current popup preference back to the config file and persist it
		// immediately so the toggle survives a restart.
		DISPLAY_POPUP_V.set(DISPLAY_POPUP);
		DISPLAY_POPUP_V.save();
	}

	/** Always reads live from the spec so config file edits take effect on reload. */
	public static PowerSourceMode getPowerSourceMode() {
		return POWER_SOURCE_MODE_V.get();
	}

	/** Always reads live from the spec: recipe conditions are evaluated on every datapack reload. */
	public static boolean getBetaUpgradeRecipes() {
		return BETA_UPGRADE_RECIPES_V.get();
	}

	public enum PowerSourceMode {
		/** Pull FE from any adjacent IEnergyStorage each tick (active). */
		ADJACENT,
		/** Accept FE pushed in by cables (passive, IEnergyStorage capability only). */
		CABLE
	}
}
