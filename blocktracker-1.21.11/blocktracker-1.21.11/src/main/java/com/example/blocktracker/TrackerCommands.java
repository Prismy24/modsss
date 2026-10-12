package com.example.blocktracker;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * /track add <block> [#RRGGBB]
 * /track remove <block>
 * /track list
 * /track radius <1-64>
 * /track alpha <0.05-1>
 * /track toggle
 *
 * The block (and colour) are read as one "greedy" argument and split on spaces,
 * because Brigadier's plain word argument cannot contain ':' or '#'.
 */
public final class TrackerCommands {
	private TrackerCommands() {}

	public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
		return ClientCommandManager.literal("track")
			.then(ClientCommandManager.literal("add")
				.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
					.suggests((c, b) -> {
						String typed = b.getRemaining().toLowerCase();
						if (!typed.contains(" ")) {
							BuiltInRegistries.BLOCK.keySet().stream()
								.map(Identifier::toString)
								.filter(s -> s.contains(typed))
								.limit(100)
								.forEach(b::suggest);
						}
						return b.buildFuture();
					})
					.executes(TrackerCommands::add)))
			.then(ClientCommandManager.literal("remove")
				.then(ClientCommandManager.argument("block", StringArgumentType.greedyString())
					.suggests((c, b) -> {
						TrackerConfig.get().blocks.keySet().forEach(b::suggest);
						return b.buildFuture();
					})
					.executes(TrackerCommands::remove)))
			.then(ClientCommandManager.literal("base").executes(TrackerCommands::addBase))
			.then(ClientCommandManager.literal("list").executes(TrackerCommands::list))
			.then(ClientCommandManager.literal("radius")
				.then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, TrackerConfig.MAX_RADIUS))
					.executes(c -> {
						TrackerConfig cfg = TrackerConfig.get();
						cfg.radius = IntegerArgumentType.getInteger(c, "value");
						cfg.save();
						BlockScanner.markDirty();
						msg(c, "Scan radius set to " + cfg.radius);
						return 1;
					})))
			.then(ClientCommandManager.literal("walls")
				.then(ClientCommandManager.argument("value", BoolArgumentType.bool())
					.executes(c -> {
						TrackerConfig cfg = TrackerConfig.get();
						cfg.throughWalls = BoolArgumentType.getBool(c, "value");
						cfg.save();
						msg(c, "Through walls: " + cfg.throughWalls);
						return 1;
					})))
			.then(ClientCommandManager.literal("alpha")
				.then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.05f, 1f))
					.executes(c -> {
						TrackerConfig cfg = TrackerConfig.get();
						cfg.lineAlpha = FloatArgumentType.getFloat(c, "value");
						cfg.save();
						msg(c, "Outline opacity set to " + cfg.lineAlpha);
						return 1;
					})))
			.then(ClientCommandManager.literal("toggle").executes(c -> {
				toggle();
				msg(c, "Block Tracker " + (TrackerConfig.get().enabled ? "enabled" : "disabled"));
				return 1;
			}));
	}

	public static void toggle() {
		TrackerConfig cfg = TrackerConfig.get();
		cfg.enabled = !cfg.enabled;
		cfg.save();
		BlockScanner.markDirty();
	}

	/** Accepts "iron_ore" or "minecraft:iron_ore". */
	private static Identifier parseBlockId(String raw) {
		return Identifier.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
	}

	private static int add(CommandContext<FabricClientCommandSource> c) {
		String[] parts = StringArgumentType.getString(c, "args").trim().split("\\s+");
		Identifier id = parseBlockId(parts[0]);
		if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
			msg(c, "Unknown block: " + parts[0]);
			return 0;
		}
		String key = id.toString();
		String hex;
		if (parts.length < 2) {
			hex = TrackerConfig.autoColor(key);
		} else {
			String digits = parts[1].replace("#", "");
			if (!digits.matches("[0-9a-fA-F]{6}")) {
				msg(c, "Colour must be 6 hex digits, e.g. #FF0000");
				return 0;
			}
			hex = "#" + digits.toUpperCase();
		}
		TrackerConfig cfg = TrackerConfig.get();
		cfg.blocks.put(key, hex);
		cfg.save();
		BlockScanner.markDirty();
		msg(c, "Tracking " + key + " (" + hex + ")");
		return 1;
	}

	private static int remove(CommandContext<FabricClientCommandSource> c) {
		Identifier id = parseBlockId(StringArgumentType.getString(c, "block").trim());
		String key = id == null ? "" : id.toString();
		TrackerConfig cfg = TrackerConfig.get();
		if (cfg.blocks.remove(key) == null) {
			msg(c, "Not tracking " + key);
			return 0;
		}
		cfg.save();
		BlockScanner.markDirty();
		msg(c, "Stopped tracking " + key);
		return 1;
	}

	private static int list(CommandContext<FabricClientCommandSource> c) {
		TrackerConfig cfg = TrackerConfig.get();
		if (cfg.blocks.isEmpty()) {
			msg(c, "No blocks tracked. Use /track add <block>");
			return 1;
		}
		StringBuilder sb = new StringBuilder("Tracking (radius " + cfg.radius + "):");
		for (Map.Entry<String, String> e : cfg.blocks.entrySet()) {
			sb.append("\n  ").append(e.getKey()).append("  ").append(e.getValue());
		}
		msg(c, sb.toString());
		return 1;
	}

	/** Blocks that usually mark a player base. Unknown ids are skipped by the scanner. */
	private static final String[] BASE_BLOCKS = {
		"minecraft:chest",
		"minecraft:trapped_chest",
		"minecraft:ender_chest",
		"minecraft:barrel",
		"minecraft:furnace",
		"minecraft:blast_furnace",
		"minecraft:smoker",
		"minecraft:crafting_table",
		"minecraft:enchanting_table",
		"minecraft:anvil",
		"minecraft:beacon",
		"minecraft:spawner",
	};

	private static int addBase(CommandContext<FabricClientCommandSource> c) {
		TrackerConfig cfg = TrackerConfig.get();
		int added = 0;
		for (String id : BASE_BLOCKS) {
			if (!cfg.blocks.containsKey(id)) {
				cfg.blocks.put(id, TrackerConfig.autoColor(id));
				added++;
			}
		}
		cfg.save();
		BlockScanner.markDirty();
		msg(c, "Added " + added + " base blocks. Use /track list to see everything tracked.");
		return 1;
	}

	private static void msg(CommandContext<FabricClientCommandSource> c, String text) {
		c.getSource().sendFeedback(Component.literal(text));
	}
}
