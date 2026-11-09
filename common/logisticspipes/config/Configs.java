package logisticspipes.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

//@formatter:off
//CHECKSTYLE:OFF

public class Configs {

	public static final String CATEGORY_MULTITHREAD = "multithread";
	public static final String CATEGORY_PERFORMANCE = "performance";

	// ── Public static fields (populated from config/logisticspipes.properties in load()) ──
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
	private static boolean chassisSlotsLoaded = false;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("logisticspipes.properties");
	private static final Properties stored = new Properties();
	// key -> "# comment\nkey=value", in file order; rewritten on every load/save so new options appear
	private static final Map<String, String> written = new LinkedHashMap<>();

	public static void load() {
		if (Configs.loaded) {
			return;
		}
		Configs.loaded = true;
		stored.clear();
		written.clear();
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				stored.load(reader);
			} catch (IOException e) {
				logisticspipes.LogisticsPipes.log.error("Could not read " + FILE + ", using defaults", e);
			}
		}

		LOGISTICS_DETECTION_LENGTH             = intValue("detection.detectionLength", "Max detection length for pipe network scan", 50, 1, 1000);
		LOGISTICS_DETECTION_COUNT              = intValue("detection.detectionCount", "Max pipes counted during detection", 100, 1, 10000);
		LOGISTICS_DETECTION_FREQUENCY          = intValue("detection.detectionFrequency", "Detection frequency in ticks", 20 * 30, 1, Integer.MAX_VALUE);
		MAX_UNROUTED_CONNECTIONS               = intValue("detection.maxUnroutedConnections", "Max unrouted connections per pipe", 32, 1, 512);
		LOGISTICS_ORDERER_COUNT_INVERTWHEEL    = boolValue("orderer.invertCountWheel", "Invert mouse wheel for item count in orderer", false);
		LOGISTICS_ORDERER_PAGE_INVERTWHEEL     = boolValue("orderer.invertPageWheel", "Invert mouse wheel for page navigation in orderer", false);
		DISPLAY_POPUP                          = boolValue("orderer.displayPopup", "Set the default configuration for the popup of the Orderer Gui. Should it be used?", true);
		LOGISTICS_HUD_RENDER_DISTANCE          = intValue("hud.hudRenderDistance", "HUD render distance in blocks", 15, 1, 256);
		TOOLTIP_INFO                           = boolValue("hud.tooltipInfo", "Show extra tooltip info", false);
		OPAQUE                                 = boolValue("hud.opaque", "Make pipes opaque", false);
		pipeDurability                         = (float) doubleValue("power.pipeDurability", "Pipe block durability", 0.25, 0.0, 1.0);
		LOGISTICS_POWER_USAGE_DISABLED         = boolValue("power.powerUsageDisabled", "Disable power usage entirely", false);
		POWER_USAGE_MULTIPLIER                 = doubleValue("power.powerUsageMultiplier", "Power usage multiplier", 1.0, 0.01, 100.0);
		LOGISTICS_CRAFTING_TABLE_POWER_USAGE   = intValue("power.craftingTablePowerUsage", "Power used per crafting operation (RF)", 250, 0, Integer.MAX_VALUE);
		POWER_SOURCE_MODE                      = enumValue("power.powerSourceMode", "How the RF power junction acquires FE. ADJACENT: pulls from any neighbouring IEnergyStorage each tick. CABLE: passive — FE cables push into the junction.", PowerSourceMode.ADJACENT);
		COMPILER_SPEED                         = doubleValue("logistics.compilerSpeed", "Program compiler speed multiplier", 1.0, 0.01, 100.0);
		ENABLE_RESEARCH_SYSTEM                 = boolValue("logistics.enableResearchSystem", "Enable research system", false);
		BETA_UPGRADE_RECIPES                   = boolValue("logistics.betaUpgradeRecipes", "Craft module upgrades from a chip only, skipping the Logistics Programmer requirement (pre-1.8 beta style)", false);
		ENABLE_PARTICLE_FX                     = boolValue("logistics.enableParticleFx", "Enable particle effects", true);
		CHECK_FOR_UPDATES                      = boolValue("logistics.checkForUpdates", "Check for mod updates on startup", true);
		EASTER_EGGS                            = boolValue("logistics.easterEggs", "Enable easter eggs", true);
		MAX_ROBOT_DISTANCE                     = intValue("logistics.maxRobotDistance", "Max robot operation distance in blocks", 64, 1, 512);
		String chassisSlots                    = stringValue("logistics.chassisSlots", "The number of slots in a chassis pipe starting from MK1 to MK5, comma separated. Because there are 5 tiers, there need to be 5 values (positive integers, zero is allowed). Needs a restart.", "1,2,3,4,8");
		MULTI_THREAD_NUMBER                    = intValue(CATEGORY_MULTITHREAD + ".threadCount", "Number of routing table update threads, 0 to disable", 4, 0, 32);
		MULTI_THREAD_PRIORITY                  = intValue(CATEGORY_MULTITHREAD + ".threadPriority", "Worker thread priority", Thread.NORM_PRIORITY, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
		DISABLE_ASYNC_WORK                     = boolValue(CATEGORY_PERFORMANCE + ".disableAsyncWork", "Disable async work processing (use main thread only)", false);
		MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = intValue(CATEGORY_PERFORMANCE + ".minSlotAccess", "Minimum inventory slot accesses per tick", 10, 1, Integer.MAX_VALUE);
		MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = intValue(CATEGORY_PERFORMANCE + ".maxSlotAccess", "Maximum inventory slot accesses per tick (0 = unlimited)", 0, 0, Integer.MAX_VALUE);
		MINIMUM_JOB_TICK_LENGTH                = intValue(CATEGORY_PERFORMANCE + ".minJobTickLength", "Minimum ticks between async jobs", 1, 1, Integer.MAX_VALUE);
		if (!chassisSlotsLoaded) {
			// Read once: live chassis inventories are sized from these values
			chassisSlotsLoaded = true;
			try {
				int[] slots = java.util.Arrays.stream(chassisSlots.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
				if (slots.length == 5 && java.util.Arrays.stream(slots).allMatch(i -> i >= 0)) {
					java.util.Arrays.sort(slots);
					CHASSIS_SLOTS_ARRAY = slots;
				} else {
					logisticspipes.LogisticsPipes.log.error("chassisSlots needs 5 non-negative values (MK1 to MK5), got " + chassisSlots + "; using defaults");
				}
			} catch (NumberFormatException e) {
				logisticspipes.LogisticsPipes.log.error("chassisSlots is not a list of integers: " + chassisSlots + "; using defaults");
			}
		}
		save();
	}

	private static String stringValue(String key, String comment, String def) {
		String value = stored.getProperty(key, def).trim();
		written.put(key, "# " + comment + "\n" + key + "=" + value);
		return value;
	}

	private static int intValue(String key, String comment, int def, int min, int max) {
		int value;
		try {
			value = Math.clamp(Long.parseLong(stringValue(key, comment, Integer.toString(def))), min, max);
		} catch (NumberFormatException e) {
			value = def;
		}
		written.put(key, "# " + comment + " [" + min + ".." + max + "]\n" + key + "=" + value);
		return value;
	}

	private static double doubleValue(String key, String comment, double def, double min, double max) {
		double value;
		try {
			value = Math.clamp(Double.parseDouble(stringValue(key, comment, Double.toString(def))), min, max);
		} catch (NumberFormatException e) {
			value = def;
		}
		written.put(key, "# " + comment + " [" + min + ".." + max + "]\n" + key + "=" + value);
		return value;
	}

	private static boolean boolValue(String key, String comment, boolean def) {
		boolean value = Boolean.parseBoolean(stringValue(key, comment, Boolean.toString(def)));
		written.put(key, "# " + comment + "\n" + key + "=" + value);
		return value;
	}

	private static <E extends Enum<E>> E enumValue(String key, String comment, E def) {
		E value;
		try {
			value = Enum.valueOf(def.getDeclaringClass(), stringValue(key, comment, def.name()));
		} catch (IllegalArgumentException e) {
			value = def;
		}
		written.put(key, "# " + comment + " " + java.util.Arrays.toString(def.getDeclaringClass().getEnumConstants()) + "\n" + key + "=" + value.name());
		return value;
	}

	private static void save() {
		try {
			Files.writeString(FILE, String.join("\n\n", written.values()) + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			logisticspipes.LogisticsPipes.log.error("Could not write " + FILE, e);
		}
	}

	public static void savePopupState() {
		// Mirror LP1: write the current popup preference back to the config file and persist it
		// immediately so the toggle survives a restart.
		written.computeIfPresent("orderer.displayPopup", (key, line) -> line.substring(0, line.lastIndexOf('=') + 1) + DISPLAY_POPUP);
		save();
	}

	public static PowerSourceMode getPowerSourceMode() {
		return POWER_SOURCE_MODE;
	}

	public static boolean getBetaUpgradeRecipes() {
		return BETA_UPGRADE_RECIPES;
	}

	public enum PowerSourceMode {
		/** Pull FE from any adjacent IEnergyStorage each tick (active). */
		ADJACENT,
		/** Accept FE pushed in by cables (passive, IEnergyStorage capability only). */
		CABLE
	}
}
