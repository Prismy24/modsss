package com.example.blocktracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockTrackerClient implements ClientModInitializer {
	public static final String MOD_ID = "blocktracker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		TrackerConfig.get(); // load (or create) the config early

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blocktracker.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				TrackerCommands.toggle();
				if (client.player != null) {
					client.player.displayClientMessage(Component.literal(
						"Block Tracker " + (TrackerConfig.get().enabled ? "enabled" : "disabled")), true);
				}
			}
			BlockScanner.tick(client);
		});

		WorldRenderEvents.AFTER_ENTITIES.register(OutlineRenderer::render);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(TrackerCommands.build()));

		LOGGER.info("Block Tracker loaded");
	}
}
