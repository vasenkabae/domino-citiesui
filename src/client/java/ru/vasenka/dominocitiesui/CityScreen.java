package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Окно управления городом. Шесть секций, все доступны сразу через вкладки сверху: «Мой город»,
 * «Хозяйство» (баффы/ресурсы в сундуках), «Все города» (справочник), «Топы»,
 * «Контракты» (доска объявлений всех городов), «Розыск» (заказ убийства/охота за головами).
 * Данные читает из {@link CityData}; при их обновлении сервером экран перестраивается.
 */
public class CityScreen extends Screen {

    private static final int MODE_CITY = 0, MODE_ECONOMY = 1, MODE_DIRECTORY = 2, MODE_TOP = 3,
            MODE_CONTRACTS = 4, MODE_BOUNTIES = 5;
    private static final int MODE_COUNT = 6;
    private static final String[] TAB_LABELS =
            {"Мой город", "Хозяйство", "Все города", "Топы", "Контракты", "Розыск"};
    private int mode = MODE_CITY;
    private EditBox input;         // название (без города) или ник для приглашения (в городе)
    private String pendingText = "";
    private EditBox titleInput;    // новое название роли (мэр)
    private String pendingTitleText = "";

    // ── Универсальный поиск-пикер ресурса (переиспользуется контрактами и розыском) ──
    private static final int PICKER_MATCH_ROWS = 3; // сколько результатов поиска показывать за раз
    // строка выбора (12) + строка поиска/кол-ва (20) + список совпадений + отступ
    private static final int PICKER_BLOCK_H = 12 + 20 + PICKER_MATCH_ROWS * 16 + 6;
    private static final int PICKER_BUTTON_H = 20;
    private static final int PICKER_LIST_GAP = 6;
    private final Map<String, EditBox> pickerSearch = new HashMap<>();
    private final Map<String, EditBox> pickerAmount = new HashMap<>();
    private final Map<String, String> pickerMaterial = new HashMap<>();       // null, пока не выбран кликом
    private final Map<String, String> pendingPickerQuery = new HashMap<>();
    private final Map<String, String> pendingPickerAmount = new HashMap<>();

    // ── Контракты ──
    private static final int CONTRACT_FORM_TOP_MARGIN = 12;

    // ── Розыск ──
    private static final int BOUNTY_NICK_TOP = 12;
    private static final int BOUNTY_NICK_ROW_H = 20 + 6; // поле ника + отступ до пикера
    private static final int MY_HUNT_BLOCK_H = 3 * 12 + 6; // 3 строки текста + отступ
    private EditBox bountyNickInput;
    private String pendingBountyNick = "";

    public CityScreen() {
        super(Component.literal("Города Domino Craft"));
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        if (input != null) pendingText = input.getValue();
        if (titleInput != null) pendingTitleText = titleInput.getValue();
        if (bountyNickInput != null) pendingBountyNick = bountyNickInput.getValue();
        for (var e : pickerSearch.entrySet()) pendingPickerQuery.put(e.getKey(), e.getValue().getValue());
        for (var e : pickerAmount.entrySet()) pendingPickerAmount.put(e.getKey(), e.getValue().getValue());
        // Периодический фоновый опрос (раз в 5 сек, см. DominoCitiesUIClient) не должен рвать
        // фокус и ввод, если игрок сейчас печатает — CityData уже обновлена и отрисуется в
        // любом случае (render читает её напрямую), а список кнопок подтянется на следующей
        // пересборке, когда поле перестанет быть в фокусе.
        if (isTypingInField()) return;
        rebuildWidgets();
    }

