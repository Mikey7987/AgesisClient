package dev.showcase.aegis.setting;

public final class ToggleSetting extends Setting<Boolean> {
	public ToggleSetting(String name, boolean def) {
		super(name, def);
	}

	@Override
	public void set(Boolean newValue) {
		this.value = newValue;
	}

	public boolean isOn() {
		return value;
	}

	public void toggle() {
		this.value = !this.value;
	}
}
