package dev.showcase.aegis.util;

/** Small math helpers, including frame-rate independent exponential interpolation. */
public final class MathUtil {
	private MathUtil() {}

	public static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	public static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	public static int clamp(int v, int lo, int hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	/**
	 * Frame-rate independent exponential approach. {@code rate} is "per second":
	 * higher means faster. The result is identical whether you call it once with
	 * dt=1/30 or twice with dt=1/60.
	 */
	public static float lerp(float current, float target, float rate, float dt) {
		return current + (target - current) * (1f - (float) Math.exp(-rate * dt));
	}

	public static float easeOutCubic(float t) {
		float u = 1f - clamp(t, 0f, 1f);
		return 1f - u * u * u;
	}

	public static float smoothstep(float t) {
		t = clamp(t, 0f, 1f);
		return t * t * (3f - 2f * t);
	}
}
