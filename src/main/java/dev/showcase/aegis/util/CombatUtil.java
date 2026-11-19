package dev.showcase.aegis.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Shared combat helpers: target validation, weapon checks, geometry and attacking. */
public final class CombatUtil {
	private static Entity lastHit;
	private static long lastHitMillis;

	private CombatUtil() {}

	// ---- last hit tracking ---------------------------------------------------------------------

	public static void recordHit(Entity e) {
		lastHit = e;
		lastHitMillis = System.currentTimeMillis();
	}

	/** True if {@code e} is the entity the player damaged most recently (within 10 seconds). */
	public static boolean isLastHit(Entity e) {
		return e != null && e == lastHit && System.currentTimeMillis() - lastHitMillis < 10_000L;
	}

	// ---- items ---------------------------------------------------------------------------------

	public static boolean isSwordOrAxe(ItemStack stack) {
		return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
	}

	public static boolean isAxe(ItemStack stack) {
		return stack.isIn(ItemTags.AXES);
	}

	/** Sword, Axe or Mace. */
	public static boolean isWeapon(ItemStack stack) {
		return isSwordOrAxe(stack) || stack.isOf(Items.MACE);
	}

	// ---- entities ------------------------------------------------------------------------------

	public static boolean isValidTarget(Entity e, Entity self, boolean playersOnly) {
		if (!(e instanceof LivingEntity le) || e == self) return false;
		if (!le.isAlive() || le.isSpectator()) return false;
		if (le instanceof ArmorStandEntity) return false;
		return !playersOnly || le instanceof PlayerEntity;
	}

	/** The living entity under the crosshair, or null. */
	public static LivingEntity crosshairLiving(MinecraftClient mc) {
		if (mc.crosshairTarget instanceof EntityHitResult r
				&& r.getEntity() instanceof LivingEntity le
				&& le != mc.player) {
			return le;
		}
		return null;
	}

	/** True when {@code target} is roughly looking towards {@code me}. */
	public static boolean isFacing(LivingEntity target, Entity me) {
		Vec3d look = target.getRotationVec(1.0f);
		double dx = me.getX() - target.getX();
		double dy = me.getY() - target.getY();
		double dz = me.getZ() - target.getZ();
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-4) return true;
		return (look.x * dx + look.y * dy + look.z * dz) / len > 0.25;
	}

	public static void attack(MinecraftClient mc, Entity target) {
		if (mc.player == null || mc.interactionManager == null) return;
		mc.interactionManager.attackEntity(mc.player, target);
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	// ---- geometry ------------------------------------------------------------------------------

	/** The point AimAssist steers towards: horizontal centre, upper torso height. */
	public static Vec3d aimPoint(LivingEntity e) {
		Box b = e.getBoundingBox();
		return new Vec3d((b.minX + b.maxX) * 0.5, b.minY + (b.maxY - b.minY) * 0.62, (b.minZ + b.maxZ) * 0.5);
	}

	public static double distanceToBox(Vec3d p, Box b) {
		double cx = MathUtil.clamp(p.x, b.minX, b.maxX);
		double cy = MathUtil.clamp(p.y, b.minY, b.maxY);
		double cz = MathUtil.clamp(p.z, b.minZ, b.maxZ);
		double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
