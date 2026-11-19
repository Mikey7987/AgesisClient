package dev.showcase.aegis.client.mixin;

import dev.showcase.aegis.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	/**
	 * Per-frame hook. Runs before the game ticks and renders, giving modules a frame delta so
	 * rotation is smooth at any FPS (the 20 Hz tick is too coarse for camera work).
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void aegis$onFrame(boolean tick, CallbackInfo ci) {
		ModuleManager.onFrame();
	}
}
