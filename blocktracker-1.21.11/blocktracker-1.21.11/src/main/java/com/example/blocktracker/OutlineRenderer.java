package com.example.blocktracker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Draws a wireframe box around every cached hit using vanilla's line render type.
 * All vertex coordinates handed to the world buffer must be relative to the camera.
 */
public final class OutlineRenderer {
	private static final float LINE_WIDTH = 2.5f;

	private OutlineRenderer() {}

	public static void render(WorldRenderContext ctx) {
		TrackerConfig cfg = TrackerConfig.get();
		if (!cfg.enabled) {
			return;
		}
		List<BlockScanner.Hit> hits = BlockScanner.getHits();
		if (hits.isEmpty()) {
			return;
		}

		// Through-walls mode: let the Gizmo system draw them. If it can't, fall through to
		// the ordinary depth-tested lines below.
		if (cfg.throughWalls && GizmoOutlines.render()) {
			return;
		}

		MultiBufferSource consumers = ctx.consumers();
		PoseStack matrices = ctx.matrices();
		if (consumers == null || matrices == null) {
			return;
		}

		Vec3 cam = ctx.gameRenderer().getMainCamera().position();
		VertexConsumer lines = consumers.getBuffer(RenderTypes.lines());

		int alpha = Math.round(Math.max(0.05f, Math.min(1f, cfg.lineAlpha)) * 255f);
		for (BlockScanner.Hit hit : hits) {
			int color = (alpha << 24) | (hit.rgb() & 0xFFFFFF);
			ShapeRenderer.renderShape(
				matrices, lines, Shapes.block(),
				hit.pos().getX() - cam.x,
				hit.pos().getY() - cam.y,
				hit.pos().getZ() - cam.z,
				color, LINE_WIDTH
			);
		}
	}
}
