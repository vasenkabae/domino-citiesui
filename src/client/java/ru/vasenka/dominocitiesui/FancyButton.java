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

    private final Runnable onPress;

    public FancyButton(int x, int y, int w, int h, Component label, Runnable onPress) {
        super(x, y, w, h, label);
        this.onPress = onPress;
    }

    public static FancyButton of(Component label, int x, int y, int w, int h, Runnable onPress) {
        return new FancyButton(x, y, w, h, label, onPress);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hot = this.active && isHoveredOrFocused();
        g.fillGradient(x, y, x + w, y + h, hot ? BG_TOP_HOT : BG_TOP, hot ? BG_BOTTOM_HOT : BG_BOTTOM);
        g.outline(x, y, w, h, hot ? EDGE_HOT : EDGE);
        if (hot) g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, GOLD);

        Font font = Minecraft.getInstance().font;
        String label = getMessage().getString();
        int maxW = w - 8;
        while (font.width(label) > maxW && label.length() > 1) {
            label = label.substring(0, label.length() - 1);
        }
        int tx = x + Math.max(4, (w - font.width(label)) / 2);
        int ty = y + (h - 8) / 2;
        g.text(font, label, tx, ty, !this.active ? TEXT_OFF : hot ? TEXT_HOT : TEXT);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.active) onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
