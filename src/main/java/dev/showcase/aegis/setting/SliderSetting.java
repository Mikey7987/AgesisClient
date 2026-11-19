package dev.showcase.aegis.setting;

import dev.showcase.aegis.util.MathUtil;

import java.util.Locale;

public final class SliderSetting extends Setting<Double> {
	public final double min;
	public final double max;
	public final double step;
	public final int decimals;
	public final String unit;

	public SliderSetting(String name, double def, double min, double max, double step, int decimals, String unit) {
		super(name, def);
		this.min = min;
		this.max = max;
		this.step = step;
		this.decimals = decimals;
		this.unit = unit;
		this.value = snap(def);
	}

	private double snap(double v) {
		double c = MathUtil.clamp(v, min, max);
		double snapped = Math.round((c - min) / step) * step + min;
		return MathUtil.clamp(snapped, min, max);
	}

	@Override
	public void set(Double newValue) {
		this.value = snap(newValue);
	}

	public double getDouble() {
		return value;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	/** Current value as 0..1 across the slider range. */
	public float fraction() {
		return (float) ((value - min) / (max - min));
	}

	public void setFraction(double fraction) {
		set(min + MathUtil.clamp(fraction, 0.0, 1.0) * (max - min));
	}

	public String format() {
		return String.format(Locale.ROOT, "%." + decimals + "f%s", value, unit);
	}
}
