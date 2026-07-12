package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

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
    private static final int CONTRACT_MATCH_ROWS = 3; // сколько результатов поиска показывать за раз
    private static final int CONTRACT_FORM_TOP_MARGIN = 12;
    // подпись+выбор (12) + строка поиска/кол-ва (20) + список совпадений + отступ
    private static final int CONTRACT_PICKER_BLOCK_H = 12 + 20 + CONTRACT_MATCH_ROWS * 16 + 6;
    private static final int CONTRACT_BUTTON_H = 20;
    private static final int CONTRACT_LIST_GAP = 6;
    private String contractReqMaterial;   // null, пока не выбран кликом по совпадению из поиска
    private String contractRewMaterial;
    private EditBox contractReqSearch, contractRewSearch;
    private String pendingContractReqQuery = "";
    private String pendingContractRewQuery = "";
    private EditBox contractReqAmount;
    private String pendingContractReqAmount = "1";
    private EditBox contractRewAmount;
    private String pendingContractRewAmount = "1";

    public CityScreen() {
        super(Component.literal("Города Domino Craft"));
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        if (input != null) pendingText = input.getValue();
        if (titleInput != null) pendingTitleText = titleInput.getValue();
        if (contractReqAmount != null) pendingContractReqAmount = contractReqAmount.getValue();
        if (contractRewAmount != null) pendingContractRewAmount = contractRewAmount.getValue();
        if (contractReqSearch != null) pendingContractReqQuery = contractReqSearch.getValue();
        if (contractRewSearch != null) pendingContractRewQuery = contractRewSearch.getValue();
        // Периодический фоновый опрос (раз в 5 сек, см. DominoCitiesUIClient) не должен рвать
        // фокус и ввод, если игрок сейчас печатает — CityData уже обновлена и отрисуется в
        // любом случае (render читает её напрямую), а список кнопок подтянется на следующей
        // пересборке, когда поле перестанет быть в фокусе.
        if (isTypingInField()) return;
        rebuildWidgets();
    }

    private boolean isTypingInField() {
        return isFocused(input) || isFocused(titleInput)
                || isFocused(contractReqSearch) || isFocused(contractRewSearch)
                || isFocused(contractReqAmount) || isFocused(contractRewAmount);
    }

    private static boolean isFocused(EditBox box) {
        return box != null && box.isFocused();
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

    /** Заказ контракта — только у кого есть город; выполнить чужой контракт можно и без города. */
    private void initContracts(int cx, int top) {
        if (CityData.hasCity) {
            int y = top + CONTRACT_FORM_TOP_MARGIN;
            y = initResourcePicker(cx, y, true);
            y = initResourcePicker(cx, y, false);

            addRenderableWidget(Button.builder(Component.literal("Заказать контракт"),
                    b -> {
                        int reqAmt = parseAmount(contractReqAmount.getValue());
                        int rewAmt = parseAmount(contractRewAmount.getValue());
                        if (reqAmt > 0 && rewAmt > 0 && contractReqMaterial != null && contractRewMaterial != null) {
                            CityActions.createContract(contractReqMaterial, reqAmt, contractRewMaterial, rewAmt);
                        }
                    })
                    .bounds(cx - 150, y, 300, CONTRACT_BUTTON_H).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Обновить"),
                b -> CityActions.requestContracts())
                .bounds(cx - 60, this.height - 40, 120, 20).build());

        int y = contractsListTop(top);
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            CityData.ContractInfo c = CityData.contracts.get(i);
            addRenderableWidget(Button.builder(Component.literal("Взять"),
                    b -> CityActions.takeContract(c.id()))
                    .bounds(cx + 152, y, 55, 18).build());
            y += 20;
        }
    }

    /** Строка поиска ресурса + количество. Список совпадений рисуется/кликается отдельно (renderMatchRows/handleMatchClick) — вживую, по текущему тексту поля, без пересборки виджетов на каждую нажатую клавишу. */
    private int initResourcePicker(int cx, int y, boolean required) {
        int rowY = y + 12; // верхние 12px — подпись с текущим выбором, рисуется в renderResourcePickerLabel
        EditBox search = new EditBox(this.font, cx - 150, rowY, 130, 18, Component.literal("Поиск ресурса"));
        search.setMaxLength(30);
        search.setValue(required ? pendingContractReqQuery : pendingContractRewQuery);
        if (required) contractReqSearch = search; else contractRewSearch = search;
        addRenderableWidget(search);

        EditBox amount = new EditBox(this.font, cx - 10, rowY, 40, 18, Component.literal("Кол-во"));
        amount.setMaxLength(4);
        amount.setValue(required ? pendingContractReqAmount : pendingContractRewAmount);
        if (required) contractReqAmount = amount; else contractRewAmount = amount;
        addRenderableWidget(amount);

        return y + CONTRACT_PICKER_BLOCK_H;
    }

    private record MatchRect(Catalogs.Resource resource, int x, int y, int w, int h) {}

    /** Совпадения поиска для блока (required/reward) — читает EditBox ЖИВЬЁМ (getValue()), поэтому обновляется каждый кадр без rebuildWidgets(). Используется и рендером, и обработчиком клика — одна и та же геометрия. */
    private List<MatchRect> contractMatchRects(int cx, int blockY, boolean required) {
        EditBox box = required ? contractReqSearch : contractRewSearch;
        List<MatchRect> out = new ArrayList<>();
        if (box == null) return out;
        int matchY = blockY + 12 + 20;
        for (Catalogs.Resource r : Catalogs.search(box.getValue(), CONTRACT_MATCH_ROWS)) {
            out.add(new MatchRect(r, cx - 150, matchY, 258, 16));
            matchY += 16;
        }
        return out;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!CityData.protocolMismatch && mode == MODE_CONTRACTS && CityData.hasCity) {
            int cx = this.width / 2;
            int y = 60 + CONTRACT_FORM_TOP_MARGIN;
            if (handleMatchClick(event.x(), event.y(), cx, y, true)) return true;
            y += CONTRACT_PICKER_BLOCK_H;
            if (handleMatchClick(event.x(), event.y(), cx, y, false)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleMatchClick(double mouseX, double mouseY, int cx, int blockY, boolean required) {
        for (MatchRect m : contractMatchRects(cx, blockY, required)) {
            if (mouseX >= m.x() && mouseX < m.x() + m.w() && mouseY >= m.y() && mouseY < m.y() + m.h()) {
                if (required) contractReqMaterial = m.resource().id(); else contractRewMaterial = m.resource().id();
                return true;
            }
        }
        return false;
    }

    /** Общая раскладка формы заказа — вычисляется теми же константами, что и initContracts, так что не может разойтись. */
    private int contractsListTop(int top) {
        if (!CityData.hasCity) return top + 24 + CONTRACT_LIST_GAP; // две строки подсказки (12px) + отступ
        return top + CONTRACT_FORM_TOP_MARGIN + 2 * CONTRACT_PICKER_BLOCK_H + CONTRACT_BUTTON_H + CONTRACT_LIST_GAP;
    }

    private static int parseAmount(String s) {
        try { int v = Integer.parseInt(s.trim()); return v > 0 ? v : 0; } catch (Exception e) { return 0; }
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
        if (CityData.hasCity) {
            int y = top + CONTRACT_FORM_TOP_MARGIN;
            renderResourcePickerLabel(g, cx, y, "Нужно", contractReqMaterial);
            renderMatchRows(g, cx, y, true);
            y += CONTRACT_PICKER_BLOCK_H;
            renderResourcePickerLabel(g, cx, y, "Награда", contractRewMaterial);
            renderMatchRows(g, cx, y, false);
        } else {
            g.text(this.font, Component.literal("§7Чтобы заказывать контракты, вступи в город."), cx - 150, top, GRAY);
            g.text(this.font, Component.literal("§7Выполнять чужие контракты можно и без города."), cx - 150, top + 12, GRAY);
        }

        int y = contractsListTop(top);
        if (CityData.contracts.isEmpty()) {
            g.text(this.font, Component.literal("§7Контрактов пока нет."), cx - 150, y, GRAY);
            return;
        }
        renderContractRows(g, cx, y);
    }

    private void renderResourcePickerLabel(GuiGraphicsExtractor g, int cx, int y, String label, String selectedMaterialId) {
        String value = selectedMaterialId == null ? "§cне выбран" : "§a" + Catalogs.resourceName(selectedMaterialId);
        g.text(this.font, Component.literal("§7" + label + ": " + value), cx - 150, y, WHITE);
    }

    /** Совпадения поиска — подсвечивает уже выбранный вариант, чтобы было видно, что клик засчитался. */
    private void renderMatchRows(GuiGraphicsExtractor g, int cx, int blockY, boolean required) {
        String selected = required ? contractReqMaterial : contractRewMaterial;
        for (MatchRect m : contractMatchRects(cx, blockY, required)) {
            if (m.resource().id().equals(selected)) {
                g.fill(m.x(), m.y(), m.x() + m.w(), m.y() + m.h(), 0x552ECC71);
            }
            g.text(this.font, Component.literal("§f" + m.resource().displayName()), m.x() + 4, m.y() + 4, WHITE);
        }
    }

    private void renderContractRows(GuiGraphicsExtractor g, int cx, int startY) {
        int y = startY;
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            CityData.ContractInfo c = CityData.contracts.get(i);
            String req = Catalogs.resourceName(c.requiredMaterial());
            String rew = Catalogs.resourceName(c.rewardMaterial());
            g.text(this.font, Component.literal("§6" + c.cityName() + "§7: нужно §f" + c.requiredAmount()
                    + "× " + req + " §7→ награда §f" + c.rewardAmount() + "× " + rew), cx - 150, y + 4, WHITE);
            y += 20;
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
