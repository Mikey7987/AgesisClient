package dev.showcase.aegis.gui;

import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.module.ModuleManager;
import dev.showcase.aegis.setting.ModeSetting;
import dev.showcase.aegis.setting.Setting;
import dev.showcase.aegis.setting.SliderSetting;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.KeyNames;
import dev.showcase.aegis.util.MathUtil;
import dev.showcase.aegis.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Immediate-mode, dark ClickGUI. Widgets draw and handle their own input in the same pass.
 *
 * <p>Mouse buttons and keys are polled from GLFW once per frame (edge detected) instead of
 * overriding the event methods, which keeps this screen independent of the input-event
 * signatures that changed across 1.21.x. Only the mouse wheel uses a Screen override.
 */
public final class ClickGuiScreen extends Screen {
	private static final int PANEL_W = 540;
	private static final int PANEL_H = 340;
	private static final int SIDEBAR_W = 124;
	private static final int HEADER_H = 28;
	private static final int CARD_GAP = 6;
	private static final int BODY_PAD = 5;
	private static final int SLIDER_H = 30;
	private static final int ROW_H = 20;
	private static final long DOUBLE_CLICK_NS = 300_000_000L;

	private static int selected = 0;

	private final MinecraftClient mc = MinecraftClient.getInstance();
	private final Category[] categories = Category.values();

	// animation state
	private float openAnim;
	private boolean closing;
	private long lastNanos = System.nanoTime();
	private float tabIndicatorY = -1f;
	private final float[] tabHover = new float[Category.values().length];
	private final float[] scroll = new float[Category.values().length];
	private final float[] scrollTarget = new float[Category.values().length];
	private final float[] contentHeight = new float[Category.values().length];

	// input state
	private int mx, my;
	private boolean prevLeft, prevRight, leftHeld, leftPressed, rightPressed;
	private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
	private boolean prevEsc, prevGuiKey;
	private Module listening;
	private SliderSetting dragging;
	private Setting<?> lastClicked;
	private long lastClickNs;

	// layout of the scrolling list (recomputed each frame)
	private int listX, listY, listW, listH;
	private String hoverDescription = "";

