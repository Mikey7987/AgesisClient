package dev.showcase.aegis.module.player;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.util.Rand;

/** Nudges the camera every few seconds so the client never looks idle. */
public final class AntiAfk extends Module {
	public final SliderSetting interval = add(new SliderSetting("Interval", 20, 5, 60, 1, 0, "s"));
	public final SliderSetting strength = add(new SliderSetting("Strength", 6, 1, 20, 1, 0, "°"));

	private int ticks;

	public AntiAfk() {
		super("Anti AFK", "Small periodic camera nudge.", Category.PLAYER);
	}

	@Override
	public void onEnable() {
		ticks = 0;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null) return;
		if (++ticks >= interval.getInt() * 20) {
			ticks = 0;
			double deg = Rand.range(-strength.getDouble(), strength.getDouble());
			mc.player.changeLookDirection(deg / 0.15, 0.0);
		}
	}
}