    private boolean isTypingInField() {
        if (isFocused(input) || isFocused(titleInput) || isFocused(bountyNickInput)) return true;
        for (EditBox b : pickerSearch.values()) if (isFocused(b)) return true;
        for (EditBox b : pickerAmount.values()) if (isFocused(b)) return true;
        return false;
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
        } else if (mode == MODE_BOUNTIES) {
            initBounties(cx, top);
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
                if (mode == MODE_BOUNTIES) CityActions.requestBounties();
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
            y = initResourcePicker(cx, y, "contractReq");
            y = initResourcePicker(cx, y, "contractRew");

            addRenderableWidget(Button.builder(Component.literal("Заказать контракт"),
                    b -> {
                        int reqAmt = pickerAmountValue("contractReq");
                        int rewAmt = pickerAmountValue("contractRew");
                        String reqMat = pickerMaterial.get("contractReq");
                        String rewMat = pickerMaterial.get("contractRew");
                        if (reqAmt > 0 && rewAmt > 0 && reqMat != null && rewMat != null) {
                            CityActions.createContract(reqMat, reqAmt, rewMat, rewAmt);
                        }
                    })
                    .bounds(cx - 150, y, 300, PICKER_BUTTON_H).build());
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

    private int contractsListTop(int top) {
        if (!CityData.hasCity) return top + 24 + PICKER_LIST_GAP; // две строки подсказки (12px) + отступ
        return top + CONTRACT_FORM_TOP_MARGIN + 2 * PICKER_BLOCK_H + PICKER_BUTTON_H + PICKER_LIST_GAP;
    }

    /**
     * Заказать розыск может кто угодно (не только житель города); ник цели — произвольный текст,
     * награда выбирается тем же поиском-пикером, что и в контрактах.
     */
    private void initBounties(int cx, int top) {
        int nickY = top + BOUNTY_NICK_TOP;
        bountyNickInput = new EditBox(this.font, cx - 150, nickY, 300, 18, Component.literal("Ник цели"));
        bountyNickInput.setMaxLength(16);
        bountyNickInput.setValue(pendingBountyNick);
        addRenderableWidget(bountyNickInput);

        int pickerY = nickY + BOUNTY_NICK_ROW_H;
        int afterPicker = initResourcePicker(cx, pickerY, "bountyReward");

        addRenderableWidget(Button.builder(Component.literal("Заказать розыск"),
                b -> {
                    String nick = bountyNickInput.getValue().trim();
                    int amount = pickerAmountValue("bountyReward");
                    String material = pickerMaterial.get("bountyReward");
                    if (!nick.isEmpty() && amount > 0 && material != null) {
                        CityActions.createBounty(nick, material, amount);
                    }
                })
                .bounds(cx - 150, afterPicker, 300, PICKER_BUTTON_H).build());

        addRenderableWidget(Button.builder(Component.literal("Обновить"),
                b -> CityActions.requestBounties())
                .bounds(cx - 60, this.height - 40, 120, 20).build());

        int y = bountyListTop(top);
        int shown = Math.min(6, CityData.bounties.size());
        for (int i = 0; i < shown; i++) {
            CityData.BountyInfo b = CityData.bounties.get(i);
            if (!b.claimed()) {
                addRenderableWidget(Button.builder(Component.literal("Взять"),
                        btn -> CityActions.takeBounty(b.id()))
                        .bounds(cx + 152, y, 55, 18).build());
            }
            y += 20;
        }
    }

    private int bountyListTop(int top) {
        int pickerY = top + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
        int afterPicker = pickerY + PICKER_BLOCK_H;
        int afterButton = afterPicker + PICKER_BUTTON_H + PICKER_LIST_GAP;
        return CityData.myHunt != null ? afterButton + MY_HUNT_BLOCK_H : afterButton;
    }

    // ── Универсальный поиск-пикер ресурса ──────────────────────────────────

    /** Строка поиска ресурса + количество. Совпадения рисуются/кликаются отдельно (см. renderPickerMatches/handlePickerClick) — вживую, по текущему тексту поля, без пересборки виджетов на каждую нажатую клавишу. */
    private int initResourcePicker(int cx, int y, String key) {
        int rowY = y + 12; // верхние 12px — подпись с текущим выбором, рисуется в renderPickerLabel
        EditBox search = new EditBox(this.font, cx - 150, rowY, 130, 18, Component.literal("Поиск ресурса"));
        search.setMaxLength(30);
        search.setValue(pendingPickerQuery.getOrDefault(key, ""));
        pickerSearch.put(key, search);
        addRenderableWidget(search);

        EditBox amount = new EditBox(this.font, cx - 10, rowY, 40, 18, Component.literal("Кол-во"));
        amount.setMaxLength(4);
        amount.setValue(pendingPickerAmount.getOrDefault(key, "1"));
        pickerAmount.put(key, amount);
        addRenderableWidget(amount);

        return y + PICKER_BLOCK_H;
    }

    private record MatchRect(Catalogs.Resource resource, int x, int y, int w, int h) {}

    /** Совпадения поиска для пикера — читает EditBox ЖИВЬЁМ (getValue()), поэтому обновляется каждый кадр без rebuildWidgets(). Используется и рендером, и обработчиком клика — одна и та же геометрия. */
    private List<MatchRect> pickerMatchRects(int cx, int blockY, String key) {
        EditBox box = pickerSearch.get(key);
        List<MatchRect> out = new ArrayList<>();
        if (box == null) return out;
        int matchY = blockY + 12 + 20;
        for (Catalogs.Resource r : Catalogs.search(box.getValue(), PICKER_MATCH_ROWS)) {
            out.add(new MatchRect(r, cx - 150, matchY, 258, 16));
            matchY += 16;
        }
        return out;
    }

    private void renderPickerLabel(GuiGraphicsExtractor g, int cx, int y, String label, String key) {
        String materialId = pickerMaterial.get(key);
        String value = materialId == null ? "§cне выбран" : "§a" + Catalogs.resourceName(materialId);
        g.text(this.font, Component.literal("§7" + label + ": " + value), cx - 150, y, WHITE);
    }

    /** Совпадения поиска — подсвечивает уже выбранный вариант, чтобы было видно, что клик засчитался. */
    private void renderPickerMatches(GuiGraphicsExtractor g, int cx, int blockY, String key) {
        String selected = pickerMaterial.get(key);
        for (MatchRect m : pickerMatchRects(cx, blockY, key)) {
            if (m.resource().id().equals(selected)) {
                g.fill(m.x(), m.y(), m.x() + m.w(), m.y() + m.h(), 0x552ECC71);
            }
            g.text(this.font, Component.literal("§f" + m.resource().displayName()), m.x() + 4, m.y() + 4, WHITE);
        }
    }

    private boolean handlePickerClick(double mouseX, double mouseY, int cx, int blockY, String key) {
        for (MatchRect m : pickerMatchRects(cx, blockY, key)) {
            if (mouseX >= m.x() && mouseX < m.x() + m.w() && mouseY >= m.y() && mouseY < m.y() + m.h()) {
                pickerMaterial.put(key, m.resource().id());
                return true;
            }
        }
        return false;
    }

    private int pickerAmountValue(String key) {
        EditBox box = pickerAmount.get(key);
        return box != null ? parseAmount(box.getValue()) : 0;
    }

    private static int parseAmount(String s) {
        try { int v = Integer.parseInt(s.trim()); return v > 0 ? v : 0; } catch (Exception e) { return 0; }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!CityData.protocolMismatch) {
            int cx = this.width / 2;
            if (mode == MODE_CONTRACTS && CityData.hasCity) {
                int y = 60 + CONTRACT_FORM_TOP_MARGIN;
                if (handlePickerClick(event.x(), event.y(), cx, y, "contractReq")) return true;
                y += PICKER_BLOCK_H;
                if (handlePickerClick(event.x(), event.y(), cx, y, "contractRew")) return true;
            } else if (mode == MODE_BOUNTIES) {
                int y = 60 + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
                if (handlePickerClick(event.x(), event.y(), cx, y, "bountyReward")) return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // Новая система рендера 26.x: рисуем через GuiGraphicsExtractor.
    // Цвета ARGB — обязателен альфа-канал 0xFF, иначе текст «прозрачный».
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY  = 0xFFAAAAAA;

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
            g.centeredText(this.font, Component.literal("§8клиент v" + Protocol.VERSION
                            + ", получено v" + CityData.lastReceivedVersion),
                    cx, this.height / 2 + 18, GRAY);
            return;
        }

        int top = 60;
        if (mode == MODE_TOP) {
            renderTop(g, cx, top);
        } else if (mode == MODE_DIRECTORY) {
            renderDirectory(g, cx, top);
        } else if (mode == MODE_CONTRACTS) {
            renderContracts(g, cx, top);
        } else if (mode == MODE_BOUNTIES) {
            renderBounties(g, cx, top);
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
            renderPickerLabel(g, cx, y, "Нужно", "contractReq");
            renderPickerMatches(g, cx, y, "contractReq");
            y += PICKER_BLOCK_H;
            renderPickerLabel(g, cx, y, "Награда", "contractRew");
            renderPickerMatches(g, cx, y, "contractRew");
        } else {
            g.text(this.font, Component.literal("§7Чтобы заказывать контракты, вступи в город."), cx - 150, top, GRAY);
            g.text(this.font, Component.literal("§7Выполнять чужие контракты можно и без города."), cx - 150, top + 12, GRAY);
        }

        int y = contractsListTop(top);
        if (CityData.contracts.isEmpty()) {
            g.text(this.font, Component.literal("§7Контрактов пока нет."), cx - 150, y, GRAY);
            return;
        }
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

    private void renderBounties(GuiGraphicsExtractor g, int cx, int top) {
        g.text(this.font, Component.literal("§7Заказать розыск — ник цели + награда из инвентаря:"), cx - 150, top, GRAY);

        int pickerY = top + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
        renderPickerLabel(g, cx, pickerY, "Награда", "bountyReward");
        renderPickerMatches(g, cx, pickerY, "bountyReward");

        int y = bountyListTop(top);

        if (CityData.myHunt != null) {
            CityData.MyHunt hunt = CityData.myHunt;
            int hy = y - MY_HUNT_BLOCK_H;
            g.text(this.font, Component.literal("§6[Розыск] §7Охотишься на: §f" + hunt.targetName()), cx - 150, hy, WHITE);
            hy += 12;
            String coordsLine = hunt.hasCoords()
                    ? "§eПоследние координаты: " + hunt.world() + " " + hunt.x() + "/" + hunt.y() + "/" + hunt.z()
                        + " §7(" + ((System.currentTimeMillis() - hunt.lastRevealAt()) / 60000) + " мин назад)"
                    : "§7Координаты придут через 30 минут после взятия заказа.";
            g.text(this.font, Component.literal(coordsLine), cx - 150, hy, WHITE);
            hy += 12;
            g.text(this.font, Component.literal("§cПогибнешь — заказ провалится и достанется другому."), cx - 150, hy, GRAY);
        }

        if (CityData.bounties.isEmpty()) {
            g.text(this.font, Component.literal("§7Заказов пока нет."), cx - 150, y, GRAY);
            return;
        }
        int shown = Math.min(6, CityData.bounties.size());
        for (int i = 0; i < shown; i++) {
            CityData.BountyInfo b = CityData.bounties.get(i);
            String status = b.claimed() ? "§cв розыске" : "§aсвободен";
            g.text(this.font, Component.literal("§6" + b.targetName() + "§7: награда §f" + b.rewardAmount()
                    + "× " + Catalogs.resourceName(b.rewardMaterial()) + " §7— " + status), cx - 150, y + 4, WHITE);
            y += 20;
        }
        if (CityData.bounties.size() > shown) {
            g.text(this.font, Component.literal("§7… ещё " + (CityData.bounties.size() - shown) + " заказов"),
                    cx - 150, y, GRAY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
