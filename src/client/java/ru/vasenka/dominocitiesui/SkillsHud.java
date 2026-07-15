package ru.vasenka.dominocitiesui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Iterator;

/**
 * Тосты профессий: «+N Шахтёр» справа от хотбара (сливаются по профессии),
 * левел-ап — крупно по центру. Данные копятся в SkillsData из пакетов dominoskills:xp.
 */
public class SkillsHud implements HudElement {

    private static final long XP_LIFE_MS = 3000;
    private static final long XP_FADE_MS = 700;
    private static final long LEVEL_LIFE_MS = 3500;

    private static final int GOLD = 0xF2B94E;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        long now = System.currentTimeMillis();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // «+N Профессия» — колонка справа от хотбара, свежие снизу.
        int shown = 0;
        Iterator<SkillsData.XpToast> it = SkillsData.xpToasts.iterator();
        while (it.hasNext()) {
            SkillsData.XpToast t = it.next();
            long age = now - t.time;
            if (age > XP_LIFE_MS) { it.remove(); continue; }
            if (shown >= 3) continue;
            int alpha = 255;
            if (age > XP_LIFE_MS - XP_FADE_MS)
                alpha = (int) (255 * (XP_LIFE_MS - age) / XP_FADE_MS);
            if (alpha < 8) continue;
            String text = "+" + t.amount + " " + SkillsData.PROF_TITLES[t.profId];
            int x = sw / 2 + 100;
            int y = sh - 48 - shown * 11;
            g.text(mc.font, text, x, y, (alpha << 24) | 0xF2F3F5);
            shown++;
        }

        // Левел-ап: одно сообщение за раз, по центру над серединой экрана.
        Iterator<SkillsData.LevelToast> lit = SkillsData.levelToasts.iterator();
        while (lit.hasNext()) {
            SkillsData.LevelToast t = lit.next();
            long age = now - t.time();
            if (age > LEVEL_LIFE_MS) { lit.remove(); continue; }
            int alpha = 255;
            if (age > LEVEL_LIFE_MS - XP_FADE_MS)
                alpha = (int) (255 * (LEVEL_LIFE_MS - age) / XP_FADE_MS);
            if (alpha < 8) continue;
            String title = SkillsData.PROF_TITLES[t.profId()] + " — уровень " + t.level() + "!";
            int cx = sw / 2;
            int y = sh / 3;
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx, y);
            pose.scale(1.4f, 1.4f);
            g.centeredText(mc.font, title, 0, 0, (alpha << 24) | GOLD);
            pose.popMatrix();
            g.centeredText(mc.font, "Открыты новые таланты — клавиша N",
                    cx, y + 16, (Math.max(alpha - 60, 0) << 24) | 0xA7ADB8);
            break; // только самый свежий
        }
    }
}
