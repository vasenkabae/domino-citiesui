package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Окно управления городом. Пять секций, все доступны сразу через вкладки сверху: «Мой город»,
 * «Хозяйство» (баффы/специализация/сбор/ресурсы в сундуках), «Все города» (справочник), «Топы»,
 * «Контракты» (доска объявлений всех городов).
 * Данные читает из {@link CityData}; при их обновлении сервером экран перестраивается.
 */
public class CityScreen extends Screen {

    private static final int MODE_CITY = 0, MODE_ECONOMY = 1, MODE_DIRECTORY = 2, MODE_TOP = 3, MODE_CONTRACTS = 4;
    private static final int MODE_COUNT = 5;
    private static final String[] TAB_LABELS = {"Мой город", "Хозяйство", "Все города", "Топы", "Контракты"};
    private int mode = MODE_CITY;
    private EditBox input;         // название (без города) или ник для приглашения (в городе)
    private String pendingText = "";
    private EditBox titleInput;    // новое название роли (мэр)
    private String pendingTitleText = "";

    public CityScreen() {
        super(Component.literal("Города Domino Craft"));
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        if (input != null) pendingText = input.getValue();
        if (titleInput != null) pendingTitleText = titleInput.getValue();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = 60;

        addTabBar();

        if (CityData.protocolMismatch) {
            return; // текст-предупреждение рисуется в render()
        }

        if (mode == MODE_TOP) {
            initTop(cx, top);
        } else if (mode == MODE_DIRECTORY) {
            initDirectory(cx, top);
        } else if (mode == MODE_CONTRACTS) {
            initContracts(cx, top);
        } else if (!CityData.hasCity) {
            initNoCity(cx, top);
        } else if (mode == MODE_ECONOMY) {
            initEconomy(cx, top);
        } else {
            initCity(cx, top);
        }
    }

    /** Все вкладки сразу в ряд — прямой переход по клику, без циклического «дальше». */
    private void addTabBar() {
        int tabWidth = Math.min(140, (this.width - 40) / MODE_COUNT);
        int totalWidth = tabWidth * MODE_COUNT;
        int startX = (this.width - totalWidth) / 2;
        for (int i = 0; i < MODE_COUNT; i++) {
            int m = i;
            Button btn = Button.builder(Component.literal(TAB_LABELS[i]), b -> {
                mode = m;
                if (mode == MODE_TOP) CityActions.requestTop();
                if (mode == MODE_DIRECTORY) CityActions.requestDirectory();
                if (mode == MODE_CONTRACTS) CityActions.requestContracts();
                rebuildWidgets();
            }).bounds(startX + i * tabWidth, 32, tabWidth - 2, 20).build();
            btn.active = (mode != m); // текущая вкладка выглядит нажатой (неактивной)
            addRenderableWidget(btn);
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
                addRenderableWidget(Button.builder(Component.literal("Передать права"),
                        b -> CityActions.transfer(m.uuid()))
                        .bounds(cx + 176, y - 2, 120, 16).build());
            }
            y += 18;
        }

        int by = top + 44 + shown * 18 + 8;

