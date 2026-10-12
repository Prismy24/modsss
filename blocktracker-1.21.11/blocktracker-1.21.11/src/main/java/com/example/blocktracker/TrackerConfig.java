package com.example.blocktracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent settings, stored at config/blocktracker.json.
 * Block colours are stored as hex strings ("#RRGGBB") keyed by block id.
 */
public class TrackerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("blocktracker.json");

	public static final int MAX_RADIUS = 64;

	public boolean enabled = true;
	public boolean throughWalls = true;
	public int radius = 24;
	public float lineAlpha = 1.0f;
	public Map<String, String> blocks = new LinkedHashMap<>();

	private static TrackerConfig instance;

	public static TrackerConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static TrackerConfig load() {
		if (Files.exists(PATH)) {
			try {
				TrackerConfig cfg = GSON.fromJson(Files.readString(PATH), TrackerConfig.class);
				if (cfg != null) {
					if (cfg.blocks == null) cfg.blocks = new LinkedHashMap<>();
					cfg.radius = Math.max(1, Math.min(MAX_RADIUS, cfg.radius));
					return cfg;
				}
			} catch (Exception e) {
				BlockTrackerClient.LOGGER.warn("Could not read config, using defaults", e);
			}
		}
		TrackerConfig cfg = new TrackerConfig();
		cfg.blocks.put("minecraft:diamond_ore", "#00E5FF");
		cfg.blocks.put("minecraft:deepslate_diamond_ore", "#00E5FF");
		cfg.blocks.put("minecraft:chest", "#FFB300");
		cfg.save();
		return cfg;
	}

	public void save() {
		try {
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			BlockTrackerClient.LOGGER.warn("Could not save config", e);
		}
	}

	/** Parses "#RRGGBB" into a packed RGB int; falls back to white. */
	public static int parseColor(String hex) {
		try {
			String s = hex.startsWith("#") ? hex.substring(1) : hex;
			return Integer.parseInt(s, 16) & 0xFFFFFF;
		} catch (Exception e) {
			return 0xFFFFFF;
		}
	}

	/** Deterministic, fairly bright colour for a block id with no explicit colour. */
	public static String autoColor(String id) {
		float hue = (Math.abs(id.hashCode()) % 360) / 360f;
		int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF;
		return String.format("#%06X", rgb);
	}
}
