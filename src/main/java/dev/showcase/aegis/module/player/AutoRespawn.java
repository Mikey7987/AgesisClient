package dev.showcase.aegis.module.player;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.SliderSetting;

/** Respawns automatically after a configurable delay. */
public final class AutoRespawn extends Module {
	public final SliderSetting delay = add(new SliderSetting("Delay", 10, 0, 40, 1, 0, " ticks"));

	private int deadTicks;

	public AutoRespawn() {
		super("Auto Respawn", "Respawns automatically after dying.", Category.PLAYER);
	}

	@Override
	public void onDisable() {
		deadTicks = 0;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			deadTicks = 0;
			return;
		}
		if (mc.player.isDead()) {
			if (++deadTicks > delay.getInt()) {
				mc.player.requestRespawn();
				deadTicks = 0;
			}
		} else {
			deadTicks = 0;
		}
	}
}
