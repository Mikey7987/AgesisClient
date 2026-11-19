package dev.showcase.aegis.setting;

import java.util.function.BooleanSupplier;

/** Base class for a module setting. {@code anim} is scratch state owned by the GUI. */
public abstract class Setting<T> {
	public final String name;
	public final T defaultValue;
	public float anim;
	protected T value;
	private BooleanSupplier visible = () -> true;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public T get() {
		return value;
	}

	public abstract void set(T newValue);

	public void reset() {
		set(defaultValue);
	}

	public Setting<T> visibleWhen(BooleanSupplier condition) {
		this.visible = condition;
		return this;
	}

	public boolean isVisible() {
		return visible.getAsBoolean();
	}
}
