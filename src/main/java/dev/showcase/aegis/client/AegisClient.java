package dev.showcase.aegis.client;

import dev.showcase.aegis.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AegisClient implements ClientModInitializer {
	public static final String MOD_ID = "aegis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	@SuppressWarnings("deprecation")
	public void onInitializeClient() {
		ModuleManager.init();
		ClientTickEvents.END_CLIENT_TICK.register(client -> ModuleManager.onTick());
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> ModuleManager.onHud(ctx));
		LOGGER.info("Aegis showcase loaded. Press RIGHT SHIFT in a world to open the ClickGUI.");
	}
}
