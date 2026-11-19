package dev.showcase.aegis.module.combat;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.CombatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Client-side hitbox expansion for crosshair targeting. The actual work happens in
 * {@code EntityMixin}, which adds {@link #extraFor(Entity)} to {@code Entity#getTargetingMargin}
 * (the value {@code ProjectileUtil.raycast} adds to a bounding box when finding the crosshair target).
 */
public final class Hitbox extends Module {
	public final SliderSetting expand = add(new SliderSetting("Expand Width", 0.15, 0.00, 0.50, 0.01, 2, ""));
	public final ToggleSetting playersOnly = add(new ToggleSetting("Players Only", true));
	public final ToggleSetting weaponOnly = add(new ToggleSetting("Weapon Only", false));

	public Hitbox() {
		super("Hitbox", "Expands target hitboxes for crosshair detection.", Category.COMBAT);
	}

	/** Extra margin (blocks) to add to {@code e}'s targeting box. Safe to call from the mixin. */
	public float extraFor(Entity e) {
		if (!isEnabled() || mc.player == null || e == mc.player) return 0f;
		if (!(e instanceof LivingEntity)) return 0f;
		if (playersOnly.isOn() && !(e instanceof PlayerEntity)) return 0f;
		if (weaponOnly.isOn() && !CombatUtil.isWeapon(mc.player.getMainHandStack())) return 0f;
		return expand.getFloat();
	}
}
