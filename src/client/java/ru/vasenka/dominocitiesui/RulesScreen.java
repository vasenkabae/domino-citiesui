package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Правила сервера — принудительный экран после входа (см. DominoCitiesUIClient), пока
 * needsRules. Как AuthScreen: shouldCloseOnEsc()==false, тиковый страж переоткрывает,
 * закрывает себя не сам, а через AuthData.refresh() после подтверждения сервером.
 *
 * Кнопка «Я ознакомился и подтверждаю» неактивна, пока игрок не долистал до последней
 * страницы — принятие должно быть осмысленным, а не мгновенным кликом на первом экране.
 */
public class RulesScreen extends Screen {

    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int CARD         = 0x2E000000;
    private static final int SCRIM        = 0xB0000000;

    private static final int PANEL_Y1 = 24;
    private static final int PAD = 16;
    private static final int CONTENT_TOP = 66;

    private record Page(String title, String[] lines) {}

    private static final Page[] PAGES = {
            new Page("Общее поведение", new String[]{
                    "Уважай других игроков: без оскорблений, травли,",
                    "разжигания конфликтов в чате",
                    "",
                    "Без спама и рекламы сторонних серверов",
            }),
            new Page("Приваты — их нет", new String[]{
                    "На сервере нет технической защиты построек",
                    "и городов — сломать или украсть можно физически",
                    "везде, даже в чужом городе",
                    "## Это не разрешение грифить",
                    "Сделано осознанно, на доверии между игроками —",
                    "а не приглашение портить чужое",
                    "Разрушение, кража, порча ландшафта без спроса —",
                    "нарушение правил, наказывается",
                    "Сомневаешься — спроси владельца или админа",
            }),
            new Page("Читы и баги", new String[]{
                    "## Читы запрещены",
                    "X-ray, автокликеры и подобное — сервер логирует",
                    "подозрительную активность",
                    "## Баги",
                    "Нашёл баг или эксплойт — сообщи администрации,",
                    "не используй в свою пользу",
            }),
            new Page("PvP и розыск", new String[]{
                    "## Свободное PvP запрещено",
                    "Не нападай на других игроков без повода",
                    "## Как решить конфликт",
                    "Закажи голову через «Розыск» (K-меню) —",
                    "единственный легальный способ",
                    "Взял чужой заказ на розыск — честная охота, разрешена",
            }),
    };

    private int page = 0;
    private FancyButton prevBtn, nextBtn, confirmBtn;

    public RulesScreen() {
        super(Component.literal("Правила Domino Craft"));
    }

    private int cx() { return this.width / 2; }
    private int panelHalf() { return Math.min(230, (this.width - 12) / 2); }
    private int px1() { return cx() - panelHalf(); }
    private int px2() { return cx() + panelHalf(); }
    private int py2() { return this.height - 12; }
    private int left() { return px1() + PAD; }
    private int right() { return px2() - PAD; }

    private boolean onLastPage() { return page == PAGES.length - 1; }

    @Override
    protected void init() {
        prevBtn = addRenderableWidget(FancyButton.of(Component.literal("◀ Назад"), left(), py2() - 52, 90, 20, () -> turn(-1)));
        nextBtn = addRenderableWidget(FancyButton.of(Component.literal("Далее ▶"), right() - 90, py2() - 52, 90, 20, () -> turn(1)));

        confirmBtn = addRenderableWidget(FancyButton.of(Component.literal("Я ознакомился и подтверждаю"),
                left(), py2() - 26, right() - left(), 20, this::confirm));
        updateNav();
    }

    private void turn(int dir) {
        page = Math.max(0, Math.min(PAGES.length - 1, page + dir));
        updateNav();
    }

    private void updateNav() {
        prevBtn.active = page > 0;
        nextBtn.active = page < PAGES.length - 1;
        confirmBtn.active = onLastPage();
    }

    private void confirm() {
        if (!onLastPage()) return; // на всякий случай — active и так это гарантирует
        AuthActions.acceptRules();
        // Экран закроет AuthData.refresh(), когда придёт подтверждение от сервера.
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, SCRIM);
        g.fillGradient(px1(), PANEL_Y1, px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), PANEL_Y1, px2() - px1(), py2() - PANEL_Y1, PANEL_EDGE);
        g.fill(px1() + 1, PANEL_Y1 + 1, px2() - 1, PANEL_Y1 + 32, 0x30000000);
        g.horizontalLine(px1() + 1, px2() - 2, PANEL_Y1 + 32, GOLD_LINE);
        g.fill(left() - 6, CONTENT_TOP - 6, right() + 6, py2() - 60, CARD);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        scaledText(g, "ПРАВИЛА DOMINO CRAFT", cx(), 9, 1.15f, GOLD, true);
        int tw = (int) (this.font.width("ПРАВИЛА DOMINO CRAFT") * 1.15f / 2) + 10;
        g.text(this.font, "◆", cx() - tw - 8, 10, GOLD_LINE);
        g.text(this.font, "◆", cx() + tw + 2, 10, GOLD_LINE);

        Page p = PAGES[page];
        scaledText(g, p.title(), cx(), PANEL_Y1 + 12, 1.1f, GOLD_BRIGHT, true);

        int y = CONTENT_TOP;
        for (String line : p.lines()) {
            if (line.isEmpty()) { y += 6; continue; }
            if (line.startsWith("## ")) {
                y += 4;
                g.text(this.font, line.substring(3), left(), y, GOLD);
            } else {
                g.text(this.font, line, left(), y, GRAY);
            }
            y += 12;
        }

        int dotsW = PAGES.length * 10 - 4;
        int dx = cx() - dotsW / 2;
        int dy = py2() - 45;
        for (int i = 0; i < PAGES.length; i++) {
            g.fill(dx + i * 10, dy, dx + i * 10 + 6, dy + 6, i == page ? GOLD : 0x33FFFFFF);
        }
        g.centeredText(this.font, (page + 1) + " / " + PAGES.length, cx(), py2() - 66, DIM);

        if (!onLastPage()) {
            g.centeredText(this.font, "Долистай до конца, чтобы подтвердить", cx(), py2() - 36, DIM);
        }
    }

    private void scaledText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color, boolean centered) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        if (centered) g.centeredText(this.font, s, 0, 0, color);
        else g.text(this.font, s, 0, 0, color);
        pose.popMatrix();
    }
}
