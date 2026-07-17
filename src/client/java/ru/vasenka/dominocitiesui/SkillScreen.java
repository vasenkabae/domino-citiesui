package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран профессий и древа талантов (клавиша P). Стиль — как CityScreen:
 * тёмная стеклянная панель, золото; фон в extractBackground, тексты поверх.
 * Все данные — статик SkillsData, обновляются пакетами сервера.
 */
public class SkillScreen extends Screen {

    // ── Палитра (как в CityScreen) ───────────────────────────────────────────
    private static final int WHITE  = 0xFFF2F3F5;
    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int GREEN  = 0xFF66D98F;
    private static final int RED    = 0xFFEB7069;

    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int CARD         = 0x2E000000;
    private static final int TAB_ACTIVE_BG = 0x26F2B94E;
    private static final int NODE_BG        = 0x66000000;
    private static final int NODE_EDGE_OFF  = 0x22FFFFFF;
    private static final int NODE_EDGE_ON   = 0x66FFFFFF;
    private static final int NODE_EDGE_GOLD = 0xCCF2B94E;
    private static final int BAR_BG   = 0x40000000;

    // ── Геометрия ────────────────────────────────────────────────────────────
    private static final int PANEL_Y1 = 24;
    private static final int PAD = 16;
    private static final int TAB_Y = 30, TAB_H = 22;
    private static final int CONTENT_TOP = 60;
    private static final int HEADER_H = 38;
    private static final int TREE_TOP = CONTENT_TOP + HEADER_H;
    private static final int ROW_H = 56;      // подпись ряда + карточка + зазор — верхний предел
    private static final int NODE_W = 140;

    private int tab = 0; // profId выбранной вкладки
    private Button resetBtn;
    private long confirmUntil;

    public SkillScreen() {
        super(Component.literal("Профессии Domino Craft"));
    }

    private int cx() { return this.width / 2; }
    private int panelHalf() { return Math.min(250, (this.width - 12) / 2); }
    private int px1() { return cx() - panelHalf(); }
    private int px2() { return cx() + panelHalf(); }
    private int py2() { return this.height - 12; }
    private int left() { return px1() + PAD; }
    private int right() { return px2() - PAD; }

    // ── Вкладки ──────────────────────────────────────────────────────────────

