package com.Morph.logisticspipes.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.Morph.logisticspipes.LPConstants;

/**
 * Minimal JSON-backed config for LP2.
 *
 * Stored at {@code <gameDir>/config/morph.json}. Read once at mod init.
 * Defaults are written if the file is missing or invalid. Unknown keys in
 * the file are preserved (round-trip safe) but ignored at runtime.
 *
 * Static-field access so callers can do {@code LPConfig.POWER_USAGE_MULTIPLIER}
 * without a singleton fetch.
 */
public final class LPConfig {

    private LPConfig() {}

    private static final Path CONFIG_PATH =
            Path.of("config").resolve(LPConstants.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---------------------------------------------------------------------
    // Tunables — defaults match the values used in code before config existed.
    // Treat these as mutable at load time; never mutate after that.
    // ---------------------------------------------------------------------

    /** Multiplier applied to every {@code useEnergy} call. 1.0 = LP1 default. */
    public static double POWER_USAGE_MULTIPLIER = 1.0;

    /** Master kill switch — when true, all useEnergy calls succeed at zero cost. */
    public static boolean POWER_USAGE_DISABLED = false;

    /** Per-Power-Junction storage cap in LP units. LP1 default: 2,000,000. */
    public static int POWER_MAX_STORAGE = 2_000_000;

    /** How often {@code ServerRouter.update} runs per pipe, in ticks. */
    public static int ROUTING_REFRESH_TICKS = 20;

    /** Soft cap on routers per network — beyond this, new routers won't update. */
    public static int MAX_NETWORK_SIZE = 1000;

    // ---------------------------------------------------------------------

    /** Loads {@code config/morph.json}, writing defaults if absent or unparseable. */
    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }
            String text = Files.readString(CONFIG_PATH);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            POWER_USAGE_MULTIPLIER = getDouble(root, "power.usageMultiplier", POWER_USAGE_MULTIPLIER);
            POWER_USAGE_DISABLED   = getBool  (root, "power.usageDisabled",   POWER_USAGE_DISABLED);
            POWER_MAX_STORAGE      = getInt   (root, "power.maxStorage",      POWER_MAX_STORAGE);
            ROUTING_REFRESH_TICKS  = getInt   (root, "routing.refreshTicks",  ROUTING_REFRESH_TICKS);
            MAX_NETWORK_SIZE       = getInt   (root, "routing.maxNetworkSize", MAX_NETWORK_SIZE);
        } catch (Exception e) {
            // Corrupt file → keep defaults, log to stderr (no logger in common yet)
            System.err.println("[morph] Failed to read " + CONFIG_PATH + ": " + e.getMessage()
                    + " — using defaults");
        }
    }

    /** Writes current values back to the JSON file. Creates the parent dir if needed. */
    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            JsonObject root = new JsonObject();
            root.add("power.usageMultiplier", new JsonPrimitive(POWER_USAGE_MULTIPLIER));
            root.add("power.usageDisabled",   new JsonPrimitive(POWER_USAGE_DISABLED));
            root.add("power.maxStorage",      new JsonPrimitive(POWER_MAX_STORAGE));
            root.add("routing.refreshTicks",  new JsonPrimitive(ROUTING_REFRESH_TICKS));
            root.add("routing.maxNetworkSize", new JsonPrimitive(MAX_NETWORK_SIZE));
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[morph] Failed to write " + CONFIG_PATH + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------

    private static double getDouble(JsonObject root, String key, double def) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.getAsJsonPrimitive(key).getAsDouble()
                : def;
    }

    private static int getInt(JsonObject root, String key, int def) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.getAsJsonPrimitive(key).getAsInt()
                : def;
    }

    private static boolean getBool(JsonObject root, String key, boolean def) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.getAsJsonPrimitive(key).getAsBoolean()
                : def;
    }
}
