package dev.showcase.aegis.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Immediate-mode drawing helpers on top of {@link DrawContext}: rounded rectangles,
 * circles, scissor clipping and a global alpha multiplier used for fade transitions.
 */
public final class RenderUtil {
	private static float globalAlpha = 1f;

	private RenderUtil() {}

	public static void setAlpha(float a) {
		globalAlpha = MathUtil.clamp(a, 0f, 1f);
	}

	public static float getAlpha() {
		return globalAlpha;
	}

	/** Applies the global alpha multiplier to an ARGB colour. */
	public static int apply(int argb) {
		int a = (int) (((argb >>> 24) & 0xFF) * globalAlpha);
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	public static int withAlpha(int argb, int alpha) {
		return (MathUtil.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
	}

	public static int lerpColor(int from, int to, float t) {
		t = MathUtil.clamp(t, 0f, 1f);
		int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
		int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
		int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
		int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
		int c = apply(color);
		if ((c >>> 24) == 0 || x2 <= x1 || y2 <= y1) return;
		ctx.fill(x1, y1, x2, y2, c);
	}

	/** Filled rounded rectangle built from at most 2r+1 scanline fills. */
	public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
		if (w <= 0 || h <= 0) return;
		int c = apply(color);
		if ((c >>> 24) == 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) {
			ctx.fill(x, y, x + w, y + h, c);
			return;
		}
		for (int i = 0; i < r; i++) {
			double d = r - i - 0.5;
			int inset = (int) Math.ceil(r - Math.sqrt(r * (double) r - d * d));
			ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
			ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
		}
		ctx.fill(x, y + r, x + w, y + h - r, c);
	}

	public static void pill(DrawContext ctx, int x, int y, int w, int h, int color) {
		roundRect(ctx, x, y, w, h, h / 2, color);
	}

	public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
		roundRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	/** Begins clipping (GUI-scaled coordinates). Always pair with {@link #endScissor}. */
	public static void scissor(DrawContext ctx, int x, int y, int w, int h) {
		ctx.enableScissor(x, y, x + Math.max(1, w), y + Math.max(1, h));
	}

	public static void endScissor(DrawContext ctx) {
		ctx.disableScissor();
	}

	// ---- text ----------------------------------------------------------------------------------

	public static int textWidth(String s) {
		return MinecraftClient.getInstance().textRenderer.getWidth(s);
	}

	public static void text(DrawContext ctx, String s, int x, int y, int color) {
		int c = apply(color);
		if ((c >>> 24) < 4) return;
		ctx.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, c, false);
	}

	public static void textRight(DrawContext ctx, String s, int rightX, int y, int color) {
		text(ctx, s, rightX - textWidth(s), y, color);
	}

	public static void textCentered(DrawContext ctx, String s, int centerX, int y, int color) {
		text(ctx, s, centerX - textWidth(s) / 2, y, color);
	}
}
