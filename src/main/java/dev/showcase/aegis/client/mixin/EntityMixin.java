package dev.showcase.aegis.client.mixin;

import dev.showcase.aegis.module.ModuleManager;
import dev.showcase.aegis.module.combat.Hitbox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * {@code ProjectileUtil.raycast} (used by the crosshair target) expands each entity's box by
	 * this value. Only applied on the client thread so the integrated server's own projectile
	 * and hit logic is untouched.
	 */
	@Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
	private void aegis$expandHitbox(CallbackInfoReturnable<Float> cir) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || !mc.isOnThread()) return;
		Hitbox hitbox = ModuleManager.get(Hitbox.class);
		if (hitbox == null) return;
		float extra = hitbox.extraFor((Entity) (Object) this);
		if (extra > 0f) {
			cir.setReturnValue(cir.getReturnValueF() + extra);
		}
	}
}
