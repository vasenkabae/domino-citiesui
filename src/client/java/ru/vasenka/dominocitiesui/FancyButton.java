package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Кнопка в стиле K-меню (тёмное стекло + золото) вместо ванильной серой Button.
 * Ванильный AbstractButton красить нельзя — его extractWidgetRenderState финальный,
 * поэтому наследуемся сразу от AbstractWidget и рисуем всё сами.
 * Клик обрабатывает AbstractWidget.mouseClicked (границы/active/звук) — мы только onClick.
 *
 * Эффекты: наведение анимируется плавно (цвета и золотая полоска подъезжают за ~120 мс),
 * клик даёт золотую вспышку и коротко «проседает» текст на 1px — кнопка ощущается живой.
 */
public class FancyButton extends AbstractWidget {
    private static final int BG_TOP        = 0xE01E2129;
    private static final int BG_BOTTOM     = 0xE9121419;
    private static final int BG_TOP_HOT    = 0xF0282C38;
    private static final int BG_BOTTOM_HOT = 0xF01A1D26;
    private static final int EDGE          = 0x2EFFFFFF;
    private static final int EDGE_HOT      = 0x66F2B94E;
    private static final int GOLD          = 0xFFF2B94E;
    private static final int TEXT          = 0xFFF2F3F5;
    private static final int TEXT_HOT      = 0xFFFFD37A;
    private static final int TEXT_OFF      = 0xFF767D8A;

    private static final int FLASH_MS = 220; // длительность вспышки после клика
    private static final int SINK_MS  = 90;  // сколько текст «утоплен» после клика

    private final Runnable onPress;

    private float hoverT;      // 0..1 — анимированная степень наведения
    private long lastFrameMs;  // прошлый кадр — для дельты анимации
    private long pressedAtMs;  // момент клика — вспышка и просадка

    public FancyButton(int x, int y, int w, int h, Component label, Runnable onPress) {
        super(x, y, w, h, label);
        this.onPress = onPress;
    }

    public static FancyButton of(Component label, int x, int y, int w, int h, Runnable onPress) {
        return new FancyButton(x, y, w, h, label, onPress);
    }

    /**
     * Стандартный звук нажатия для кликабельных областей, которые не виджеты
     * (вкладки, узлы древа, строки списков). Дёргает ванильный playDownSound
     * через одноразовый экземпляр — не полагаемся на прямой доступ к SoundEvents.
     */
    private static FancyButton soundSource;

    public static void uiClick() {
        if (soundSource == null) soundSource = new FancyButton(0, 0, 1, 1, Component.empty(), () -> {});
        soundSource.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    /** Покомпонентная линейная интерполяция ARGB. */
    private static int mix(int a, int b, float t) {
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0 : Math.min(50, now - lastFrameMs) / 1000f;
        lastFrameMs = now;
        boolean hot = this.active && isHoveredOrFocused();
        // Экспоненциальное приближение к цели: быстро, но без рывка.
        hoverT += (float) (((hot ? 1f : 0f) - hoverT) * Math.min(1f, dt * 14f));
        if (hoverT < 0.004f) hoverT = 0;
        else if (hoverT > 0.996f) hoverT = 1;

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        long sincePress = now - pressedAtMs;
        int sink = pressedAtMs != 0 && sincePress < SINK_MS ? 1 : 0;

        g.fillGradient(x, y, x + w, y + h,
                mix(BG_TOP, BG_TOP_HOT, hoverT), mix(BG_BOTTOM, BG_BOTTOM_HOT, hoverT));
        g.outline(x, y, w, h, mix(EDGE, EDGE_HOT, hoverT));
        // Золотая полоска растёт из центра при наведении.
        int uw = (int) ((w - 2) * hoverT);
        if (uw > 0) {
            int ux = x + (w - uw) / 2;
            g.fill(ux, y + h - 2, ux + uw, y + h - 1, GOLD);
        }

        Font font = Minecraft.getInstance().font;
        String label = getMessage().getString();
        int maxW = w - 8;
        while (font.width(label) > maxW && label.length() > 1) {
            label = label.substring(0, label.length() - 1);
        }
        int tx = x + Math.max(4, (w - font.width(label)) / 2);
        int ty = y + (h - 8) / 2 + sink;
        g.text(font, label, tx, ty, !this.active ? TEXT_OFF : mix(TEXT, TEXT_HOT, hoverT));

        // Вспышка после клика — затухающий золотой отблеск поверх кнопки.
        if (pressedAtMs != 0 && sincePress < FLASH_MS) {
            int alpha = (int) (0x58 * (1 - sincePress / (float) FLASH_MS));
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, (alpha << 24) | 0xF2B94E);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active) return;
        pressedAtMs = System.currentTimeMillis();
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
