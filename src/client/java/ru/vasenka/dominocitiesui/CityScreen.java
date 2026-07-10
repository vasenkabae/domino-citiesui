package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Окно управления городом. Три секции: «Мой город», «Хозяйство» (баффы/специализация/сбор), «Топы».
 * Данные читает из {@link CityData}; при их обновлении сервером экран перестраивается.
 */
public class CityScreen extends Screen {

    private static final int MODE_CITY = 0, MODE_ECONOMY = 1, MODE_TOP = 2;
    private int mode = MODE_CITY;
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

        String nextLabel = switch (mode) {
            case MODE_CITY -> "Хозяйство →";
            case MODE_ECONOMY -> "Топы →";
            default -> "← Мой город";
        };
        // В правом верхнем углу — подальше от центрированного заголовка, чтобы не наезжать
        // на него независимо от длины текста заголовка/названия города.
        addRenderableWidget(Button.builder(Component.literal(nextLabel), b -> {
            mode = (mode + 1) % 3;
            if (mode == MODE_TOP) CityActions.requestTop();
            rebuildWidgets();
        }).bounds(this.width - 140, 12, 130, 20).build());

        // Карта городов доступна всем, независимо от того, есть ли у игрока свой город.
        addRenderableWidget(Button.builder(Component.literal("Карта"),
                b -> CityActions.getMap())
                .bounds(this.width - 140, 36, 130, 20).build());

        if (CityData.protocolMismatch) {
            return; // текст-предупреждение рисуется в render()
        }

        if (mode == MODE_TOP) {
            initTop(cx, top);
        } else if (!CityData.hasCity) {
            initNoCity(cx, top);
        } else if (mode == MODE_ECONOMY) {
            initEconomy(cx, top);
        } else {
            initCity(cx, top);
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

        // Список жителей: кнопки управления зависят от роли смотрящего и роли цели.
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            boolean canKick = !m.isMayor() && (CityData.isMayor || (CityData.isOfficer && !m.isOfficer()));
            if (canKick) {
                addRenderableWidget(Button.builder(Component.literal("Кик"),
                        b -> CityActions.kick(m.uuid()))
                        .bounds(cx + 55, y - 2, 45, 16).build());
            }
            if (CityData.isMayor && !m.isMayor()) {
                String promoteLabel = m.isOfficer() ? "Понизить" : "Повысить";
                addRenderableWidget(Button.builder(Component.literal(promoteLabel),
                        b -> { if (m.isOfficer()) CityActions.demote(m.uuid()); else CityActions.promote(m.uuid()); })
                        .bounds(cx + 103, y - 2, 70, 16).build());
                addRenderableWidget(Button.builder(Component.literal("Мэру"),
                        b -> CityActions.transfer(m.uuid()))
                        .bounds(cx + 176, y - 2, 50, 16).build());
            }
            y += 18;
        }

        int by = top + 44 + shown * 18 + 8;

        // Поле приглашения (для мэра и офицеров)
        if (CityData.isMayor || CityData.isOfficer) {
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

    private void initEconomy(int cx, int top) {
        int y = top + 30;

        // Баффы: кнопка «Купить», если мэр, ещё не куплено и хватает очков.
        for (Catalogs.Buff b : Catalogs.BUFFS) {
            boolean bought = CityData.buffs.contains(b.id());
            if (!bought && CityData.isMayor && CityData.points >= b.cost()) {
                addRenderableWidget(Button.builder(Component.literal("Купить"),
                        btn -> CityActions.buyBuff(b.id()))
                        .bounds(cx + 60, y - 2, 90, 16).build());
            }
            y += 18;
        }

        y += 10; // здесь y совпадает с labelY в renderEconomy — важно для совпадения раскладки
        // Под "Специализация:" в renderEconomy всегда идёт ровно N строк текста по 14px:
        // 2 (заголовок + подсказка/название) или 3, если специализация выбрана (+ строка запаса).
        int specTextLines = CityData.specialization.isEmpty() ? 2 : 3;
        int buttonsY = y + specTextLines * 14;

        if (!CityData.specialization.isEmpty()) {
            if (CityData.isMayor && CityData.resourceStock > 0) {
                addRenderableWidget(Button.builder(Component.literal("Собрать"),
                        btn -> CityActions.collect())
                        .bounds(cx - 150, buttonsY, 150, 20).build());
            }
        } else if (CityData.isMayor) {
            int sy = buttonsY;
            for (Catalogs.Spec s : Catalogs.SPECS) {
                addRenderableWidget(Button.builder(Component.literal(s.displayName()),
                        btn -> CityActions.setSpecialization(s.id()))
                        .bounds(cx - 150, sy, 150, 18).build());
                sy += 20;
            }
        }
    }

    private void initTop(int cx, int top) {
        addRenderableWidget(Button.builder(Component.literal("Обновить топ"),
                b -> CityActions.requestTop())
                .bounds(cx - 60, this.height - 40, 120, 20).build());
    }

    // Новая система рендера 26.x: рисуем через GuiGraphicsExtractor.
    // Цвета ARGB — обязателен альфа-канал 0xFF, иначе текст «прозрачный».
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY  = 0xFFAAAAAA;
    private static final int GOLD  = 0xFFFFAA00;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        g.centeredText(this.font, this.title, cx, 16, WHITE);

        if (CityData.protocolMismatch) {
            g.centeredText(this.font, Component.literal("§cВерсия мода не совпадает с сервером."),
                    cx, this.height / 2 - 10, WHITE);
            g.centeredText(this.font, Component.literal("§7Обнови лаунчер Domino Craft."),
                    cx, this.height / 2 + 4, GRAY);
            return;
        }

        int top = 40;
        if (mode == MODE_TOP) {
            renderTop(g, cx, top);
        } else if (!CityData.hasCity) {
            g.centeredText(this.font, Component.literal("§7У тебя пока нет города."), cx, top + 12, GRAY);
            g.centeredText(this.font, Component.literal("§7Встань у своего Колокола и основай."), cx, top + 22, GRAY);
        } else if (mode == MODE_ECONOMY) {
            renderEconomy(g, cx, top);
        } else {
            renderCity(g, cx, top);
        }

        // Строка результата последнего действия
        if (!CityData.lastResult.isEmpty()) {
            g.centeredText(this.font, Component.literal((CityData.lastOk ? "§a" : "§c") + CityData.lastResult),
                    cx, this.height - 60, WHITE);
        }
    }

