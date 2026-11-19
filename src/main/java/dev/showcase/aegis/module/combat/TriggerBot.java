package dev.showcase.aegis.module.combat;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.module.ModuleManager;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.CombatUtil;
import dev.showcase.aegis.util.Rand;
import dev.showcase.aegis.util.Time;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

/** Attacks when the crosshair is on a valid target, respecting 1.21 cooldown and crit rules. */
public final class TriggerBot extends Module {
	public final SliderSetting cooldown = add(new SliderSetting("Cooldown Threshold", 0.95, 0.85, 1.00, 0.01, 2, ""));
	public final ToggleSetting airCrits = add(new ToggleSetting("Smart Air Crits", true));
	public final SliderSetting hoverDelay = add(new SliderSetting("Hover Delay", 40, 0, 150, 1, 0, "ms"));
	public final SliderSetting postDelay = add(new SliderSetting("Post-Cooldown Delay", 30, 0, 100, 1, 0, "ms"));
	public final ToggleSetting shieldCheck = add(new ToggleSetting("Shield Check", true));
	public final ToggleSetting weaponOnly = add(new ToggleSetting("Weapon Only", true));

	private int targetId = -1;
	private long hoverReadyNs;
	private boolean cooldownArmed;
	private long cooldownReadyNs;

	public TriggerBot() {
		super("TriggerBot", "Attacks when your crosshair is on a target at full cooldown.", Category.COMBAT);
	}

	@Override
	public void onDisable() {
		reset();
	}

	private void reset() {
		targetId = -1;
		cooldownArmed = false;
	}

	@Override
	public void onFrame(float dt) {
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
			reset();
			return;
		}

		Entity target = null;
		if (mc.crosshairTarget instanceof EntityHitResult hit
				&& CombatUtil.isValidTarget(hit.getEntity(), p, false)) {
			target = hit.getEntity();
		}
		if (target == null) {
			reset();
			return;
		}
		if (weaponOnly.isOn() && !CombatUtil.isWeapon(p.getMainHandStack())) {
			reset();
			return;
		}

		long now = System.nanoTime();

		// Crosshair just entered a (new) hitbox: start the hover / reaction timer.
		if (target.getId() != targetId) {
			targetId = target.getId();
			double sd = hoverDelay.getDouble() * 0.25;
			hoverReadyNs = now + Time.ms(Rand.gaussian(hoverDelay.getDouble(), sd, 0.0, hoverDelay.getDouble() * 2.0));
			cooldownArmed = false;
		}

		// While ShieldBreaker runs its axe stun it owns the swing. This also means the shield
		// pause below can never stall the stun.
		ShieldBreaker breaker = ModuleManager.get(ShieldBreaker.class);
		if (breaker != null && breaker.isExecuting()) return;

		if (shieldCheck.isOn() && target instanceof LivingEntity le
				&& le.isBlocking() && CombatUtil.isFacing(le, p)) {
			return;
		}

		float progress = p.getAttackCooldownProgress(0.5f);
		if (progress < cooldown.getFloat()) {
			cooldownArmed = false;
			return;
		}
		if (!cooldownArmed) {
			cooldownArmed = true;
			cooldownReadyNs = now + Time.ms(Rand.range(0.0, postDelay.getDouble()));
		}

		// Smart air crits: airborne -> wait until we are actually falling.
		if (airCrits.isOn() && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
				&& p.getVelocity().y >= 0.0) {
			return;
		}

		if (now < hoverReadyNs || now < cooldownReadyNs) return;

		CombatUtil.attack(mc, target);
		cooldownArmed = false;
	}
}
