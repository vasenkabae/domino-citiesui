package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Окно управления городом. Две секции: «Мой город» и «Топы».
 * Данные читает из {@link CityData}; при их обновлении сервером экран перестраивается.
 */
public class CityScreen extends Screen {

    private boolean showTop = false;
    private EditBox input;         // название (без города) или ник для приглашения (в городе)
    private String pendingText = "";

    public CityScreen() {
        super(Component.literal("Города Domino Craft"));
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        if (input != null) pendingText = input.getValue();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = 40;

        // Переключатель секций
        addRenderableWidget(Button.builder(
                Component.literal(showTop ? "← Мой город" : "Топы городов →"),
                b -> { showTop = !showTop; if (showTop) CityActions.requestTop(); rebuildWidgets(); })
                .bounds(cx + 40, 12, 120, 20).build());

        if (CityData.protocolMismatch) {
            return; // текст-предупреждение рисуется в render()
        }

        if (showTop) {
            initTop(cx, top);
        } else if (CityData.hasCity) {
            initCity(cx, top);
        } else {
            initNoCity(cx, top);
        }
    }

    private void initNoCity(int cx, int top) {
        input = new EditBox(this.font, cx - 100, top + 30, 200, 20, Component.literal("Название"));
        input.setMaxLength(20);
        input.setHint(Component.literal("Название города (3–20)"));
        input.setValue(pendingText);
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.literal("Основать"),
                b -> { CityActions.create(input.getValue()); })
                .bounds(cx - 100, top + 56, 200, 20).build());
    }

    private void initCity(int cx, int top) {
        int y = top + 44;

        // Список жителей с кнопкой «Выгнать» для мэра
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            if (CityData.isMayor && !m.mayor()) {
                addRenderableWidget(Button.builder(Component.literal("Выгнать"),
                        b -> { CityActions.kick(m.uuid()); })
                        .bounds(cx + 60, y - 2, 70, 16).build());
            }
            y += 18;
        }

        int by = top + 44 + shown * 18 + 8;

        // Поле приглашения (для мэра)
        if (CityData.isMayor) {
            input = new EditBox(this.font, cx - 150, by, 150, 20, Component.literal("Ник"));
            input.setMaxLength(16);
            input.setHint(Component.literal("Ник игрока"));
            input.setValue(pendingText);
            addRenderableWidget(input);
            addRenderableWidget(Button.builder(Component.literal("Пригласить"),
                    b -> { CityActions.invite(input.getValue()); })
                    .bounds(cx + 4, by, 100, 20).build());
            by += 26;
        }

        addRenderableWidget(Button.builder(Component.literal("Показать границу"),
                b -> CityActions.toggleBorder())
                .bounds(cx - 150, by, 150, 20).build());

        if (CityData.isMayor) {
            addRenderableWidget(Button.builder(Component.literal("Распустить город"),
                    b -> CityActions.disband())
                    .bounds(cx + 4, by, 150, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Покинуть город"),
                    b -> CityActions.leave())
                    .bounds(cx + 4, by, 150, 20).build());
        }
    }

    private void initTop(int cx, int top) {
        addRenderableWidget(Button.builder(Component.literal("Обновить топ"),
                b -> CityActions.requestTop())
                .bounds(cx - 60, this.height - 40, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 16, 0xFFFFFF);

        if (CityData.protocolMismatch) {
            g.drawCenteredString(this.font, Component.literal("§cВерсия мода не совпадает с сервером."),
                    cx, this.height / 2 - 10, 0xFF5555);
            g.drawCenteredString(this.font, Component.literal("§7Обнови лаунчер Domino Craft."),
                    cx, this.height / 2 + 4, 0xAAAAAA);
            return;
        }

        int top = 40;
        if (showTop) {
            renderTop(g, cx, top);
        } else if (CityData.hasCity) {
            renderCity(g, cx, top);
        } else {
            g.drawCenteredString(this.font, Component.literal("§7У тебя пока нет города."), cx, top + 12, 0xAAAAAA);
            g.drawCenteredString(this.font, Component.literal("§7Встань у своего Колокола и основай."), cx, top + 22, 0xAAAAAA);
        }

        // Строка результата последнего действия
        if (!CityData.lastResult.isEmpty()) {
            int color = CityData.lastOk ? 0x55FF55 : 0xFF5555;
            g.drawCenteredString(this.font, Component.literal(CityData.lastResult), cx, this.height - 60, color);
        }
    }

    private void renderCity(GuiGraphics g, int cx, int top) {
        g.drawString(this.font, Component.literal("§6" + CityData.cityName), cx - 150, top, 0xFFFFFF);
        g.drawString(this.font, Component.literal("§7Мэр: §f" + CityData.mayorName), cx - 150, top + 12, 0xFFFFFF);
        g.drawString(this.font, Component.literal("§7Радиус: §f" + CityData.radius
                + "  §7Очки: §f" + CityData.score), cx - 150, top + 24, 0xFFFFFF);

        int y = top + 44;
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            String tag = m.mayor() ? " §6(мэр)" : "";
            g.drawString(this.font, Component.literal("§f• " + m.name() + tag), cx - 150, y, 0xFFFFFF);
            y += 18;
        }
        if (CityData.members.size() > shown) {
            g.drawString(this.font, Component.literal("§7… ещё " + (CityData.members.size() - shown)),
                    cx - 150, y, 0xAAAAAA);
        }
    }

    private void renderTop(GuiGraphics g, int cx, int top) {
        if (CityData.top.isEmpty()) {
            g.drawCenteredString(this.font, Component.literal("§7Городов пока нет."), cx, top + 12, 0xAAAAAA);
            return;
        }
        int y = top;
        int place = 1;
        for (CityData.TopEntry e : CityData.top) {
            g.drawString(this.font, Component.literal("§e" + place + ". §f" + e.name()
                    + " §7— " + e.members() + " жит., " + e.score() + " очков"), cx - 150, y, 0xFFFFFF);
            y += 14;
            place++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
