package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Быстрый выбор активки на клавишу G (открывается зажатием G). Клик по строке —
 * выбрать/экипировать активку; дальше короткий тап G применяет выбранное
 * (у капстоунов — активация с кулдауном, у «Лёгкой руки» — вкл/выкл).
 */
public class AbilityMenuScreen extends Screen {

    private static final int GOLD = 0xFFF2B94E;
    private static final int DIM  = 0xFF767D8A;
    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;

    private static final int ROW_H = 24, GAP = 4, BTN_W = 260;

    public AbilityMenuScreen() { super(Component.literal("Выбор активки")); }

    private int cx() { return this.width / 2; }

    private int rowCount() {
        int c = 0;
        for (int i = 0; i < SkillsData.PROF_COUNT; i++) {
            SkillsCatalog.Node n = SkillsCatalog.capstone(i);
            if (n != null && SkillsData.rank(n.id()) > 0) c++;
        }
        if (SkillsData.hasLightHand()) c++;
        return c;
    }

    private int rowsHeight() { return rowCount() * (ROW_H + GAP); }
    private int panelH() { return 26 + Math.max(ROW_H, rowsHeight()) + 18; }
    private int panelY1() { return this.height / 2 - panelH() / 2; }
    private int firstRowY() { return panelY1() + 26; }

    @Override
    protected void init() {
        int y = firstRowY();
        int x = cx() - BTN_W / 2;
        for (int i = 0; i < SkillsData.PROF_COUNT; i++) {
            SkillsCatalog.Node n = SkillsCatalog.capstone(i);
            if (n == null || SkillsData.rank(n.id()) == 0) continue;
            boolean eq = !SkillsData.equippedLightHand && SkillsData.lastAbilityProf == i;
            final int prof = i;
            addRenderableWidget(Button.builder(
                    Component.literal((eq ? "✔ " : "") + n.name()),
                    b -> { SkillsData.equippedLightHand = false; SkillsData.lastAbilityProf = prof; onClose(); })
                    .bounds(x, y, BTN_W, ROW_H).build());
            y += ROW_H + GAP;
        }
        if (SkillsData.hasLightHand()) {
            boolean eq = SkillsData.equippedLightHand;
            String state = SkillsData.lightHandOn ? "ВКЛ" : "ВЫКЛ";
            addRenderableWidget(Button.builder(
                    Component.literal((eq ? "✔ " : "") + "Лёгкая рука [" + state + "]"),
                    b -> { SkillsData.equippedLightHand = true; onClose(); })
                    .bounds(x, y, BTN_W, ROW_H).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int y1 = panelY1(), y2 = y1 + panelH();
        int x1 = cx() - BTN_W / 2 - 12, x2 = cx() + BTN_W / 2 + 12;
        g.fillGradient(x1, y1, x2, y2, PANEL_TOP, PANEL_BOTTOM);
        g.outline(x1, y1, x2 - x1, y2 - y1, PANEL_EDGE);
        g.horizontalLine(x1 + 1, x2 - 2, y1 + 22, GOLD_LINE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int y1 = panelY1();
        g.centeredText(this.font, "Выбор активки (G)", cx(), y1 + 7, GOLD);
        if (rowCount() == 0) {
            g.centeredText(this.font, "Нет изученных активок — изучи в древе (N)", cx(), this.height / 2, DIM);
        } else {
            g.centeredText(this.font, "клик — выбрать · тап G — применить · Esc — закрыть",
                    cx(), y1 + panelH() - 11, DIM);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
