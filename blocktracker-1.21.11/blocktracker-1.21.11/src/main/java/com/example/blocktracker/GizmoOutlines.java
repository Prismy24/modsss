package com.example.blocktracker;

import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

/**
 * Through-walls outlines using vanilla's Gizmo system, which can draw shapes "always on top"
 * (ignoring depth). Gizmos must be submitted every frame while the world is rendering.
 */
public final class GizmoOutlines {
	private static final float LINE_WIDTH = 2.5f;

	private static boolean failed = false;

	private GizmoOutlines() {}

	/**
	 * Submits one always-on-top outline per cached hit.
	 *
	 * @return true if the outlines were submitted; false if the gizmo system could not be used
	 *         (the caller then falls back to ordinary depth-tested lines).
	 */
	public static boolean render() {
		if (failed) {
			return false;
		}
		try {
			TrackerConfig cfg = TrackerConfig.get();
			int alpha = Math.round(Math.max(0.05f, Math.min(1f, cfg.lineAlpha)) * 255f);
			for (BlockScanner.Hit hit : BlockScanner.getHits()) {
				int color = (alpha << 24) | (hit.rgb() & 0xFFFFFF);
				// stroke colour, stroke width, fill colour (0 = no fill)
				GizmoStyle style = new GizmoStyle(color, LINE_WIDTH, 0);
				Gizmos.cuboid(hit.pos(), style).setAlwaysOnTop();
			}
			return true;
		} catch (Throwable t) {
			failed = true;
			BlockTrackerClient.LOGGER.warn("Gizmo outlines failed, falling back to depth-tested lines", t);
			return false;
		}
	}
}
