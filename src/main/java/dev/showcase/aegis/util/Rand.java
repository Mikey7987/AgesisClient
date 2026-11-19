package dev.showcase.aegis.util;

import java.util.concurrent.ThreadLocalRandom;

/** Randomisation helpers used for human-like timing variance. */
public final class Rand {
	private Rand() {}

	public static double range(double lo, double hi) {
		if (hi <= lo) return lo;
		return ThreadLocalRandom.current().nextDouble(lo, hi);
	}

	/** Gaussian sample clamped to [lo, hi]. */
	public static double gaussian(double mean, double sd, double lo, double hi) {
		double v = mean + ThreadLocalRandom.current().nextGaussian() * sd;
		return MathUtil.clamp(v, lo, hi);
	}

	/** @param percent probability in percent (0-100). */
	public static boolean chance(double percent) {
		return ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
	}
}
