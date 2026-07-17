package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Отдельная полноэкранная карта мира (клавиша Y, не часть K-меню — чтобы не захламлять его).
 * Схематичный (не текстурный) плоский снимок с сервера + зум/пан колесом и перетаскиванием,
 * границы и названия городов, точки живых жителей своего города, личные метки (клик по карте).
 *
 * Зум/пан реализован через матрицу (pushMatrix/translate/scale) + enableScissor, а НЕ через
 * доп. параметры blit — так не зависим от неоднозначной семантики multi-int оверлоада blit
 * в этом форке: текстура всегда рисуется «как есть» (0,0 → полный размер), всё позиционирование
 * и масштаб — снаружи, через уже проверенный (см. scaledText в CityScreen) матричный путь.
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
    private static final int TEAMMATE_DOT = 0xFF6FB0E0;
    private static final int MARKER_COLOR = 0xFFE0B040;
    private static final int BUILDING_COLOR = 0xFFE07B4A;

    private static final int PANEL_MARGIN = 20;
    private static final int CONTENT_TOP = 44;
    private static final int BOTTOM_MARGIN = 40;
    private static final double MIN_ZOOM = 1.0, MAX_ZOOM = 8.0;
    private static final double MARKER_CLICK_RADIUS = 6.0;

    // ── Вид (зум/пан) — переживает rebuildWidgets(), сбрасывается только при новом открытии ──
    private boolean viewInitialized = false;
    private double viewCenterX, viewCenterZ;
    private double zoomFactor = 1.0;

    // ── Размещение метки: клик по карте → форма названия ──
    private boolean placingMarker = false;
    private Double pendingMarkerWorldX, pendingMarkerWorldZ;
    private EditBox markerNameInput;
    private String pendingMarkerName = "";

    public WorldMapScreen() {
        super(Component.literal("Карта мира"));
    }

    @Override
    protected void init() {
        if (pendingMarkerWorldX != null) { initMarkerForm(); return; }

        String label = CityData.mapInProgress ? "Идёт пересчёт…"
                : CityData.mapCooldownSeconds > 0 ? "Обновить карту (" + CityData.mapCooldownSeconds + "с)"
                : "Обновить карту";
        addRenderableWidget(Button.builder(Component.literal(label), b -> CityActions.refreshMap())
                .bounds(this.width / 2 - 165, this.height - 32, 150, 20).build());

        if (CityData.mapHasImage) {
            String markLabel = placingMarker ? "Кликни по карте (Esc — отмена)" : "Поставить метку";
            addRenderableWidget(Button.builder(Component.literal(markLabel), b -> {
                placingMarker = !placingMarker;
                rebuildWidgets();
            }).bounds(this.width / 2 + 15, this.height - 32, placingMarker ? 230 : 150, 20).build());
        }
    }

    private void initMarkerForm() {
        markerNameInput = new EditBox(this.font, this.width / 2 - 180, this.height - 32, 220, 20,
                Component.literal("Название метки"));
        markerNameInput.setMaxLength(32);
        markerNameInput.setHint(Component.literal("Название метки (до 32)"));
        markerNameInput.setValue(pendingMarkerName);
        addRenderableWidget(markerNameInput);
        addRenderableWidget(Button.builder(Component.literal("Поставить"), b -> {
            String name = markerNameInput.getValue().trim();
            if (!name.isEmpty()) {
                CityActions.addMarker(CityData.mapWorld,
                        (int) Math.round(pendingMarkerWorldX), (int) Math.round(pendingMarkerWorldZ), name);
                cancelPendingMarker();
            }
        }).bounds(this.width / 2 + 46, this.height - 32, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> cancelPendingMarker())
                .bounds(this.width / 2 + 132, this.height - 32, 70, 20).build());
    }

    private void cancelPendingMarker() {
        pendingMarkerWorldX = null;
        pendingMarkerWorldZ = null;
        pendingMarkerName = "";
        markerNameInput = null;
        rebuildWidgets();
    }

    /** Вызывается из CityData при получении свежих данных — не рвём фокус, если игрок печатает. */
    public void refresh() {
        if (markerNameInput != null) pendingMarkerName = markerNameInput.getValue();
        if (markerNameInput != null && markerNameInput.isFocused()) return;
        rebuildWidgets();
    }

    // ── Геометрия панели/вьюпорта ────────────────────────────────────────────

    private int px1() { return PANEL_MARGIN; }
    private int px2() { return this.width - PANEL_MARGIN; }
    private int py1() { return PANEL_MARGIN; }
    private int py2() { return this.height - PANEL_MARGIN; }

    private int boxX1() { return px1() + 12; }
    private int boxY1() { return py1() + CONTENT_TOP; }
    private int boxX2() { return px2() - 12; }
    private int boxY2() { return py2() - BOTTOM_MARGIN; }
    private double boxCenterX() { return (boxX1() + boxX2()) / 2.0; }
    private double boxCenterY() { return (boxY1() + boxY2()) / 2.0; }

    private boolean inBox(double sx, double sy) {
        return sx >= boxX1() && sx < boxX2() && sy >= boxY1() && sy < boxY2();
    }

    /** Пикселей экрана на 1 блок мира при текущем зуме (0, если карты ещё нет). */
    private double pxPerBlock() {
        if (!CityData.mapHasImage) return 0;
        double worldSpanX = Math.max(1, CityData.mapWidth * (double) CityData.mapBlockSize);
        double worldSpanZ = Math.max(1, CityData.mapHeight * (double) CityData.mapBlockSize);
        double boxW = boxX2() - boxX1(), boxH = boxY2() - boxY1();
        double fit = Math.min(boxW / worldSpanX, boxH / worldSpanZ);
        return fit * zoomFactor;
    }

    private double worldToScreenX(double wx) { return boxCenterX() + (wx - viewCenterX) * pxPerBlock(); }
    private double worldToScreenZ(double wz) { return boxCenterY() + (wz - viewCenterZ) * pxPerBlock(); }
    private double screenToWorldX(double sx) { return viewCenterX + (sx - boxCenterX()) / pxPerBlock(); }
    private double screenToWorldZ(double sy) { return viewCenterZ + (sy - boxCenterY()) / pxPerBlock(); }

    private void ensureViewInitialized() {
        if (viewInitialized || !CityData.mapHasImage) return;
        viewInitialized = true;
        var player = Minecraft.getInstance().player;
        if (player != null && worldMatches(player.level().dimension(), CityData.mapWorld)) {
            viewCenterX = player.getX();
            viewCenterZ = player.getZ();
        } else {
            viewCenterX = CityData.mapMinX + CityData.mapWidth * CityData.mapBlockSize / 2.0;
            viewCenterZ = CityData.mapMinZ + CityData.mapHeight * CityData.mapBlockSize / 2.0;
        }
        clampView();
    }

    private void clampView() {
        zoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomFactor));
        if (!CityData.mapHasImage) return;
        double spanX = CityData.mapWidth * (double) CityData.mapBlockSize;
        double spanZ = CityData.mapHeight * (double) CityData.mapBlockSize;
        viewCenterX = Math.max(CityData.mapMinX, Math.min(CityData.mapMinX + spanX, viewCenterX));
        viewCenterZ = Math.max(CityData.mapMinZ, Math.min(CityData.mapMinZ + spanZ, viewCenterZ));
    }

    /** Ближайшая своя метка под курсором (в пределах MARKER_CLICK_RADIUS px) или null. */
    private CityData.MarkerInfo findMarkerNear(double sx, double sy) {
        CityData.MarkerInfo best = null;
        double bestDist = MARKER_CLICK_RADIUS;
        for (CityData.MarkerInfo m : CityData.mapMarkers) {
            if (!m.world().equals(CityData.mapWorld)) continue;
            double dx = worldToScreenX(m.x()) - sx, dz = worldToScreenZ(m.z()) - sy;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < bestDist) { bestDist = dist; best = m; }
        }
        return best;
    }

    // ── Ввод: зум колесом, пан перетаскиванием, клик — метка/удаление ────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!CityData.mapHasImage || !inBox(mouseX, mouseY) || scrollY == 0) return false;
        zoomFactor *= Math.pow(1.2, scrollY);
        clampView();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) return true;
        if (!CityData.mapHasImage || event.button() != 0 || !inBox(event.x(), event.y())) return false;
        double ppb = pxPerBlock();
        if (ppb <= 0) return false;
        viewCenterX -= dragX / ppb;
        viewCenterZ -= dragY / ppb;
        clampView();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!CityData.mapHasImage || !inBox(event.x(), event.y())) return false;
        if (event.button() == 1) {
            CityData.MarkerInfo hit = findMarkerNear(event.x(), event.y());
            if (hit != null) { CityActions.deleteMarker(hit.id()); return true; }
            return false;
        }
        if (event.button() == 0 && placingMarker) {
            pendingMarkerWorldX = screenToWorldX(event.x());
            pendingMarkerWorldZ = screenToWorldZ(event.y());
            placingMarker = false;
            rebuildWidgets();
            return true;
        }
        return false;
    }

    // ── Фон: панель + карта + границы городов + тиммейты + метки ────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        ensureViewInitialized();

        g.fillGradient(px1(), py1(), px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), py1(), px2() - px1(), py2() - py1(), PANEL_EDGE);
        g.horizontalLine(px1() + 1, px2() - 2, boxY1() - 10, GOLD_LINE);
        g.fill(boxX1() - 1, boxY1() - 1, boxX2() + 1, boxY2() + 1, CARD);
        if (!CityData.mapHasImage) return;

        g.enableScissor(boxX1(), boxY1(), boxX2(), boxY2());

        double ppb = pxPerBlock();
        Identifier tex = CityMapTexture.get(CityData.mapVersion);
        if (tex != null && ppb > 0) {
            double scale = CityData.mapBlockSize * ppb;
            double originX = worldToScreenX(CityData.mapMinX);
            double originY = worldToScreenZ(CityData.mapMinZ);
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) originX, (float) originY);
            pose.scale((float) scale, (float) scale);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f,
                    CityMapTexture.width(), CityMapTexture.height(),
                    CityMapTexture.width(), CityMapTexture.height());
            pose.popMatrix();
        }

        for (CityData.MapCityInfo c : CityData.mapCities) {
            if (!c.world().equals(CityData.mapWorld)) continue;
            int x1 = (int) worldToScreenX(c.x() - c.radius()), z1 = (int) worldToScreenZ(c.z() - c.radius());
            int x2 = (int) worldToScreenX(c.x() + c.radius()), z2 = (int) worldToScreenZ(c.z() + c.radius());
            g.outline(x1, z1, Math.max(1, x2 - x1), Math.max(1, z2 - z1), c.mine() ? MAP_MINE_LINE : MAP_OTHER_LINE);
        }

        for (CityData.TeammateInfo t : CityData.mapTeammates) {
            if (!t.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(t.x()), sy = (int) worldToScreenZ(t.z());
            g.fill(sx - 2, sy - 2, sx + 2, sy + 2, TEAMMATE_DOT);
        }

        for (CityData.MarkerInfo m : CityData.mapMarkers) {
            if (!m.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(m.x()), sy = (int) worldToScreenZ(m.z());
            g.fill(sx - 1, sy - 5, sx + 1, sy + 1, MARKER_COLOR);
            g.fill(sx - 3, sy - 5, sx + 3, sy - 3, MARKER_COLOR);
        }

        // Постройки городов — «домик»: корпус + крыша.
        for (CityData.MapBuildingInfo b : CityData.mapBuildings) {
            if (!b.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(b.x()), sy = (int) worldToScreenZ(b.z());
            g.fill(sx - 3, sy - 1, sx + 4, sy + 4, BUILDING_COLOR);
            g.fill(sx - 2, sy - 3, sx + 3, sy - 1, BUILDING_COLOR);
            g.fill(sx - 1, sy - 4, sx + 2, sy - 3, BUILDING_COLOR);
        }

        var player = Minecraft.getInstance().player;
        if (player != null && worldMatches(player.level().dimension(), CityData.mapWorld)) {
            int sx = (int) worldToScreenX(player.getX()), sz = (int) worldToScreenZ(player.getZ());
            g.fill(sx - 2, sz - 2, sx + 2, sz + 2, 0xFFFFFFFF);
        }

        g.disableScissor();
    }

    // ── Передний план: заголовок, подсказки, подписи городов/меток ──────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, "КАРТА МИРА", this.width / 2, py1() + 10, GOLD);

        if (!CityData.mapHasImage) {
            int cy = (boxY1() + boxY2()) / 2;
            g.centeredText(this.font, "Карта ещё не строилась", this.width / 2, cy - 4, GRAY);
            g.centeredText(this.font, "нажми «Обновить карту»", this.width / 2, cy + 8, DIM);
            return;
        }
        if (CityMapTexture.get(CityData.mapVersion) == null) {
            g.centeredText(this.font, "Загрузка изображения…", this.width / 2, (boxY1() + boxY2()) / 2, GRAY);
        }
        g.text(this.font, "серое — там ещё никто не бывал  ·  колесо — зум, ЛКМ+тяни — двигать карту",
                px1() + 12, boxY1() - 22, DIM);
        g.text(this.font, "золотая рамка — твой город, синяя — чужие  ·  домик — постройка (наведи курсор)  ·  ПКМ по метке — удалить",
                px1() + 12, boxY1() - 10, DIM);

        for (CityData.MapCityInfo c : CityData.mapCities) {
            if (!c.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(c.x());
            int topY = (int) worldToScreenZ(c.z() - c.radius()) - 10;
            if (!inBox(sx, topY + 10) && !inBox(sx, (int) worldToScreenZ(c.z()))) continue;
            g.centeredText(this.font, c.name(), sx, Math.max(boxY1() + 2, topY), c.mine() ? GOLD_BRIGHT : WHITE);
        }

        for (CityData.MarkerInfo m : CityData.mapMarkers) {
            if (!m.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(m.x()), sy = (int) worldToScreenZ(m.z());
            if (!inBox(sx, sy)) continue;
            if (Math.abs(mouseX - sx) < 6 && Math.abs(mouseY - sy) < 8) {
                g.text(this.font, m.name(), sx + 6, sy - 8, GOLD_BRIGHT);
            }
        }

        // Постройка под курсором — всплывающая карточка: название, город, фото.
        CityData.MapBuildingInfo hoverBuilding = null;
        for (CityData.MapBuildingInfo b : CityData.mapBuildings) {
            if (!b.world().equals(CityData.mapWorld)) continue;
            int sx = (int) worldToScreenX(b.x()), sy = (int) worldToScreenZ(b.z());
            if (!inBox(sx, sy)) continue;
            if (Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) hoverBuilding = b;
        }
        if (hoverBuilding != null) drawBuildingTooltip(g, hoverBuilding, mouseX, mouseY);
    }

    private void drawBuildingTooltip(GuiGraphicsExtractor g, CityData.MapBuildingInfo b, int mouseX, int mouseY) {
        int photoW = 120, photoH = 68; // 16:9, как сами фото построек
        boolean hasPhoto = !b.photoId().isEmpty();
        int textW = Math.max(this.font.width(b.name()), this.font.width("город: " + b.cityName()));
        int w = Math.max(hasPhoto ? photoW : 0, textW) + 12;
        int h = hasPhoto ? 30 + photoH + 6 : 30;
        int x = Math.min(mouseX + 10, px2() - w - 4);
        int y = Math.min(mouseY + 10, py2() - h - 4);
        if (y < py1() + 4) y = py1() + 4;
        g.fill(x, y, x + w, y + h, 0xF0101116);
        g.outline(x, y, w, h, GOLD_LINE);
        g.text(this.font, b.name(), x + 6, y + 6, GOLD_BRIGHT);
        g.text(this.font, "город: " + b.cityName(), x + 6, y + 17, GRAY);
        if (hasPhoto) {
            int py0 = y + 30;
            g.fill(x + 6, py0, x + 6 + photoW, py0 + photoH, 0x30000000);
            Identifier tex = BuildingPhotos.get(b.photoId());
            if (tex != null) {
                int[] sz = BuildingPhotos.size(b.photoId());
                g.blit(RenderPipelines.GUI_TEXTURED, tex, x + 6, py0, 0f, 0f,
                        photoW, photoH, sz[0], sz[1], sz[0], sz[1]);
            } else {
                g.centeredText(this.font, "фото загружается…", x + w / 2, py0 + photoH / 2 - 4, DIM);
            }
        }
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
