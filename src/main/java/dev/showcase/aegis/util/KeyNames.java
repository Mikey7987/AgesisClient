package dev.showcase.aegis.util;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Human readable names for GLFW key codes (kept independent of the version-specific InputUtil API). */
public final class KeyNames {
	private static final Map<Integer, String> NAMES = new HashMap<>();

	static {
		NAMES.put(GLFW.GLFW_KEY_SPACE, "SPACE");
		NAMES.put(GLFW.GLFW_KEY_ESCAPE, "ESC");
		NAMES.put(GLFW.GLFW_KEY_ENTER, "ENTER");
		NAMES.put(GLFW.GLFW_KEY_TAB, "TAB");
		NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "BACKSPACE");
		NAMES.put(GLFW.GLFW_KEY_INSERT, "INSERT");
		NAMES.put(GLFW.GLFW_KEY_DELETE, "DELETE");
		NAMES.put(GLFW.GLFW_KEY_RIGHT, "RIGHT");
		NAMES.put(GLFW.GLFW_KEY_LEFT, "LEFT");
		NAMES.put(GLFW.GLFW_KEY_DOWN, "DOWN");
		NAMES.put(GLFW.GLFW_KEY_UP, "UP");
		NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "PGUP");
		NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "PGDN");
		NAMES.put(GLFW.GLFW_KEY_HOME, "HOME");
		NAMES.put(GLFW.GLFW_KEY_END, "END");
		NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "CAPS");
		NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT");
		NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT");
		NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCTRL");
		NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCTRL");
		NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "LALT");
		NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "RALT");
		for (int i = 0; i < 12; i++) {
			NAMES.put(GLFW.GLFW_KEY_F1 + i, "F" + (i + 1));
		}
		for (int i = 0; i <= 9; i++) {
			NAMES.put(GLFW.GLFW_KEY_KP_0 + i, "NUM" + i);
		}
	}

	private KeyNames() {}

	public static String name(int key) {
		if (key < 0) return "NONE";
		String fixed = NAMES.get(key);
		if (fixed != null) return fixed;
		String n = GLFW.glfwGetKeyName(key, 0);
		if (n != null && !n.isEmpty()) return n.toUpperCase();
		return "KEY" + key;
	}
}
