package dev.showcase.aegis.module.render;

import dev.showcase.aegis.gui.Theme;
import dev.showcase.aegis.module.Category;
import dev.showcase.aegis.module.Module;
import dev.showcase.aegis.module.ModuleManager;
import dev.showcase.aegis.setting.ToggleSetting;
import dev.showcase.aegis.util.MathUtil;
import dev.showcase.aegis.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** HUD list of enabled modules with slide-in animation. */
public final class ModuleList extends Module {
	public final ToggleSetting background = add(new ToggleSetting("Background", true));

	public ModuleList() {
		super("Module List", "Shows enabled modules on the HUD.", Category.RENDER);
		setEnabled(true);
	}

	@Override
	public void onHud(DrawContext ctx, int sw, int sh, float dt) {
		hudAnim = isEnabled() ? 1f : 0f;
		if (!isEnabled()) return;

		List<Module> list = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			// Render-category modules (this one, Target HUD) drive their own hudAnim.
			if (m.category == Category.RENDER) continue;
			m.hudAnim = MathUtil.lerp(m.hudAnim, m.isEnabled() ? 1f : 0f, 14f, dt);
			if (m.hudAnim > 0.02f) list.add(m);
		}
		list.sort(Comparator.comparingInt((Module m) -> -RenderUtil.textWidth(m.name)));

		RenderUtil.setAlpha(1f);
		int y = 4;
		for (Module m : list) {
			int tw = RenderUtil.textWidth(m.name);
			int w = tw + 10;
			int slide = (int) ((1f - MathUtil.easeOutCubic(m.hudAnim)) * (w + 6));
			int x = sw - w - 4 + slide;
			RenderUtil.setAlpha(m.hudAnim);
			if (background.isOn()) RenderUtil.roundRect(ctx, x, y, w, 13, 3, 0xA0101014);
			RenderUtil.roundRect(ctx, x + w - 2, y + 2, 2, 9, 1, Theme.ACCENT);
			RenderUtil.text(ctx, m.name, x + 4, y + 3, Theme.TEXT);
			y += 14;
		}
		RenderUtil.setAlpha(1f);
	}
}
