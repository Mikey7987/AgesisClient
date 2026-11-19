package dev.showcase.aegis.module.movement;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import net.minecraft.client.option.KeyBinding;

/** Toggle-sneak: keeps the sneak key held while enabled. */
public final class SneakHold extends Module {
	public SneakHold() {
		super("Sneak Hold", "Keeps you sneaking without holding the key.", Category.MOVEMENT);
	}

	@Override
	public void onDisable() {
		KeyBinding.updatePressedStates();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null) return;
		mc.options.sneakKey.setPressed(true);
	}
}