    private record TabRect(int index, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private List<TabRect> tabRects() {
        List<TabRect> out = new ArrayList<>();
        int count = SkillsData.PROF_COUNT;
        int startX = px1() + 4;
        int tabW = (px2() - px1() - 8) / count;
        for (int i = 0; i < count; i++)
            out.add(new TabRect(i, startX + i * tabW, TAB_Y, tabW - 2, TAB_H));
        return out;
    }

    // ── Узлы древа ───────────────────────────────────────────────────────────

    private int nodeX(int col) {
        return switch (col) {
            case 0 -> left();
            case 1 -> cx() - NODE_W / 2;
            default -> right() - NODE_W;
        };
    }

    /** Высота ряда — сжимается, если 7 рядов не влезают в экран (мелкие логические разрешения). */
    private int rowH() {
        int avail = py2() - 34 - TREE_TOP; // до кнопки сброса
        return Math.min(ROW_H, Math.max(28, avail / 7));
    }

    /** Совсем тесно — прячем подписи рядов и пипсы, оставляя место узлам. */
    private boolean compact() { return rowH() < 44; }

    private int nodeH() { return rowH() - (compact() ? 2 : 14); }

    private int rowTop(int tier) { return TREE_TOP + (tier - 1) * rowH(); }

    private int nodeY(int tier) { return rowTop(tier) + (compact() ? 1 : 12); }

    @Override
    protected void init() {
        resetBtn = addRenderableWidget(Button.builder(Component.literal(resetLabel()),
                b -> onResetClick()).bounds(left(), py2() - 26, 180, 20).build());
    }

    private String resetLabel() {
        if (System.currentTimeMillis() < confirmUntil) return "Точно сбросить? (клик)";
        return "Сбросить древо (" + SkillsData.resetCost + " алм.)";
    }

    private void onResetClick() {
        long now = System.currentTimeMillis();
        if (now < confirmUntil) {
            confirmUntil = 0;
            SkillsActions.reset();
        } else {
            confirmUntil = now + 4000;
        }
    }

    /** Дёргается из SkillsData при получении result — сбрасываем подтверждение. */
    public void onResult() {
        confirmUntil = 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (TabRect t : tabRects()) {
            if (t.contains(event.x(), event.y())) {
                tab = t.index();
                return true;
            }
        }
        if (!SkillsData.protocolMismatch && SkillsData.stateLoaded) {
            for (SkillsCatalog.Node n : SkillsCatalog.forProfession(tab)) {
                int x = nodeX(n.col()), y = nodeY(n.tier());
                if (event.x() >= x && event.x() < x + NODE_W && event.y() >= y && event.y() < y + nodeH()) {
                    onNodeClick(n);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onNodeClick(SkillsCatalog.Node n) {
        int rank = SkillsData.rank(n.id());
        // Изученная активка (ряд 4 или 7): клик = активировать (сервер сам проверит перезарядку).
        if ((n.tier() == 4 || n.tier() == 7) && rank > 0) {
            SkillsData.lastAbilityProf = tab;
            SkillsData.lastAbilityTier = n.tier();
            SkillsActions.activate(tab, n.tier());
            return;
        }
        if (rank >= n.maxRank()) return;
        if (SkillsData.prof[tab].level < SkillsCatalog.tierGate(n.tier())) return;
        if (SkillsData.pointsAvailable() < n.cost()) return;
        SkillsActions.learn(n.id());
    }

    // ── Фон ──────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fillGradient(px1(), PANEL_Y1, px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), PANEL_Y1, px2() - px1(), py2() - PANEL_Y1, PANEL_EDGE);
        // Полоса вкладок
        g.fill(px1() + 1, PANEL_Y1 + 1, px2() - 1, TAB_Y + TAB_H + 3, 0x30000000);
        g.horizontalLine(px1() + 1, px2() - 2, TAB_Y + TAB_H + 3, GOLD_LINE);
        for (TabRect t : tabRects()) {
            if (t.index() == tab) g.fill(t.x(), t.y(), t.x() + t.w(), t.y() + t.h(), TAB_ACTIVE_BG);
        }
        // Карточка шапки профессии
        g.fill(left() - 6, CONTENT_TOP - 4, right() + 6, CONTENT_TOP + HEADER_H - 10, CARD);
    }

    // ── Контент ──────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        resetBtn.setMessage(Component.literal(resetLabel()));
        resetBtn.active = SkillsData.pointsSpent > 0 || !SkillsData.talents.isEmpty();

        scaledText(g, "ПРОФЕССИИ", cx(), 9, 1.15f, GOLD, true);
        int tw = (int) (this.font.width("ПРОФЕССИИ") * 1.15f / 2) + 10;
        g.text(this.font, "◆", cx() - tw - 8, 10, GOLD_LINE);
        g.text(this.font, "◆", cx() + tw + 2, 10, GOLD_LINE);

        // Вкладки
        for (TabRect t : tabRects()) {
            boolean active = t.index() == tab;
            boolean hover = t.contains(mouseX, mouseY);
            int color = active ? GOLD_BRIGHT : hover ? WHITE : GRAY;
            g.centeredText(this.font, SkillsData.PROF_TITLES[t.index()], t.x() + t.w() / 2, t.y() + 7, color);
            if (active) g.fill(t.x() + 6, t.y() + t.h() - 2, t.x() + t.w() - 6, t.y() + t.h(), GOLD);
        }

        if (SkillsData.protocolMismatch) {
            g.centeredText(this.font, "Версия мода не совпадает с сервером.", cx(), CONTENT_TOP + 30, RED);
            g.centeredText(this.font, "Перезапусти игру через лаунчер — он подтянет обновление.", cx(), CONTENT_TOP + 44, GRAY);
            return;
        }
        if (!SkillsData.stateLoaded) {
            g.centeredText(this.font, "Загрузка…", cx(), CONTENT_TOP + 30, DIM);
            return;
        }

        drawHeader(g);
        SkillsCatalog.Node hovered = drawTree(g, mouseX, mouseY);

        // Тост результата — справа от кнопки сброса (низ панели занят рядом 4)
        if (!SkillsData.lastResult.isEmpty()
                && System.currentTimeMillis() - SkillsData.lastResultTime < 5000) {
            g.text(this.font, SkillsData.lastResult, left() + 190, py2() - 20,
                    SkillsData.lastOk ? GREEN : RED);
        }

        if (hovered != null) drawTooltip(g, hovered, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        SkillsData.ProfState ps = SkillsData.prof[tab];
        scaledText(g, SkillsData.PROF_TITLES[tab], left(), CONTENT_TOP + 2, 1.1f, GOLD_BRIGHT, false);
        String lvl = ps.xpNeed == 0 ? "Уровень " + ps.level + " — МАКС" : "Уровень " + ps.level;
        g.text(this.font, lvl, left(), CONTENT_TOP + 16, WHITE);

        // Полоса опыта
        int barX = left() + 90, barX2 = right() - 150;
        if (barX2 > barX + 30) {
            int by = CONTENT_TOP + 18;
            g.fill(barX, by, barX2, by + 6, BAR_BG);
            if (ps.xpNeed > 0) {
                int w = (int) ((long) (barX2 - barX) * Math.min(ps.xpInto, ps.xpNeed) / ps.xpNeed);
                g.fill(barX, by, barX + w, by + 6, GOLD);
                g.text(this.font, ps.xpInto + " / " + ps.xpNeed, barX2 + 6, by - 1, DIM);
            } else {
                g.fill(barX, by, barX2, by + 6, GOLD);
            }
        }

        String pts = "Очки: " + SkillsData.pointsAvailable() + " свободно";
        g.text(this.font, pts, right() - this.font.width(pts), CONTENT_TOP + 2, GREEN);
        String spent = "потрачено " + SkillsData.pointsSpent + " · заработано " + SkillsData.pointsEarned;
        g.text(this.font, spent, right() - this.font.width(spent), CONTENT_TOP + 14, DIM);
    }

    /** Рисует три ряда узлов, возвращает узел под курсором (для тултипа). */
    private SkillsCatalog.Node drawTree(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SkillsCatalog.Node hovered = null;
        int profLevel = SkillsData.prof[tab].level;

        if (!compact()) {
            for (int tier = 1; tier <= 7; tier++) {
                int gate = SkillsCatalog.tierGate(tier);
                String label = tier == 1 ? "Ряд 1"
                        : tier == 4 ? "Ряд 4 — с " + gate + " уровня — активная способность"
                        : tier == 7 ? "Ряд 7 — с " + gate + " уровня — вторая активка"
                        : "Ряд " + tier + " — с " + gate + " уровня";
                g.text(this.font, label, left(), rowTop(tier) + 1, profLevel >= gate ? GRAY : DIM);
                g.horizontalLine(left() + this.font.width(label) + 6, right(), rowTop(tier) + 5, 0x14FFFFFF);
            }
        }

        for (SkillsCatalog.Node n : SkillsCatalog.forProfession(tab)) {
            int x = nodeX(n.col()), y = nodeY(n.tier());
            int rank = SkillsData.rank(n.id());
            boolean maxed = rank >= n.maxRank();
            boolean unlocked = profLevel >= SkillsCatalog.tierGate(n.tier());
            boolean afford = SkillsData.pointsAvailable() >= n.cost();
            boolean hover = mouseX >= x && mouseX < x + NODE_W && mouseY >= y && mouseY < y + nodeH();
            if (hover) hovered = n;

            g.fill(x, y, x + NODE_W, y + nodeH(), NODE_BG);
            if (hover && unlocked && !maxed) g.fill(x, y, x + NODE_W, y + nodeH(), 0x14FFFFFF);
            int edge = maxed || rank > 0 ? NODE_EDGE_GOLD : unlocked ? NODE_EDGE_ON : NODE_EDGE_OFF;
            g.outline(x, y, NODE_W, nodeH(), edge);

            int nameColor = maxed ? GOLD_BRIGHT : rank > 0 ? GOLD : unlocked ? WHITE : DIM;
            g.text(this.font, fit(n.name(), NODE_W - 12), x + 6, y + 5, nameColor);

            // Пипсы рангов для многоранговых (в тесном режиме — только тултип)
            if (n.maxRank() > 1 && !compact()) {
                int pipX = x + 6;
                int pipY = y + 18;
                for (int i = 0; i < n.maxRank(); i++) {
                    g.fill(pipX + i * 10, pipY, pipX + i * 10 + 7, pipY + 4, i < rank ? GOLD : 0x33FFFFFF);
                }
            }

            String status;
            int statusColor;
            if ((n.tier() == 4 || n.tier() == 7) && rank > 0) {
                // Изученная активка: живой статус (у ряда 7 — своя пара таймеров).
                long now = System.currentTimeMillis();
                SkillsData.ProfState st = SkillsData.prof[tab];
                long activeUntil = n.tier() == 7 ? st.ability7ActiveUntil : st.abilityActiveUntil;
                long cdUntil = n.tier() == 7 ? st.ability7CooldownUntil : st.abilityCooldownUntil;
                if (activeUntil > now) {
                    status = "АКТИВНА " + mmss(activeUntil - now);
                    statusColor = GOLD_BRIGHT;
                } else if (cdUntil > now) {
                    status = "Перезарядка " + mmss(cdUntil - now);
                    statusColor = DIM;
                } else {
                    status = "Готова — клик или G";
                    statusColor = GREEN;
                }
            } else if (maxed) { status = "Изучено"; statusColor = GOLD; }
            else if (!unlocked) { status = "Закрыто до " + SkillsCatalog.tierGate(n.tier()) + " ур."; statusColor = DIM; }
            else {
                status = (rank > 0 ? "Улучшить: " : "Изучить: ") + n.cost() + " оч.";
                statusColor = afford ? GREEN : RED;
            }
            g.text(this.font, status, x + 6, y + nodeH() - 12, statusColor);
        }
        return hovered;
    }

    private void drawTooltip(GuiGraphicsExtractor g, SkillsCatalog.Node n, int mouseX, int mouseY) {
        int w = 190;
        List<String> lines = new ArrayList<>();
        int rank = SkillsData.rank(n.id());
        lines.add("§6" + n.name() + (n.maxRank() > 1 ? " §7(" + rank + "/" + n.maxRank() + ")" : ""));
        lines.addAll(wrap(n.desc(), w - 12));
        lines.add("§7Стоимость: " + n.cost() + (n.maxRank() > 1 ? " оч. за ранг" : " оч."));
        int gate = SkillsCatalog.tierGate(n.tier());
        if (SkillsData.prof[tab].level < gate)
            lines.add("§cОткроется с " + gate + " уровня профессии");

        int h = lines.size() * 11 + 10;
        int x = Math.min(mouseX + 10, this.width - w - 4);
        int y = Math.min(mouseY + 10, this.height - h - 4);
        g.fill(x, y, x + w, y + h, 0xF0101216);
        g.outline(x, y, w, h, GOLD_LINE);
        int ty = y + 5;
        for (String line : lines) {
            g.text(this.font, line, x + 6, ty, WHITE);
            ty += 11;
        }
    }

    static String mmss(long ms) {
        long total = Math.max(0, ms / 1000);
        return total >= 60 ? (total / 60) + ":" + String.format("%02d", total % 60) : total + " с";
    }

    // ── Утилиты текста ───────────────────────────────────────────────────────

    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        String out = s;
        while (out.length() > 1 && this.font.width(out + "…") > maxW)
            out = out.substring(0, out.length() - 1);
        return out + "…";
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(probe) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(probe);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
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
