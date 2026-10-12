package com.example.blocktracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Periodically scans the cube around the player for tracked blocks and caches the hits,
 * so the renderer only has to draw a ready-made list every frame.
 */
public final class BlockScanner {
	public record Hit(BlockPos pos, int rgb) {}

	private static final int SCAN_INTERVAL_TICKS = 10;
	private static final int MAX_HITS = 2000;

	private static volatile List<Hit> hits = List.of();
	private static int tickCounter = 0;
	private static boolean dirty = true;

	private BlockScanner() {}

	/** Call after the tracked block list or radius changes so the next tick rescans. */
	public static void markDirty() {
		dirty = true;
	}

	public static List<Hit> getHits() {
		return hits;
	}

	public static void tick(Minecraft client) {
		TrackerConfig cfg = TrackerConfig.get();
		if (client.level == null || client.player == null || !cfg.enabled || cfg.blocks.isEmpty()) {
			hits = List.of();
			return;
		}
		if (!dirty && ++tickCounter < SCAN_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;
		dirty = false;
		hits = scan(client.level, client.player.blockPosition(), cfg);
	}

	private static List<Hit> scan(ClientLevel level, BlockPos center, TrackerConfig cfg) {
		Map<Block, Integer> wanted = new HashMap<>();
		for (Map.Entry<String, String> e : cfg.blocks.entrySet()) {
			Identifier id = Identifier.tryParse(e.getKey());
			if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				wanted.put(BuiltInRegistries.BLOCK.getValue(id), TrackerConfig.parseColor(e.getValue()));
			}
		}
		if (wanted.isEmpty()) {
			return List.of();
		}

		int r = cfg.radius;
		int levelMinY = level.getMinY();
		int levelMaxY = levelMinY + level.getHeight() - 1;
		int minY = Math.max(levelMinY, center.getY() - r);
		int maxY = Math.min(levelMaxY, center.getY() + r);

		List<Hit> found = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = center.getX() - r; x <= center.getX() + r; x++) {
			for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
				if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				for (int y = minY; y <= maxY; y++) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					Integer rgb = wanted.get(state.getBlock());
					if (rgb != null) {
						found.add(new Hit(pos.immutable(), rgb));
						if (found.size() >= MAX_HITS) {
							return found;
						}
					}
				}
			}
		}
		return found;
	}
}
