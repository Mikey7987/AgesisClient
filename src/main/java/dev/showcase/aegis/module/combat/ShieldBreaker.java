package dev.showcase.aegis.module.combat;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.CombatUtil;
import dev.showcase.aegis.util.Rand;
import dev.showcase.aegis.util.Time;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;

/**
 * Detects a raised shield on the crosshair target, swaps to an axe from the hotbar, hits to
 * disable the shield, and optionally swaps back.
 */
public final class ShieldBreaker extends Module {
	private enum State { IDLE, REACTING, SWAPPED, SWAP_BACK }

	public final SliderSetting chance = add(new SliderSetting("Trigger Chance", 85, 1, 100, 1, 0, "%"));
	public final SliderSetting reactionDelay = add(new SliderSetting("Reaction Delay", 60, 0, 200, 1, 0, "ms"));
	public final SliderSetting swapToHitDelay = add(new SliderSetting("Swap-to-Hit Delay", 60, 0, 150, 1, 0, "ms"));
	public final ToggleSetting swapBack = add(new ToggleSetting("Swap Back", true));
	public final SliderSetting swapBackDelay = add(new SliderSetting("Swap Back Delay", 80, 0, 200, 1, 0, "ms"));

	private State state = State.IDLE;
	private long deadlineNs;
	private int previousSlot = -1;
	private int axeSlot = -1;
	private int swapAge;
	private LivingEntity target;

	// one roll per "shield raised" encounter
	private int encounterId = -1;
	private boolean encounterPass;

	public ShieldBreaker() {
		super("ShieldBreaker", "Swaps to an axe to disable raised shields.", Category.COMBAT);
	}

	/** True while a stun sequence is in progress (TriggerBot yields to it). */
	public boolean isExecuting() {
		return state != State.IDLE;
	}

	@Override
	public void onDisable() {
		abort();
	}

	private void abort() {
		if (state == State.SWAPPED || state == State.SWAP_BACK) restoreSlot();
		state = State.IDLE;
		target = null;
	}

	private void restoreSlot() {
		ClientPlayerEntity p = mc.player;
		if (p != null && swapBack.isOn() && previousSlot >= 0 && previousSlot != p.getInventory().getSelectedSlot()) {
			p.getInventory().setSelectedSlot(previousSlot);
		}
	}

	@Override
	public void onFrame(float dt) {
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
			if (state != State.IDLE) abort();
			return;
		}
		long now = System.nanoTime();
		LivingEntity looked = CombatUtil.crosshairLiving(mc);
		boolean blocking = looked != null && looked.isBlocking() && CombatUtil.isFacing(looked, p);

		switch (state) {
			case IDLE -> {
				if (!blocking) {
					encounterId = -1;
					return;
				}
				if (looked.getId() != encounterId) {
					encounterId = looked.getId();
					encounterPass = Rand.chance(chance.getDouble());
				}
				if (!encounterPass) return;

				PlayerInventory inv = p.getInventory();
				int current = inv.getSelectedSlot();
				int axe = CombatUtil.isAxe(p.getMainHandStack()) ? current : findAxeSlot(inv);
				if (axe < 0) {
					encounterPass = false; // nothing to swap to
					return;
				}
				encounterPass = false; // consumed: one stun attempt per raise
				previousSlot = current;
				axeSlot = axe;
				target = looked;
				deadlineNs = now + Time.ms(Rand.gaussian(reactionDelay.getDouble(), reactionDelay.getDouble() * 0.15, 0, 400));
				state = State.REACTING;
			}
			case REACTING -> {
				if (!blocking || looked != target) {
					abort();
					return;
				}
				if (now < deadlineNs) return;
				PlayerInventory inv = p.getInventory();
				if (axeSlot != inv.getSelectedSlot()) inv.setSelectedSlot(axeSlot);
				swapAge = p.age; // the slot packet is flushed on the next client tick
				deadlineNs = now + Time.ms(swapToHitDelay.getDouble());
				state = State.SWAPPED;
			}
			case SWAPPED -> {
				if (now < deadlineNs || p.age <= swapAge) return;
				if (looked == target && target != null && target.isAlive()) {
					CombatUtil.attack(mc, target);
				}
				deadlineNs = now + Time.ms(swapBackDelay.getDouble());
				state = State.SWAP_BACK;
			}
			case SWAP_BACK -> {
				if (now < deadlineNs) return;
				restoreSlot();
				state = State.IDLE;
				target = null;
			}
		}
	}

	private int findAxeSlot(PlayerInventory inv) {
		for (int i = 0; i < 9; i++) {
			if (CombatUtil.isAxe(inv.getStack(i))) return i;
		}
		return -1;
	}
}
