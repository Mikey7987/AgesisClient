package dev.showcase.aegis.util;

public final class Time {
	private Time() {}

	/** Milliseconds (may be fractional) to nanoseconds. */
	public static long ms(double ms) {
		return (long) (ms * 1_000_000.0);
	}
}