	public ClickGuiScreen() {
		super(Text.literal("Aegis"));
		long h = mc.getWindow().getHandle();
		prevLeft = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		prevRight = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		prevEsc = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
		// RIGHT_SHIFT is physically down right now (it just opened us); don't close on the same press.
		prevGuiKey = true;
		for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
			prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
		}
	}

	// ---- Screen overrides -----------------------------------------------------------------------

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // ESC is polled so it can also cancel keybind listening
	}

	@Override
	public void close() {
		closing = true;
	}

	@Override
	public void tick() {
		if (closing && openAnim < 0.03f) {
			RenderUtil.setAlpha(1f);
			mc.setScreen(null);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (inList(mouseX, mouseY)) {
			scrollTarget[selected] -= (float) verticalAmount * 28f;
			return true;
		}
		return false;
	}

	// ---- frame ----------------------------------------------------------------------------------

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float dt = MathUtil.clamp((now - lastNanos) / 1_000_000_000f, 0f, 0.1f);
		lastNanos = now;
		mx = mouseX;
		my = mouseY;
		pollInput();

		openAnim = MathUtil.lerp(openAnim, closing ? 0f : 1f, 16f, dt);
		float eased = MathUtil.easeOutCubic(openAnim);

		RenderUtil.setAlpha(1f);
		ctx.fill(0, 0, width, height, ((int) (120 * eased)) << 24);

		float scale = 0.9f + 0.1f * eased;
		float cx = width / 2f;
		float cy = height / 2f;
		ctx.getMatrices().pushMatrix();
		ctx.getMatrices().translate(cx, cy);
		ctx.getMatrices().scale(scale, scale);
		ctx.getMatrices().translate(-cx, -cy);
		RenderUtil.setAlpha(eased);
		try {
			drawPanel(ctx, dt);
		} finally {
			ctx.getMatrices().popMatrix();
			RenderUtil.setAlpha(1f);
		}
	}

	private void pollInput() {
		long h = mc.getWindow().getHandle();
		boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		leftPressed = l && !prevLeft;
		rightPressed = r && !prevRight;
		leftHeld = l;
		prevLeft = l;
		prevRight = r;

		boolean esc = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
		boolean guiKey = GLFW.glfwGetKey(h, ModuleManager.GUI_KEY) == GLFW.GLFW_PRESS;

		if (listening != null) {
			for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
				boolean down = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
				if (down && !prevKeys[k]) {
					listening.setKey(k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_DELETE ? -1 : k);
					listening = null;
					break;
				}
			}
		} else if ((esc && !prevEsc) || (guiKey && !prevGuiKey)) {
			closing = true;
		}

		prevEsc = esc;
		prevGuiKey = guiKey;
		for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
			prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
		}
	}

	private boolean inList(double x, double y) {
		return x >= listX && x < listX + listW && y >= listY && y < listY + listH;
	}

	private static boolean inRect(int px, int py, int x, int y, int w, int h) {
		return px >= x && px < x + w && py >= y && py < y + h;
	}

	// ---- panel ----------------------------------------------------------------------------------

	private void drawPanel(DrawContext ctx, float dt) {
		int px = (width - PANEL_W) / 2;
		int py = (height - PANEL_H) / 2;
		hoverDescription = "";

		RenderUtil.roundRect(ctx, px - 3, py - 3, PANEL_W + 6, PANEL_H + 6, 11, Theme.SHADOW);
		RenderUtil.roundRect(ctx, px, py, PANEL_W, PANEL_H, 9, Theme.PANEL);

		drawSidebar(ctx, px, py, dt);

		int contentX = px + SIDEBAR_W + 6;
		int contentW = PANEL_W - SIDEBAR_W - 16;
		Category cat = categories[selected];

		RenderUtil.text(ctx, cat.label, contentX, py + 16, Theme.TEXT);
		RenderUtil.textRight(ctx, "Right-click a module to configure", contentX + contentW, py + 16, Theme.TEXT_DIM);

		listX = contentX;
		listY = py + 36;
		listW = contentW;
		listH = PANEL_H - 36 - 30;

		drawList(ctx, cat, dt);

		RenderUtil.text(ctx, hoverDescription.isEmpty() ? "RIGHT SHIFT or ESC to close" : hoverDescription,
				contentX, py + PANEL_H - 20, Theme.TEXT_DIM);
	}

	private void drawSidebar(DrawContext ctx, int px, int py, float dt) {
		RenderUtil.roundRect(ctx, px + 6, py + 6, SIDEBAR_W - 8, PANEL_H - 12, 7, Theme.SIDEBAR);
		RenderUtil.text(ctx, "AEGIS", px + 20, py + 20, Theme.ACCENT);
		RenderUtil.text(ctx, "showcase build", px + 20, py + 32, Theme.TEXT_DIM);

		int tabY0 = py + 62;
		float targetY = tabY0 + selected * 30;
		tabIndicatorY = tabIndicatorY < 0 ? targetY : MathUtil.lerp(tabIndicatorY, targetY, 16f, dt);
		int indY = Math.round(tabIndicatorY);
		RenderUtil.roundRect(ctx, px + 12, indY, SIDEBAR_W - 20, 26, 6, Theme.ACCENT_SOFT);
		RenderUtil.roundRect(ctx, px + 12, indY + 6, 3, 14, 1, Theme.ACCENT);

		for (int i = 0; i < categories.length; i++) {
			int ty = tabY0 + i * 30;
			boolean hover = inRect(mx, my, px + 12, ty, SIDEBAR_W - 20, 26);
			tabHover[i] = MathUtil.lerp(tabHover[i], hover ? 1f : 0f, 14f, dt);
			float lit = Math.max(tabHover[i], i == selected ? 1f : 0f);
			RenderUtil.text(ctx, categories[i].label, px + 24 + (int) (tabHover[i] * 2f), ty + 9,
					RenderUtil.lerpColor(Theme.TEXT_DIM, Theme.TEXT, lit));
			if (hover && leftPressed) selected = i;
		}
	}

	private void drawList(DrawContext ctx, Category cat, float dt) {
		float maxScroll = Math.max(0f, contentHeight[selected] - listH);
		scrollTarget[selected] = MathUtil.clamp(scrollTarget[selected], 0f, maxScroll);
		scroll[selected] = MathUtil.lerp(scroll[selected], scrollTarget[selected], 16f, dt);

		int cardW = listW - 8;
		int startY = listY - Math.round(scroll[selected]);
		int y = startY;

		RenderUtil.scissor(ctx, listX, listY, listW, listH);
		try {
			List<Module> modules = ModuleManager.byCategory(cat);
			for (Module m : modules) {
				int h = drawModule(ctx, m, listX, y, cardW, dt);
				y += h + CARD_GAP;
			}
		} finally {
			RenderUtil.endScissor(ctx);
		}
		contentHeight[selected] = y - startY;

		// scrollbar
		if (maxScroll > 0f) {
			float ratio = listH / contentHeight[selected];
			int thumbH = Math.max(18, (int) (listH * ratio));
			float t = scroll[selected] / maxScroll;
			int thumbY = listY + (int) ((listH - thumbH) * t);
			RenderUtil.roundRect(ctx, listX + listW - 3, thumbY, 3, thumbH, 1, Theme.TRACK_HOVER);
		}
	}

	// ---- module card ----------------------------------------------------------------------------

	private int bodyHeight(Module m) {
		int h = 0;
		for (Setting<?> s : m.settings) {
			if (!s.isVisible()) continue;
			h += s instanceof SliderSetting ? SLIDER_H : ROW_H;
		}
		h += ROW_H + 2; // keybind row
		return h + BODY_PAD * 2;
	}

	private int drawModule(DrawContext ctx, Module m, int x, int y, int w, float dt) {
		m.expandAnim = MathUtil.lerp(m.expandAnim, m.expanded ? 1f : 0f, 14f, dt);
		m.toggleAnim = MathUtil.lerp(m.toggleAnim, m.isEnabled() ? 1f : 0f, 16f, dt);
		int visibleBody = Math.round(bodyHeight(m) * m.expandAnim);
		int totalH = HEADER_H + visibleBody;

		// Off-screen cards only need their height for layout.
		if (y + totalH < listY || y > listY + listH) return totalH;

		boolean headerHover = inList(mx, my) && inRect(mx, my, x, y, w, HEADER_H);
		if (headerHover) hoverDescription = m.description;

		RenderUtil.roundRect(ctx, x, y, w, totalH, 7, headerHover ? Theme.CARD_HOVER : Theme.CARD);
		if (m.toggleAnim > 0.02f) {
			RenderUtil.roundRect(ctx, x, y + 6, 3, HEADER_H - 12, 1, RenderUtil.withAlpha(Theme.ACCENT, (int) (255 * m.toggleAnim)));
		}

		// name + bind hint
		int nameColor = RenderUtil.lerpColor(Theme.TEXT_DIM, Theme.TEXT, m.toggleAnim);
		RenderUtil.text(ctx, m.name, x + 12, y + 10, nameColor);
		if (m.getKey() >= 0) {
			RenderUtil.text(ctx, "[" + KeyNames.name(m.getKey()) + "]", x + 12 + RenderUtil.textWidth(m.name) + 6, y + 10, Theme.TEXT_DIM);
		}

		// toggle pill
		int pillX = x + w - 40;
		int pillY = y + 7;
		RenderUtil.pill(ctx, pillX, pillY, 28, 14, RenderUtil.lerpColor(Theme.OFF, Theme.ACCENT, m.toggleAnim));
		RenderUtil.circle(ctx, pillX + 7 + Math.round(14 * m.toggleAnim), pillY + 7, 5, Theme.KNOB);

		// expand chevron
		int chevX = x + w - 64;
		boolean chevHover = headerHover && inRect(mx, my, chevX - 4, y, 24, HEADER_H);
		RenderUtil.text(ctx, m.expanded ? "v" : ">", chevX + 4, y + 10, chevHover ? Theme.TEXT : Theme.TEXT_DIM);

		if (headerHover) {
			if (rightPressed) {
				m.expanded = !m.expanded;
			} else if (leftPressed) {
				if (chevHover) m.expanded = !m.expanded;
				else m.toggle();
			}
		}

		// settings body
		if (visibleBody > 2) {
			int bodyY = y + HEADER_H;
			boolean bodyActive = inList(mx, my) && inRect(mx, my, x, bodyY, w, visibleBody);
			RenderUtil.scissor(ctx, x, bodyY, w, visibleBody);
			try {
				int rx = x + 12;
				int rw = w - 24;
				int ry = bodyY + BODY_PAD;
				for (Setting<?> s : m.settings) {
					if (!s.isVisible()) continue;
					if (s instanceof SliderSetting slider) {
						drawSlider(ctx, slider, rx, ry, rw, bodyActive, dt);
						ry += SLIDER_H;
					} else if (s instanceof ToggleSetting toggle) {
						drawToggle(ctx, toggle, rx, ry, rw, bodyActive, dt);
						ry += ROW_H;
					} else if (s instanceof ModeSetting mode) {
						drawMode(ctx, mode, rx, ry, rw, bodyActive, dt);
						ry += ROW_H;
					}
				}
				drawKeybind(ctx, m, rx, ry + 2, rw, bodyActive);
			} finally {
				RenderUtil.endScissor(ctx);
			}
		}
		return totalH;
	}

	// ---- widgets --------------------------------------------------------------------------------

	private void drawSlider(DrawContext ctx, SliderSetting s, int x, int y, int w, boolean active, float dt) {
		boolean hover = active && inRect(mx, my, x, y, w, SLIDER_H);
		boolean isDragging = dragging == s;

		RenderUtil.text(ctx, s.name, x, y + 3, hover || isDragging ? Theme.TEXT : Theme.TEXT_DIM);
		RenderUtil.textRight(ctx, s.format(), x + w, y + 3, Theme.TEXT);

		int trackY = y + 18;
		int trackH = 5;
		s.anim = MathUtil.lerp(s.anim, s.fraction(), 20f, dt);
		int fillW = Math.max(trackH, Math.round(w * MathUtil.clamp(s.anim, 0f, 1f)));

		RenderUtil.roundRect(ctx, x, trackY, w, trackH, 2, hover || isDragging ? Theme.TRACK_HOVER : Theme.TRACK);
		RenderUtil.roundRect(ctx, x, trackY, fillW, trackH, 2, Theme.ACCENT);
		RenderUtil.circle(ctx, Math.max(x + 4, Math.min(x + w - 4, x + fillW)), trackY + 2, hover || isDragging ? 5 : 4, Theme.KNOB);

		if (hover && leftPressed) {
			long now = System.nanoTime();
			if (lastClicked == s && now - lastClickNs < DOUBLE_CLICK_NS) {
				s.reset(); // double-click resets
				lastClicked = null;
				dragging = null;
			} else {
				dragging = s;
				lastClicked = s;
				lastClickNs = now;
			}
		}
		if (hover && rightPressed) s.reset();
		if (dragging == s) {
			if (leftHeld) s.setFraction((mx - x) / (double) w);
			else dragging = null;
		}
	}

	private void drawToggle(DrawContext ctx, ToggleSetting s, int x, int y, int w, boolean active, float dt) {
		boolean hover = active && inRect(mx, my, x, y, w, ROW_H);
		s.anim = MathUtil.lerp(s.anim, s.isOn() ? 1f : 0f, 18f, dt);
		if (hover) RenderUtil.roundRect(ctx, x - 4, y, w + 8, ROW_H, 4, Theme.ROW_HOVER);

		RenderUtil.text(ctx, s.name, x, y + 6, hover ? Theme.TEXT : Theme.TEXT_DIM);
		int pillX = x + w - 26;
		RenderUtil.pill(ctx, pillX, y + 4, 26, 12, RenderUtil.lerpColor(Theme.OFF, Theme.ACCENT, s.anim));
		RenderUtil.circle(ctx, pillX + 6 + Math.round(14 * s.anim), y + 10, 4, Theme.KNOB);

		if (hover && leftPressed) s.toggle();
	}

	private void drawMode(DrawContext ctx, ModeSetting s, int x, int y, int w, boolean active, float dt) {
		boolean hover = active && inRect(mx, my, x, y, w, ROW_H);
		s.anim = MathUtil.lerp(s.anim, 0f, 9f, dt);
		if (hover) RenderUtil.roundRect(ctx, x - 4, y, w + 8, ROW_H, 4, Theme.ROW_HOVER);

		RenderUtil.text(ctx, s.name, x, y + 6, hover ? Theme.TEXT : Theme.TEXT_DIM);
		String label = s.get();
		int pw = RenderUtil.textWidth(label) + 14;
		int pillX = x + w - pw;
		RenderUtil.pill(ctx, pillX, y + 3, pw, 14, RenderUtil.lerpColor(Theme.TRACK, Theme.ACCENT, s.anim));
		RenderUtil.textCentered(ctx, label, pillX + pw / 2, y + 6, Theme.TEXT);

		if (hover && leftPressed) s.next();
		else if (hover && rightPressed) s.prev();
	}

	private void drawKeybind(DrawContext ctx, Module m, int x, int y, int w, boolean active) {
		boolean hover = active && inRect(mx, my, x, y, w, ROW_H);
		boolean isListening = listening == m;
		if (hover) RenderUtil.roundRect(ctx, x - 4, y, w + 8, ROW_H, 4, Theme.ROW_HOVER);

		RenderUtil.text(ctx, "Keybind", x, y + 6, hover ? Theme.TEXT : Theme.TEXT_DIM);
		String label = isListening ? "press a key..." : KeyNames.name(m.getKey());
		int bw = Math.max(54, RenderUtil.textWidth(label) + 14);
		int bx = x + w - bw;
		RenderUtil.roundRect(ctx, bx, y + 3, bw, 14, 5, isListening ? Theme.ACCENT : Theme.TRACK);
		RenderUtil.textCentered(ctx, label, bx + bw / 2, y + 6, Theme.TEXT);

		if (hover && leftPressed) {
			listening = isListening ? null : m;
		} else if (hover && rightPressed) {
			m.setKey(-1);
			listening = null;
		}
	}
}
