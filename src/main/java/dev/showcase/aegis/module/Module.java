package dev.showcase.aegis.module;

import dev.showcase.aegis.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Base class of every module. Lifecycle hooks are all optional. */
public abstract class Module {
	protected final MinecraftClient mc = MinecraftClient.getInstance();

	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting<?>> settings = new ArrayList<>();

	// GUI / HUD animation state
	public float toggleAnim;
	public float expandAnim;
	public float hudAnim;
	public boolean expanded;

	private boolean enabled;
	private int key = -1;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean value) {
		if (enabled == value) return;
		enabled = value;
		if (value) onEnable();
		else onDisable();
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	protected boolean inGame() {
		return mc.player != null && mc.world != null;
	}

	/** Called once on the enabling transition. */
	public void onEnable() {}

	/** Called once on the disabling transition. */
	public void onDisable() {}

	/** Called every client tick (20 Hz) while enabled. */
	public void onTick() {}

	/** Called every rendered frame while enabled, with the frame delta in seconds. */
	public void onFrame(float dt) {}

	/** Called while the HUD is drawn, only while enabled. */
	public void onHud(DrawContext ctx, int screenWidth, int screenHeight, float dt) {}
}