    private void renderCity(GuiGraphicsExtractor g, int cx, int top) {
        g.text(this.font, Component.literal("§6" + CityData.cityName), cx - 150, top, WHITE);
        g.text(this.font, Component.literal("§7Мэр: §f" + CityData.mayorName), cx - 150, top + 12, WHITE);
        g.text(this.font, Component.literal("§7Рейтинг: §f" + CityData.score
                + "  §7Радиус: §f" + CityData.radius), cx - 150, top + 24, WHITE);

        int y = top + 44;
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            String tag = m.isMayor() ? " §6(мэр)" : m.isOfficer() ? " §b(офицер)" : "";
            g.text(this.font, Component.literal("§f• " + m.name() + tag), cx - 150, y, WHITE);
            y += 18;
        }
        if (CityData.members.size() > shown) {
            g.text(this.font, Component.literal("§7… ещё " + (CityData.members.size() - shown)),
                    cx - 150, y, GRAY);
        }
    }

    private void renderEconomy(GuiGraphicsExtractor g, int cx, int top) {
        g.text(this.font, Component.literal("§6Очки города: §f" + CityData.points), cx - 150, top, WHITE);
        g.text(this.font, Component.literal("§7Баффы жителям (навсегда, платит мэр):"), cx - 150, top + 14, GRAY);

        int y = top + 30;
        for (Catalogs.Buff b : Catalogs.BUFFS) {
            boolean bought = CityData.buffs.contains(b.id());
            String status = bought ? "§a✔ куплено" : "§7" + b.cost() + " очков";
            g.text(this.font, Component.literal("§f" + b.displayName() + " " + status), cx - 150, y, WHITE);
            y += 18;
        }

        y += 10;
        g.text(this.font, Component.literal("§7Специализация:"), cx - 150, y, GRAY);
        y += 14;
        if (!CityData.specialization.isEmpty()) {
            g.text(this.font, Component.literal("§6" + Catalogs.specName(CityData.specialization)), cx - 150, y, GOLD);
            y += 14;
            g.text(this.font, Component.literal("§7Накоплено ресурсов: §f" + CityData.resourceStock),
                    cx - 150, y, WHITE);
        } else if (CityData.isMayor) {
            g.text(this.font, Component.literal("§7Выбери навсегда (кнопки ниже):"), cx - 150, y, GRAY);
        } else {
            g.text(this.font, Component.literal("§7Не выбрана — решает мэр."), cx - 150, y, GRAY);
        }
    }

    private void renderTop(GuiGraphicsExtractor g, int cx, int top) {
        if (CityData.top.isEmpty()) {
            g.centeredText(this.font, Component.literal("§7Городов пока нет."), cx, top + 12, GRAY);
            return;
        }
        int y = top;
        int place = 1;
        for (CityData.TopEntry e : CityData.top) {
            g.text(this.font, Component.literal("§e" + place + ". §f" + e.name()
                    + " §7— " + e.members() + " жит., " + e.score() + " очков"), cx - 150, y, WHITE);
            y += 14;
            place++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