        // Поле приглашения (для мэра и офицеров) — при открытом городе не нужно, но не мешает.
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
            addRenderableWidget(Button.builder(
                    Component.literal(CityData.open ? "Сделать закрытым" : "Сделать открытым"),
                    b -> CityActions.toggleOpen())
                    .bounds(cx + 158, by, 150, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Покинуть город"),
                    b -> CityActions.leave())
                    .bounds(cx + 4, by, 150, 20).build());
        }
        by += 26;

        // Переименование ролей — только мэр.
        if (CityData.isMayor) {
            titleInput = new EditBox(this.font, cx - 150, by, 150, 20, Component.literal("Новое название"));
            titleInput.setMaxLength(16);
            titleInput.setHint(Component.literal("Новое название роли"));
            titleInput.setValue(pendingTitleText);
            addRenderableWidget(titleInput);
            addRenderableWidget(Button.builder(Component.literal(CityData.mayorTitle),
                    b -> CityActions.setTitle((byte) 2, titleInput.getValue()))
                    .bounds(cx + 4, by, 90, 20).build());
            addRenderableWidget(Button.builder(Component.literal(CityData.officerTitle),
                    b -> CityActions.setTitle((byte) 1, titleInput.getValue()))
                    .bounds(cx + 98, by, 90, 20).build());
            addRenderableWidget(Button.builder(Component.literal(CityData.memberTitle),
                    b -> CityActions.setTitle((byte) 0, titleInput.getValue()))
                    .bounds(cx + 192, by, 90, 20).build());
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

        // Ресурсы в сундуках — жёстко привязано ко дну экрана (список выше кнопки,
        // кнопка выше строки результата на height-60), не зависит от переменной высоты блока выше.
        addRenderableWidget(Button.builder(Component.literal("Показать сундуки"),
                b -> CityActions.requestResources())
                .bounds(cx - 75, this.height - 90, 150, 20).build());

        if (CityData.isMayor || CityData.isOfficer) {
            addRenderableWidget(Button.builder(Component.literal("Проложить дорогу (земля)"),
                    b -> CityActions.buildRoad())
                    .bounds(cx - 90, this.height - 115, 180, 20).build());
        }
    }

    private void initDirectory(int cx, int top) {
        addRenderableWidget(Button.builder(Component.literal("Обновить"),
                b -> CityActions.requestDirectory())
                .bounds(cx - 60, this.height - 40, 120, 20).build());

        int y = top;
        int shown = Math.min(6, CityData.directory.size());
        for (int i = 0; i < shown; i++) {
            CityData.CityInfo c = CityData.directory.get(i);
            if (c.open() && !c.name().equals(CityData.cityName)) {
                addRenderableWidget(Button.builder(Component.literal("Вступить"),
                        b -> CityActions.join(c.name()))
                        .bounds(cx + 160, y - 2, 70, 18).build());
            }
            y += 28;
        }
    }

    private void initTop(int cx, int top) {
        addRenderableWidget(Button.builder(Component.literal("Обновить топ"),
                b -> CityActions.requestTop())
                .bounds(cx - 60, this.height - 40, 120, 20).build());
    }

    private void initContracts(int cx, int top) {
        addRenderableWidget(Button.builder(Component.literal("Обновить"),
                b -> CityActions.requestContracts())
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

        int top = 60;
        if (mode == MODE_TOP) {
            renderTop(g, cx, top);
        } else if (mode == MODE_DIRECTORY) {
            renderDirectory(g, cx, top);
        } else if (mode == MODE_CONTRACTS) {
            renderContracts(g, cx, top);
        } else if (!CityData.hasCity) {
            g.centeredText(this.font, Component.literal("§7У тебя пока нет города."), cx, top + 12, GRAY);
            g.centeredText(this.font, Component.literal("§7Введи название и нажми «Основать» — город появится там, где стоишь."), cx, top + 22, GRAY);
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
        String openTag = CityData.open ? "§a(открытый)" : "§c(закрытый)";
        g.text(this.font, Component.literal("§6" + CityData.cityName + " " + openTag), cx - 150, top, WHITE);
        g.text(this.font, Component.literal("§7" + CityData.mayorTitle + ": §f" + CityData.mayorName), cx - 150, top + 12, WHITE);
        g.text(this.font, Component.literal("§7Рейтинг: §f" + CityData.score
                + "  §7Радиус: §f" + CityData.radius), cx - 150, top + 24, WHITE);

        int y = top + 44;
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            String tag = m.isMayor() ? " §6(" + CityData.mayorTitle + ")"
                    : m.isOfficer() ? " §b(" + CityData.officerTitle + ")" : "";
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

        // Сундуки — фиксированная зона над кнопкой "Показать сундуки" (height-90),
        // с запасом не доходит до неё, чтобы список и кнопка не наезжали друг на друга.
        int ry = this.height - 170;
        g.text(this.font, Component.literal("§7Ресурсы в сундуках (загруженные чанки):"), cx - 150, ry, GRAY);
        ry += 14;
        if (CityData.resources.isEmpty()) {
            g.text(this.font, Component.literal("§7(нажми «Показать сундуки»)"), cx - 150, ry, GRAY);
        } else {
            int shownRes = Math.min(4, CityData.resources.size());
            for (int i = 0; i < shownRes; i++) {
                CityData.ResourceEntry e = CityData.resources.get(i);
                g.text(this.font, Component.literal("§f" + e.material() + " §7— " + e.count()), cx - 150, ry, WHITE);
                ry += 12;
            }
            if (CityData.resources.size() > shownRes) {
                g.text(this.font, Component.literal("§7… ещё " + (CityData.resources.size() - shownRes) + " видов"),
                        cx - 150, ry, GRAY);
            }
        }
    }

    private void renderDirectory(GuiGraphicsExtractor g, int cx, int top) {
        if (CityData.directory.isEmpty()) {
            g.centeredText(this.font, Component.literal("§7Городов пока нет."), cx, top + 12, GRAY);
            return;
        }
        int y = top;
        int shown = Math.min(6, CityData.directory.size());
        for (int i = 0; i < shown; i++) {
            CityData.CityInfo c = CityData.directory.get(i);
            String tag = c.open() ? "§a(открыт)" : "§c(закрыт)";
            g.text(this.font, Component.literal("§6" + c.name() + " " + tag + " §7— мэр " + c.mayor()), cx - 150, y, WHITE);
            y += 12;
            String members = String.join(", ", c.memberNames());
            if (members.length() > 60) members = members.substring(0, 60) + "…";
            g.text(this.font, Component.literal("§7  " + members), cx - 150, y, GRAY);
            y += 16;
        }
        if (CityData.directory.size() > shown) {
            g.text(this.font, Component.literal("§7… ещё " + (CityData.directory.size() - shown) + " городов"),
                    cx - 150, y, GRAY);
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

    private void renderContracts(GuiGraphicsExtractor g, int cx, int top) {
        if (CityData.contracts.isEmpty()) {
            g.centeredText(this.font, Component.literal("§7Контрактов пока нет."), cx, top + 12, GRAY);
            g.centeredText(this.font,
                    Component.literal("§7/city contract create <материал> <кол-во> <награда> <кол-во>"),
                    cx, top + 24, GRAY);
            return;
        }
        int y = top;
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            CityData.ContractInfo c = CityData.contracts.get(i);
            g.text(this.font, Component.literal("§6" + c.cityName() + "§7: нужно §f" + c.requiredAmount()
                    + "× " + c.requiredMaterial() + " §7→ награда §f" + c.rewardAmount()
                    + "× " + c.rewardMaterial()), cx - 150, y, WHITE);
            y += 12;
            g.text(this.font, Component.literal("§7  сундук: " + c.world() + " " + c.x() + "/" + c.y() + "/" + c.z()),
                    cx - 150, y, GRAY);
            y += 16;
        }
        if (CityData.contracts.size() > shown) {
            g.text(this.font, Component.literal("§7… ещё " + (CityData.contracts.size() - shown) + " контрактов"),
                    cx - 150, y, GRAY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
