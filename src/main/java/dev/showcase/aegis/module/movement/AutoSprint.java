package dev.showcase.aegis.module.movement;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.ModeSetting;
import net.minecraft.client.option.KeyBinding;

/** Holds the sprint key while moving. */
public final class AutoSprint extends Module {
	public final ModeSetting mode = add(new ModeSetting("Mode", "Forward", "Forward", "Any Direction"));

	public AutoSprint() {
		super("Auto Sprint", "Sprints automatically while moving.", Category.MOVEMENT);
	}

	@Override
	public void onDisable() {
		KeyBinding.updatePressedStates();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null) return;
		boolean forward = mc.options.forwardKey.isPressed();
		boolean any = forward || mc.options.backKey.isPressed()
				|| mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
		boolean moving = mode.is("Forward") ? forward : any;
		if (moving && !mc.player.isSneaking()) {
			mc.options.sprintKey.setPressed(true);
		}
	}
}
