package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Отдельная полноэкранная карта мира (клавиша Y, не часть K-меню — чтобы не захламлять его).
 * Схематичный (не текстурный) плоский снимок с сервера + границы городов и точка игрока
 * поверх векторно. Данные и текстура — {@link CityData}/{@link CityMapTexture}, тот же протокол,
 * что раньше использовался под вкладкой «Карта» внутри CityScreen.
 *
 * Визуальный стиль повторяет CityScreen/GuideScreen: тёмная стеклянная панель, золотые акценты.
 */
public class WorldMapScreen extends Screen {

    // ── Палитра (как в CityScreen) ───────────────────────────────────────────
    private static final int WHITE  = 0xFFF2F3F5;
    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int CARD         = 0x2E000000;
    private static final int MAP_MINE_LINE  = 0xC0F2B94E;
    private static final int MAP_OTHER_LINE = 0x906FA0D0;

    private static final int PANEL_MARGIN = 20;
    private static final int CONTENT_TOP = 44;
    private static final int BOTTOM_MARGIN = 40;

    public WorldMapScreen() {
        super(Component.literal("Карта мира"));
    }

    @Override
    protected void init() {
        CityActions.requestCityMap();

        String label = CityData.mapInProgress ? "Идёт пересчёт…"
                : CityData.mapCooldownSeconds > 0 ? "Обновить карту (" + CityData.mapCooldownSeconds + "с)"
                : "Обновить карту";
        addRenderableWidget(Button.builder(Component.literal(label), b -> CityActions.refreshMap())
                .bounds(this.width / 2 - 80, this.height - 32, 160, 20).build());
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        rebuildWidgets();
    }

    private int px1() { return PANEL_MARGIN; }
    private int px2() { return this.width - PANEL_MARGIN; }
    private int py1() { return PANEL_MARGIN; }
    private int py2() { return this.height - PANEL_MARGIN; }

    /** Прямоугольник отображения картинки карты внутри панели (letterbox по аспекту). */
    private record MapRect(double x, double y, double w, double h) {}

    private MapRect displayRect() {
        double boxX1 = px1() + 12, boxY1 = py1() + CONTENT_TOP;
        double boxX2 = px2() - 12, boxY2 = py2() - BOTTOM_MARGIN;
        double boxW = boxX2 - boxX1, boxH = boxY2 - boxY1;
        double imgW = Math.max(1, CityData.mapWidth), imgH = Math.max(1, CityData.mapHeight);
        double scale = Math.min(boxW / imgW, boxH / imgH);
        double dispW = imgW * scale, dispH = imgH * scale;
        return new MapRect(boxX1 + (boxW - dispW) / 2, boxY1 + (boxH - dispH) / 2, dispW, dispH);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        g.fillGradient(px1(), py1(), px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), py1(), px2() - px1(), py2() - py1(), PANEL_EDGE);
        g.horizontalLine(px1() + 1, px2() - 2, py1() + CONTENT_TOP - 10, GOLD_LINE);

        MapRect r = displayRect();
        g.fill((int) r.x() - 1, (int) r.y() - 1, (int) (r.x() + r.w()) + 1, (int) (r.y() + r.h()) + 1, CARD);
        if (!CityData.mapHasImage) return;

        Identifier tex = CityMapTexture.get(CityData.mapVersion);
        if (tex != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, (int) r.x(), (int) r.y(), 0f, 0f,
                    (int) r.w(), (int) r.h(), CityMapTexture.width(), CityMapTexture.height(),
                    CityMapTexture.width(), CityMapTexture.height());
        }

        double scale = r.w() / Math.max(1, CityData.mapWidth);
        for (CityData.MapCityInfo c : CityData.mapCities) {
            if (!c.world().equals(CityData.mapWorld)) continue;
            double imgX = (c.x() - CityData.mapMinX) / (double) CityData.mapBlockSize;
            double imgZ = (c.z() - CityData.mapMinZ) / (double) CityData.mapBlockSize;
            double imgR = c.radius() / (double) CityData.mapBlockSize;
            int x1 = (int) (r.x() + (imgX - imgR) * scale), z1 = (int) (r.y() + (imgZ - imgR) * scale);
            int x2 = (int) (r.x() + (imgX + imgR) * scale), z2 = (int) (r.y() + (imgZ + imgR) * scale);
            g.outline(x1, z1, Math.max(1, x2 - x1), Math.max(1, z2 - z1), c.mine() ? MAP_MINE_LINE : MAP_OTHER_LINE);
        }

        var player = Minecraft.getInstance().player;
        if (player != null && worldMatches(player.level().dimension(), CityData.mapWorld)) {
            double imgX = (player.getX() - CityData.mapMinX) / CityData.mapBlockSize;
            double imgZ = (player.getZ() - CityData.mapMinZ) / CityData.mapBlockSize;
            int px = (int) (r.x() + imgX * scale), pz = (int) (r.y() + imgZ * scale);
            g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFFFFFFFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, "КАРТА МИРА", this.width / 2, py1() + 10, GOLD);

        MapRect r = displayRect();
        if (!CityData.mapHasImage) {
            g.centeredText(this.font, "Карта ещё не строилась", this.width / 2, (int) (r.y() + r.h() / 2) - 4, GRAY);
            g.centeredText(this.font, "нажми «Обновить карту»", this.width / 2, (int) (r.y() + r.h() / 2) + 8, DIM);
        } else if (CityMapTexture.get(CityData.mapVersion) == null) {
            g.centeredText(this.font, "Загрузка изображения…", this.width / 2, (int) (r.y() + r.h() / 2), GRAY);
        }
        g.text(this.font, "серое — там ещё никто не бывал (карта не грузит мир сама)",
                px1() + 12, py1() + CONTENT_TOP - 22, DIM);
        g.text(this.font, "золотая рамка — твой город, синяя — чужие", px1() + 12, py1() + CONTENT_TOP - 10, DIM);
    }

    /** Bukkit хранит имя папки мира ("world"/"world_nether"/"world_the_end"), клиент — ResourceKey измерения. */
    private static boolean worldMatches(ResourceKey<Level> dimension, String bukkitWorld) {
        return switch (bukkitWorld) {
            case "world" -> dimension.equals(Level.OVERWORLD);
            case "world_nether" -> dimension.equals(Level.NETHER);
            case "world_the_end" -> dimension.equals(Level.END);
            default -> false;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
