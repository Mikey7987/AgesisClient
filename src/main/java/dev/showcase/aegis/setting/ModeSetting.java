package dev.showcase.aegis.setting;

public final class ModeSetting extends Setting<String> {
	public final String[] modes;
	private int index;

	public ModeSetting(String name, String def, String... modes) {
		super(name, def);
		this.modes = modes;
		set(def);
	}

	@Override
	public void set(String newValue) {
		for (int i = 0; i < modes.length; i++) {
			if (modes[i].equalsIgnoreCase(newValue)) {
				index = i;
				value = modes[i];
				return;
			}
		}
	}

	public int index() {
		return index;
	}

	public void next() {
		index = (index + 1) % modes.length;
		value = modes[index];
		anim = 1f;
	}

	public void prev() {
		index = (index - 1 + modes.length) % modes.length;
		value = modes[index];
		anim = 1f;
	}

	public boolean is(String mode) {
		return value.equalsIgnoreCase(mode);
	}
}
