package dev.showcase.aegis.module.combat;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.ModeSetting;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.CombatUtil;
import dev.showcase.aegis.util.MathUtil;
import dev.showcase.aegis.util.Rand;
import dev.showcase.aegis.util.Time;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Frame-rate independent camera assistance with configurable axis speeds and smoothing.
 *
 * <p>Runs once per rendered frame (not per 20 Hz tick). Rotation is applied through
 * {@code Entity#changeLookDirection}, the same call the mouse uses, so the vanilla camera
 * interpolation stays consistent and the motion is smooth at any FPS.
 */
public final class AimAssist extends Module {
	/** Vanilla converts cursor units to degrees with this factor. */
	private static final float CURSOR_TO_DEGREES = 0.15f;

	public final ModeSetting mode = add(new ModeSetting("Movement Mode", "Smooth", "Smooth", "Linear", "Balanced"));
	public final SliderSetting smoothness = add(new SliderSetting("Smoothness", 45, 1, 100, 0.5, 1, ""));
	public final SliderSetting horizontalSpeed = add(new SliderSetting("Horizontal Speed", 70, 0, 100, 1, 0, "%"));
	public final SliderSetting verticalSpeed = add(new SliderSetting("Vertical Speed", 55, 0, 100, 1, 0, "%"));
	public final SliderSetting jitterReduction = add(new SliderSetting("Jitter Reduction", 40, 0, 100, 1, 0, "%"));
	public final SliderSetting transitionSmoothing = add(new SliderSetting("Transition Smoothing", 35, 0, 100, 1, 0, "%"));
	public final SliderSetting reaction = add(new SliderSetting("Reaction Time", 80, 0, 300, 5, 0, "ms"));
	public final SliderSetting overshoot = add(new SliderSetting("Overshoot", 8, 0, 30, 1, 0, "%"));
	public final SliderSetting fov = add(new SliderSetting("FOV", 90, 10, 180, 1, 0, "°"));
	public final SliderSetting range = add(new SliderSetting("Range", 4.2, 2.5, 6.0, 0.1, 1, ""));
	public final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Last Hit"));
	public final ToggleSetting weaponOnly = add(new ToggleSetting("Weapon Only", true));
	public final ToggleSetting playersOnly = add(new ToggleSetting("Players Only", true));
	public final ToggleSetting requireClick = add(new ToggleSetting("Require Click", true));

	// target state
	private LivingEntity target;
	private LivingEntity lastTarget;
	private long targetLostNs;
	private Vec3d smoothPoint;
	private float filteredYawStep, filteredPitchStep;

	// reaction state
	private long reactionUntilNs;
	private long lastReactionNs;
	private boolean swingPending;

	// swing state
	private float swingStartDistance;
	private int overshootPhase = 2; // 0 = heading to virtual target, 1 = settling back, 2 = none
	private float overshootYaw, overshootPitch, overshootMagnitude;
	private float prevDirYaw, prevDirPitch, prevMagnitude;

	// mouse tracking for "Require Click"
	private double lastMouseX, lastMouseY;
	private long lastMouseMoveNs;

	public AimAssist() {
		super("AimAssist", "Smooth, human-like camera assistance towards targets.", Category.COMBAT);
	}

	@Override
	public void onDisable() {
		resetState();
	}

	private void resetState() {
		if (target != null) {
			lastTarget = target;
			targetLostNs = System.nanoTime();
		}
		target = null;
		smoothPoint = null;
		filteredYawStep = 0f;
		filteredPitchStep = 0f;
		swingPending = false;
		overshootPhase = 2;
		prevMagnitude = 0f;
	}

	@Override
	public void onFrame(float dt) {
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.world == null || mc.currentScreen != null) {
			resetState();
			return;
		}
		long now = System.nanoTime();

		double mouseX = mc.mouse.getX();
		double mouseY = mc.mouse.getY();
		if (mouseX != lastMouseX || mouseY != lastMouseY) lastMouseMoveNs = now;
		lastMouseX = mouseX;
		lastMouseY = mouseY;

		if (weaponOnly.isOn() && !CombatUtil.isSwordOrAxe(p.getMainHandStack())) {
			resetState();
			return;
		}
		if (requireClick.isOn()) {
			boolean clicking = mc.options.attackKey.isPressed();
			boolean mouseMoving = now - lastMouseMoveNs < Time.ms(150);
			if (!clicking && !mouseMoving) {
				resetState();
				return;
			}
		}

		Vec3d eye = p.getEyePos();
		LivingEntity best = selectTarget(p, eye, p.getRotationVec(1.0f));
		if (best == null) {
			resetState();
			return;
		}

		// ---- (re)acquisition & reaction time ----
		if (best != target) {
			boolean quickReacquire = best == lastTarget && now - targetLostNs < Time.ms(250);
			target = best;
			smoothPoint = null;
			filteredYawStep = 0f;
			filteredPitchStep = 0f;
			overshootPhase = 2;
			if (!quickReacquire) startReaction(now);
		}
		if (now < reactionUntilNs) return;

		// ---- aim point, low-passed so 20 Hz entity position steps never reach the camera ----
		Vec3d raw = CombatUtil.aimPoint(target);
		double filterRate = 28.0 - jitterReduction.getDouble() * 0.20;
		smoothPoint = smoothPoint == null ? raw : smoothPoint.lerp(raw, 1.0 - Math.exp(-filterRate * dt));

		double dx = smoothPoint.x - eye.x;
		double dy = smoothPoint.y - eye.y;
		double dz = smoothPoint.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

		float dYaw = MathHelper.wrapDegrees(targetYaw - p.getYaw());
		float dPitch = targetPitch - p.getPitch();
		float magnitude = (float) Math.hypot(dYaw, dPitch);

		// ---- rapid direction change -> human delay again ----
		if (reaction.getDouble() > 0 && magnitude > 8f && prevMagnitude > 8f
				&& now - lastReactionNs > Time.ms(500)) {
			float dot = (dYaw * prevDirYaw + dPitch * prevDirPitch) / (magnitude * prevMagnitude);
			if (dot < -0.3f) {
				startReaction(now);
				prevMagnitude = 0f;
				return;
			}
		}
		prevDirYaw = dYaw;
		prevDirPitch = dPitch;
		prevMagnitude = magnitude;

		if (swingPending) {
			beginSwing(dYaw, dPitch, magnitude);
			swingPending = false;
		}

		// ---- overshoot: aim at a virtual point beyond the target, then relax back ----
		if (overshootPhase == 0) {
			float vm = (float) Math.hypot(dYaw + overshootYaw, dPitch + overshootPitch);
			if (vm < overshootMagnitude * 0.4f + 0.3f) overshootPhase = 1;
		}
		if (overshootPhase == 1) {
			float decay = (float) Math.exp(-5.0 * dt);
			overshootYaw *= decay;
			overshootPitch *= decay;
			if (Math.hypot(overshootYaw, overshootPitch) < 0.05) {
				overshootPhase = 2;
				overshootYaw = 0f;
				overshootPitch = 0f;
			}
		}
		float vYaw = dYaw + (overshootPhase == 2 ? 0f : overshootYaw);
		float vPitch = dPitch + (overshootPhase == 2 ? 0f : overshootPitch);
		float vMag = (float) Math.hypot(vYaw, vPitch);
		if (vMag < 0.0005f) return;

		// ---- speed profile ----
		float step = computeStep(vMag, magnitude, dt);
		if (step <= 0f) return;
		float k = step / vMag;
		float desiredYaw = vYaw * k * (horizontalSpeed.getFloat() / 100f);
		float desiredPitch = vPitch * k * (verticalSpeed.getFloat() / 100f);
		float transitionRate = 5f + (100f - transitionSmoothing.getFloat()) * 0.28f;
		filteredYawStep = horizontalSpeed.getFloat() <= 0f ? 0f
				: MathUtil.lerp(filteredYawStep, desiredYaw, transitionRate, dt);
		filteredPitchStep = verticalSpeed.getFloat() <= 0f ? 0f
				: MathUtil.lerp(filteredPitchStep, desiredPitch, transitionRate, dt);
		if (Math.abs(filteredYawStep) < 0.0005f && Math.abs(filteredPitchStep) < 0.0005f) return;

		p.changeLookDirection(filteredYawStep / CURSOR_TO_DEGREES, filteredPitchStep / CURSOR_TO_DEGREES);
	}

	private void startReaction(long now) {
		double ms = Math.max(0.0, reaction.getDouble() + Rand.range(-15.0, 15.0));
		if (reaction.getDouble() <= 0.0) ms = 0.0;
		reactionUntilNs = now + Time.ms(ms);
		lastReactionNs = now;
		swingPending = true;
	}

	private void beginSwing(float dYaw, float dPitch, float magnitude) {
		swingStartDistance = magnitude;
		overshootPhase = 2;
		overshootYaw = 0f;
		overshootPitch = 0f;
		float pct = overshoot.getFloat() / 100f;
		if (pct > 0f && magnitude > 12f) {
			float amount = pct * Math.min(magnitude, 70f);
			overshootYaw = dYaw / magnitude * amount;
			overshootPitch = dPitch / magnitude * amount;
			overshootMagnitude = (float) Math.hypot(overshootYaw, overshootPitch);
			overshootPhase = 0;
		}
	}

	/**
	 * Angular distance (degrees) to travel this frame.
	 *
	 * @param virtualMag distance to the (possibly overshooting) virtual target
	 * @param trueMag    distance to the real target, used by the Balanced curve
	 */
	private float computeStep(float virtualMag, float trueMag, float dt) {
		float s = smoothness.getFloat();
		switch (mode.get()) {
			case "Linear": {
				// Constant angular velocity; higher smoothness => slower.
				float speed = 900f / (1f + s * 0.12f);
				return Math.min(virtualMag, speed * dt);
			}
			case "Balanced": {
				// S-curve: gentle start, fast mid-swing, soft landing.
				swingStartDistance = Math.max(trueMag, swingStartDistance * (float) Math.exp(-0.8 * dt));
				float progress = swingStartDistance > 0.01f
						? MathUtil.clamp(1f - trueMag / swingStartDistance, 0f, 1f)
						: 1f;
				float curve = 0.22f + 0.78f * (float) Math.sin(Math.PI * progress);
				float edge = MathUtil.smoothstep(virtualMag / 6f);
				float vmax = 1600f / (1f + s * 0.1f);
				float step = vmax * curve * edge * dt;
				return Math.min(virtualMag, step);
			}
			default: {
				// Smooth: exponential ease-out, decelerates naturally onto the hitbox.
				float tau = 0.02f + (s - 1f) / 99f * 0.55f;
				return virtualMag * (1f - (float) Math.exp(-dt / tau));
			}
		}
	}

	// ---- target selection -----------------------------------------------------------------------

	private LivingEntity selectTarget(ClientPlayerEntity p, Vec3d eye, Vec3d look) {
		double maxRange = range.getDouble();
		double halfFov = fov.getDouble() * 0.5;
		double cosLimit = Math.cos(Math.toRadians(halfFov));

		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		boolean currentValid = false;
		double currentScore = Double.MAX_VALUE;

		for (Entity e : mc.world.getEntities()) {
			if (!CombatUtil.isValidTarget(e, p, playersOnly.isOn())) continue;
			LivingEntity le = (LivingEntity) e;
			double dist = CombatUtil.distanceToBox(eye, le.getBoundingBox());
			if (dist > maxRange) continue;
			Vec3d dir = CombatUtil.aimPoint(le).subtract(eye).normalize();
			if (dir.dotProduct(look) < cosLimit) continue;

			double score = score(le, dist);
			if (le == target) {
				currentValid = true;
				currentScore = score;
			}
			if (score < bestScore) {
				bestScore = score;
				best = le;
			}
		}
		// Hysteresis: keep the current target unless something is clearly better.
		if (currentValid && best != target && !(bestScore < currentScore * 0.8 - 0.05)) {
			return target;
		}
		return best;
	}

	private double score(LivingEntity e, double dist) {
		return switch (priority.get()) {
			case "Health" -> e.getHealth() + dist * 0.01;
			case "Last Hit" -> (CombatUtil.isLastHit(e) ? 0.0 : 100.0) + dist;
			default -> dist;
		};
	}

	/** Used by the Target HUD. */
	public LivingEntity getTarget() {
		return target;
	}
}
