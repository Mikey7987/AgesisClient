package dev.showcase.aegis.module.render;

import dev.showcase.aegis.gui.Theme;
import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.util.CombatUtil;
import dev.showcase.aegis.util.MathUtil;
import dev.showcase.aegis.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

/** Small panel next to the crosshair showing the looked-at entity's name and health. */
public final class TargetHud extends Module {
	private LivingEntity shown;
	private float showAnim;
	private float healthAnim;

	public TargetHud() {
		super("Target HUD", "Shows name and health of the entity under the crosshair.", Category.RENDER);
		setEnabled(true);
	}

	@Override
	public void onHud(DrawContext ctx, int sw, int sh, float dt) {
		LivingEntity looked = isEnabled() ? CombatUtil.crosshairLiving(mc) : null;
		if (looked != null) {
			if (looked != shown) healthAnim = looked.getHealth();
			shown = looked;
		}
		showAnim = MathUtil.lerp(showAnim, looked != null ? 1f : 0f, 12f, dt);
		hudAnim = showAnim;
		if (showAnim < 0.02f || shown == null) return;

		float max = Math.max(1f, shown.getMaxHealth());
		float hp = Math.max(0f, shown.getHealth());
		healthAnim = MathUtil.lerp(healthAnim, hp, 10f, dt);

		int w = 124, h = 34;
		int x = sw / 2 + 22;
		int y = sh / 2 + 18 + (int) ((1f - showAnim) * 6f);

		RenderUtil.setAlpha(showAnim);
		RenderUtil.roundRect(ctx, x, y, w, h, 6, 0xC8101014);
		RenderUtil.text(ctx, shown.getName().getString(), x + 8, y + 6, Theme.TEXT);

		int bx = x + 8, by = y + 20, bw = w - 16;
		RenderUtil.roundRect(ctx, bx, by, bw, 5, 2, Theme.TRACK);
		int fill = Math.max(4, (int) (bw * MathUtil.clamp(healthAnim / max, 0f, 1f)));
		int color = RenderUtil.lerpColor(0xFFE5484D, 0xFF46D17B, MathUtil.clamp(healthAnim / max, 0f, 1f));
		RenderUtil.roundRect(ctx, bx, by, fill, 5, 2, color);
		String hpText = String.format(Locale.ROOT, "%.1f", hp);
		RenderUtil.textRight(ctx, hpText, x + w - 8, y + 6, Theme.TEXT_DIM);
		RenderUtil.setAlpha(1f);
	}
}
