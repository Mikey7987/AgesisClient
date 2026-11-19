package dev.showcase.aegis.module;

import dev.showcase.aegis.client.AegisClient;
import dev.showcase.aegis.gui.ClickGuiScreen;
import dev.showcase.aegis.module.combat.AimAssist;
import dev.showcase.aegis.module.combat.Hitbox;
import dev.showcase.aegis.module.combat.ShieldBreaker;
import dev.showcase.aegis.module.combat.TriggerBot;
import dev.showcase.aegis.module.movement.AutoSprint;
import dev.showcase.aegis.module.movement.SneakHold;
import dev.showcase.aegis.module.player.AntiAfk;
import dev.showcase.aegis.module.player.AutoRespawn;
import dev.showcase.aegis.module.render.ModuleList;
import dev.showcase.aegis.module.render.TargetHud;
import dev.showcase.aegis.util.CombatUtil;
import dev.showcase.aegis.util.MathUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns every module, dispatches tick / frame / HUD callbacks and polls module keybinds. */
public final class ModuleManager {
	/** Key that opens the ClickGUI. */
	public static final int GUI_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<Class<?>, Module> BY_CLASS = new HashMap<>();
	private static final boolean[] PREV_KEYS = new boolean[GLFW.GLFW_KEY_LAST + 1];
	private static final Set<Module> FAILED = new HashSet<>();
	private static long lastFrameNanos = System.nanoTime();
	private static long lastHudNanos = System.nanoTime();

	private ModuleManager() {}

	public static void init() {
		// Combat
		register(new AimAssist());
		register(new TriggerBot());
		register(new Hitbox());
		register(new ShieldBreaker());
		// Render
		register(new ModuleList());
		register(new TargetHud());
		// Player
		register(new AutoRespawn());
		register(new AntiAfk());
		// Movement
		register(new AutoSprint());
		register(new SneakHold());

		// Feeds AimAssist's "Last Hit" priority.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient() && entity instanceof LivingEntity) {
				CombatUtil.recordHit(entity);
			}
			return ActionResult.PASS;
		});
	}

	private static void register(Module m) {
		MODULES.add(m);
		BY_CLASS.put(m.getClass(), m);
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> byCategory(Category category) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) out.add(m);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T get(Class<T> type) {
		return (T) BY_CLASS.get(type);
	}

	// ---- dispatch ------------------------------------------------------------------------------

	/** Called from {@code MinecraftClient.render} (once per rendered frame). */
	public static void onFrame() {
		MinecraftClient mc = MinecraftClient.getInstance();
		long now = System.nanoTime();
		float dt = MathUtil.clamp((now - lastFrameNanos) / 1_000_000_000f, 0f, 0.1f);
		lastFrameNanos = now;

		pollKeys(mc);

		for (Module m : MODULES) {
			if (!m.isEnabled() || FAILED.contains(m)) continue;
			try {
				m.onFrame(dt);
			} catch (Throwable t) {
				disableAfterError(m, t);
			}
		}
	}

	public static void onTick() {
		for (Module m : MODULES) {
			if (!m.isEnabled() || FAILED.contains(m)) continue;
			try {
				m.onTick();
			} catch (Throwable t) {
				disableAfterError(m, t);
			}
		}
	}

	public static void onHud(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		long now = System.nanoTime();
		float dt = MathUtil.clamp((now - lastHudNanos) / 1_000_000_000f, 0f, 0.1f);
		lastHudNanos = now;
		if (mc.options.hudHidden || mc.player == null) return;

		int w = mc.getWindow().getScaledWidth();
		int h = mc.getWindow().getScaledHeight();
		for (Module m : MODULES) {
			// TargetHud animates its own fade-out, so it is called even when it just got disabled.
			if (!m.isEnabled() && m.hudAnim <= 0.01f) continue;
			if (FAILED.contains(m)) continue;
			try {
				m.onHud(ctx, w, h, dt);
			} catch (Throwable t) {
				disableAfterError(m, t);
			}
		}
	}

	private static void disableAfterError(Module m, Throwable t) {
		AegisClient.LOGGER.error("Module {} crashed and was disabled", m.name, t);
		FAILED.add(m);
		try {
			m.setEnabled(false);
		} catch (Throwable ignored) {
			// nothing else to do
		}
		FAILED.remove(m); // allow the user to try again from the GUI
	}

	// ---- keybinds ------------------------------------------------------------------------------

	/**
	 * Polls every key each frame (so quick taps are never missed between 20 Hz ticks) and fires
	 * on the rising edge. Previous states are always updated so a key that was held while a screen
	 * was open or while binding cannot trigger a module afterwards.
	 */
	private static void pollKeys(MinecraftClient mc) {
		if (mc.getWindow() == null) return;
		long handle = mc.getWindow().getHandle();
		boolean blocked = mc.currentScreen != null || mc.player == null;
		for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
			boolean down = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
			if (down && !PREV_KEYS[k] && !blocked) {
				onKeyPressed(mc, k);
			}
			PREV_KEYS[k] = down;
		}
	}

	private static void onKeyPressed(MinecraftClient mc, int key) {
		if (key == GUI_KEY) {
			mc.setScreen(new ClickGuiScreen());
			return;
		}
		for (Module m : MODULES) {
			if (m.getKey() == key) m.toggle();
		}
	}
}
