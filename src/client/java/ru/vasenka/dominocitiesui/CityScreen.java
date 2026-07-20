package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Окно управления городом. Шесть секций, все доступны сразу через вкладки сверху: «Мой город»,
 * «Хозяйство» (баффы/ресурсы в сундуках), «Все города» (справочник), «Топы»,
 * «Контракты» (доска объявлений всех городов), «Розыск» (заказ убийства/охота за головами).
 * Данные читает из {@link CityData}; при их обновлении сервером экран перестраивается.
 *
 * Визуальный слой: тёмная стеклянная панель с золотым акцентом. Фоновые подложки (панель,
 * вкладки, карточки, зебра списков) рисуются в extractBackground — ПОД ванильными виджетами;
 * тексты — в extractRenderState, поверх. Геометрия у обеих фаз общая (одни и те же функции).
 */
public class CityScreen extends Screen {

    private static final int MODE_CITY = 0, MODE_ECONOMY = 1, MODE_DIRECTORY = 2, MODE_TOP = 3,
            MODE_CONTRACTS = 4, MODE_BOUNTIES = 5, MODE_MARKET = 6, MODE_EVENTS = 7;
    private static final int MODE_COUNT = 8;
    private static final String[] TAB_LABELS =
            {"Мой город", "Хозяйство", "Все города", "Топы", "Контракты", "Розыск", "Рынок", "Ивенты"};
    private int mode = MODE_CITY;
    private EditBox input;         // название (без города) или ник для приглашения (в городе)
    private String pendingText = "";
    private EditBox titleInput;    // новое название роли (мэр)
    private String pendingTitleText = "";

    // ── Палитра ──────────────────────────────────────────────────────────────
    private static final int WHITE  = 0xFFF2F3F5;
    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int GREEN  = 0xFF66D98F;
    private static final int RED    = 0xFFEB7069;
    private static final int BLUE   = 0xFF7FB9EB;
    private static final int SILVER = 0xFFC7CCD6;
    private static final int BRONZE = 0xFFCB8F5A;

    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int LINE         = 0x1AFFFFFF;
    private static final int CARD         = 0x2E000000;
    private static final int ROW_A        = 0x10FFFFFF;
    private static final int ROW_HOVER    = 0x1EFFFFFF;
    private static final int TAB_ACTIVE_BG = 0x26F2B94E;
    private static final int SELECT_BG    = 0x4027AE60;
    private static final int CHIP_GREEN_BG = 0x4020703C;
    private static final int CHIP_RED_BG   = 0x40702A26;

    // ── Геометрия ────────────────────────────────────────────────────────────
    private static final int CONTENT_TOP = 60; // верх контента; на него завязаны клики пикеров
    private static final int PAD = 16;
    private static final int PANEL_Y1 = 24;
    private static final int TAB_Y = 30, TAB_H = 22;

    private int cx() { return this.width / 2; }
    private int panelHalf() { return Math.min(250, (this.width - 12) / 2); }
    private int px1() { return cx() - panelHalf(); }
    private int px2() { return cx() + panelHalf(); }
    private int py2() { return this.height - 12; }
    private int left() { return px1() + PAD; }
    private int right() { return px2() - PAD; }

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

    // ── Рынок ──
    private static final int MARKET_FORM_TOP_MARGIN = 12;
    private static final int MARKET_ROW_H = 20;
    private static final int MARKET_GAP = 6;
    private EditBox marketPriceInput;
    private EditBox marketQuantityInput;
    private String pendingMarketPrice = "";
    private String pendingMarketQuantity = "1";

    // ── Ивенты ──
    private static final int EVENT_FORM_TOP_MARGIN = 12;
    private static final int EVENT_ROW_H2 = 20; // высота полей формы (название/описание/дата/кнопка)
    private static final int EVENT_GAP = 6;
    private static final int EVENT_ROW_H = 54; // высота карточки ивента в списке (4 строки текста)
    private static final int EVENT_BTN_W = 64; // ширина колонки кнопок «Участвую»/«Удалить»
    private EditBox eventTitleInput;
    private EditBox eventDescInput;
    private EditBox eventDateInput;
    private String pendingEventTitle = "";
    private String pendingEventDesc = "";
    private String pendingEventDate = "";

    // ── Постройки/рейтинг/комментарии (карточка города внутри «Все города») ──
    private static final int CARD_INFO_TOP = 28;        // строка описания
    private static final int CARD_TOOLBAR_TOP = 40;      // рейтинг + подвкладки
    private static final int BUILDING_LIST_TOP = 58;     // после шапки карточки города
    private static final int BUILDING_ROW_H = 40;
    private static final int BUILDING_THUMB_W = 56, BUILDING_THUMB_H = 32;
    private static final int COMMENT_ROW_H = 34;
    private static final SimpleDateFormat BUILDING_DATE = new SimpleDateFormat("dd.MM.yyyy");
    private static final int CARD_SUB_BUILDINGS = 0, CARD_SUB_COMMENTS = 1, CARD_SUB_LAWS = 2;
    private static final int LAW_ROW_H = 24;
    private String selectedCity = null;    // null — показываем список городов
    private int selectedBuildingId = -1;   // -1 — список построек выбранного города
    private boolean buildingFormOpen = false;
    private EditBox buildingNameInput, buildingDescInput;
    private String pendingBuildingName = "", pendingBuildingDesc = "";
    private int cardSubMode = CARD_SUB_BUILDINGS;
    private EditBox commentInput;
    private String pendingComment = "";
    private EditBox lawInput;
    private String pendingLaw = "";
    private EditBox mayorDescInput;
    private String pendingMayorDesc = "";
    private boolean mayorDescSeeded = false; // seed pendingMayorDesc from CityData.description ровно один раз
    private EditBox cityNameInput;
    private String pendingCityName = "";
    private boolean cityNameSeeded = false; // seed pendingCityName from CityData.cityName ровно один раз

    // Подсказка с полным текстом обрезанного поля (описание/комментарий/закон) — рисуется поверх в конце кадра.
    private int hoverMouseX = -1, hoverMouseY = -1;
    private String hoverTip = null;

    public CityScreen() {
        super(Component.literal("Города Domino Craft"));
    }

    /** Вызывается из CityData при получении свежих данных. */
    public void refresh() {
        if (input != null) pendingText = input.getValue();
        if (titleInput != null) pendingTitleText = titleInput.getValue();
        if (bountyNickInput != null) pendingBountyNick = bountyNickInput.getValue();
        if (marketPriceInput != null) pendingMarketPrice = marketPriceInput.getValue();
        if (marketQuantityInput != null) pendingMarketQuantity = marketQuantityInput.getValue();
        if (buildingNameInput != null) pendingBuildingName = buildingNameInput.getValue();
        if (buildingDescInput != null) pendingBuildingDesc = buildingDescInput.getValue();
        if (commentInput != null) pendingComment = commentInput.getValue();
        if (mayorDescInput != null) pendingMayorDesc = mayorDescInput.getValue();
        if (lawInput != null) pendingLaw = lawInput.getValue();
        if (cityNameInput != null) pendingCityName = cityNameInput.getValue();
        if (eventTitleInput != null) pendingEventTitle = eventTitleInput.getValue();
        if (eventDescInput != null) pendingEventDesc = eventDescInput.getValue();
        if (eventDateInput != null) pendingEventDate = eventDateInput.getValue();
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
        if (isFocused(input) || isFocused(titleInput) || isFocused(bountyNickInput)
                || isFocused(marketPriceInput) || isFocused(marketQuantityInput)
                || isFocused(buildingNameInput) || isFocused(buildingDescInput)
                || isFocused(commentInput) || isFocused(mayorDescInput) || isFocused(lawInput)
                || isFocused(cityNameInput) || isFocused(eventTitleInput) || isFocused(eventDescInput)
                || isFocused(eventDateInput)) return true;
        for (EditBox b : pickerSearch.values()) if (isFocused(b)) return true;
        for (EditBox b : pickerAmount.values()) if (isFocused(b)) return true;
        return false;
    }

    private static boolean isFocused(EditBox box) {
        return box != null && box.isFocused();
    }

    @Override
    protected void init() {
        int top = CONTENT_TOP;

        // Памятка сервера — маленький «?» над правым верхним углом панели.
        addRenderableWidget(FancyButton.of(Component.literal("?"), px2() - 18, 6, 18, 16, () -> net.minecraft.client.Minecraft.getInstance().setScreen(new GuideScreen())));

        if (CityData.protocolMismatch) {
            return; // предупреждение рисуется в render()
        }

        if (mode == MODE_TOP) {
            initTop(top);
        } else if (mode == MODE_DIRECTORY) {
            initDirectory(top);
        } else if (mode == MODE_CONTRACTS) {
            initContracts(top);
        } else if (mode == MODE_BOUNTIES) {
            initBounties(top);
        } else if (mode == MODE_MARKET) {
            initMarket(top);
        } else if (mode == MODE_EVENTS) {
            initEvents(top);
        } else if (!CityData.hasCity) {
            initNoCity(top);
        } else if (mode == MODE_ECONOMY) {
            initEconomy(top);
        } else {
            initCity(top);
        }
    }

    // ── Вкладки (кастомные: рисуем и кликаем сами, hover бесплатно) ──────────

    private record TabRect(int index, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private List<TabRect> tabRects() {
        int innerW = px2() - px1() - 8;
        int tabW = innerW / MODE_COUNT;
        int startX = px1() + 4 + (innerW - tabW * MODE_COUNT) / 2;
        List<TabRect> out = new ArrayList<>(MODE_COUNT);
        for (int i = 0; i < MODE_COUNT; i++) {
            out.add(new TabRect(i, startX + i * tabW, TAB_Y, tabW - 2, TAB_H));
        }
        return out;
    }

    private void switchMode(int m) {
        mode = m;
        // Возврат на вкладку «Все города» всегда начинается со списка городов.
        selectedCity = null;
        selectedBuildingId = -1;
        buildingFormOpen = false;
        cardSubMode = CARD_SUB_BUILDINGS;
        if (mode == MODE_TOP) CityActions.requestTop();
        if (mode == MODE_DIRECTORY) CityActions.requestDirectory();
        if (mode == MODE_CONTRACTS) CityActions.requestContracts();
        if (mode == MODE_BOUNTIES) CityActions.requestBounties();
        if (mode == MODE_MARKET) CityActions.requestMarket();
        if (mode == MODE_EVENTS) CityActions.requestEvents();
        rebuildWidgets();
    }

    // ── init по вкладкам ─────────────────────────────────────────────────────

    private void initNoCity(int top) {
        input = new EditBox(this.font, cx() - 100, top + 46, 200, 20, Component.literal("Название"));
        input.setMaxLength(20);
        input.setHint(Component.literal("Название города (3–20)"));
        input.setValue(pendingText);
        addRenderableWidget(input);

        addRenderableWidget(FancyButton.of(Component.literal("Основать город"), cx() - 100, top + 72, 200, 20, () -> { CityActions.create(input.getValue()); }));
    }

    private void initCity(int top) {
        int y = top + 44;

        // Список жителей: кнопки управления зависят от роли смотрящего и роли цели.
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            boolean canKick = !m.isMayor() && (CityData.isMayor || (CityData.isOfficer && !m.isOfficer()));
            if (canKick) {
                addRenderableWidget(FancyButton.of(Component.literal("Кик"), right() - 176, y - 2, 40, 16, () -> CityActions.kick(m.uuid())));
            }
            if (CityData.isMayor && !m.isMayor()) {
                String promoteLabel = m.isOfficer() ? "Понизить" : "Повысить";
                addRenderableWidget(FancyButton.of(Component.literal(promoteLabel), right() - 132, y - 2, 64, 16, () -> { if (m.isOfficer()) CityActions.demote(m.uuid()); else CityActions.promote(m.uuid()); }));
                addRenderableWidget(FancyButton.of(Component.literal("Передать"), right() - 64, y - 2, 64, 16, () -> CityActions.transfer(m.uuid())));
            }
            y += 18;
        }

        int by = top + 44 + shown * 18 + 8;

        // Поле приглашения (для мэра и офицеров) — при открытом городе не нужно, но не мешает.
        if (CityData.isMayor || CityData.isOfficer) {
            input = new EditBox(this.font, left(), by, 170, 20, Component.literal("Ник"));
            input.setMaxLength(16);
            input.setHint(Component.literal("Ник игрока"));
            input.setValue(pendingText);
            addRenderableWidget(input);
            addRenderableWidget(FancyButton.of(Component.literal("Пригласить"), left() + 176, by, 96, 20, () -> { CityActions.invite(input.getValue()); }));
            by += 26;
        }

        addRenderableWidget(FancyButton.of(Component.literal("Показать границу"), left(), by, 142, 20, () -> CityActions.toggleBorder()));

        if (CityData.isMayor) {
            addRenderableWidget(FancyButton.of(Component.literal("Распустить город"), left() + 146, by, 142, 20, () -> CityActions.disband()));
            addRenderableWidget(FancyButton.of(Component.literal(CityData.open ? "Сделать закрытым" : "Сделать открытым"), left() + 292, by, 142, 20, () -> CityActions.toggleOpen()));
        } else {
            addRenderableWidget(FancyButton.of(Component.literal("Покинуть город"), left() + 146, by, 142, 20, () -> CityActions.leave()));
        }
        by += 26;

        // Переименование ролей — только мэр.
        if (CityData.isMayor) {
            titleInput = new EditBox(this.font, left(), by, 142, 20, Component.literal("Новое название"));
            titleInput.setMaxLength(16);
            titleInput.setHint(Component.literal("Новое название роли"));
            titleInput.setValue(pendingTitleText);
            addRenderableWidget(titleInput);
            addRenderableWidget(FancyButton.of(Component.literal(CityData.mayorTitle), left() + 146, by, 94, 20, () -> CityActions.setTitle((byte) 2, titleInput.getValue())));
            addRenderableWidget(FancyButton.of(Component.literal(CityData.officerTitle), left() + 244, by, 94, 20, () -> CityActions.setTitle((byte) 1, titleInput.getValue())));
            addRenderableWidget(FancyButton.of(Component.literal(CityData.memberTitle), left() + 342, by, 94, 20, () -> CityActions.setTitle((byte) 0, titleInput.getValue())));
            by += 26;

            // Переименование самого города — только мэр.
            if (!cityNameSeeded) { pendingCityName = CityData.cityName; cityNameSeeded = true; }
            cityNameInput = new EditBox(this.font, left(), by, right() - left() - 94, 20,
                    Component.literal("Название города"));
            cityNameInput.setMaxLength(20);
            cityNameInput.setHint(Component.literal("Новое название города (3–20)"));
            cityNameInput.setValue(pendingCityName);
            addRenderableWidget(cityNameInput);
            addRenderableWidget(FancyButton.of(Component.literal("Переименовать"), right() - 90, by, 90, 20, () -> CityActions.renameCity(cityNameInput.getValue())));
            by += 26;

            if (!mayorDescSeeded) { pendingMayorDesc = CityData.description; mayorDescSeeded = true; }
            mayorDescInput = new EditBox(this.font, left(), by, right() - left() - 94, 20,
                    Component.literal("Описание города"));
            mayorDescInput.setMaxLength(200);
            mayorDescInput.setHint(Component.literal("Описание города (до 200, видно всем)"));
            mayorDescInput.setValue(pendingMayorDesc);
            addRenderableWidget(mayorDescInput);
            addRenderableWidget(FancyButton.of(Component.literal("Сохранить"), right() - 90, by, 90, 20, () -> CityActions.setCityDescription(mayorDescInput.getValue())));
            by += 26;

            // Платное расширение границы города по чанкам (за алмазы).
            boolean atMax = CityData.boughtRadius + CityData.expandStep > CityData.expandMax;
            String exLabel = atMax
                    ? "Граница: максимум (+" + CityData.expandMax + " блоков)"
                    : "Расширить границу +" + CityData.expandStep + " (" + CityData.expandCost + " алм.)";
            FancyButton exBtn = FancyButton.of(Component.literal(exLabel), left(), by, right() - left(), 20, () -> CityActions.expandCity());
            exBtn.active = !atMax;
            addRenderableWidget(exBtn);
        }
    }

    private void initEconomy(int top) {
        int y = top + 30;

        // Баффы: кнопка «Купить», если мэр, ещё не куплено и хватает очков.
        for (Catalogs.Buff b : Catalogs.BUFFS) {
            boolean bought = CityData.buffs.contains(b.id());
            if (!bought && CityData.isMayor && CityData.points >= b.cost()) {
                addRenderableWidget(FancyButton.of(Component.literal("Купить"), right() - 76, y - 2, 76, 16, () -> CityActions.buyBuff(b.id())));
            }
            y += 18;
        }

        // Ресурсы в сундуках — жёстко привязано ко дну экрана (список выше кнопки,
        // кнопка выше строки результата на height-60), не зависит от переменной высоты блока выше.
        addRenderableWidget(FancyButton.of(Component.literal("Показать сундуки"), cx() - 75, this.height - 90, 150, 20, () -> CityActions.requestResources()));

        if (CityData.isMayor || CityData.isOfficer) {
            addRenderableWidget(FancyButton.of(Component.literal("Проложить дорогу (земля)"), cx() - 90, this.height - 115, 180, 20, () -> CityActions.buildRoad()));
        }
    }

    private void initDirectory(int top) {
        if (selectedCity != null) { initCityCard(top); return; }

        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestDirectory()));

        int y = top;
        int shown = Math.min(6, CityData.directory.size());
        boolean canRoad = CityData.isMayor || CityData.isOfficer;
        for (int i = 0; i < shown; i++) {
            CityData.CityInfo c = CityData.directory.get(i);
            if (c.open() && !c.name().equals(CityData.cityName)) {
                addRenderableWidget(FancyButton.of(Component.literal("Вступить"), right() - 70, y - 1, 66, 18, () -> CityActions.join(c.name())));
            }
            // Дорога между городами: от ядра своего города до этого (мэр/офицер, за землю)
            if (canRoad && !c.name().equals(CityData.cityName)) {
                addRenderableWidget(FancyButton.of(Component.literal("Дорога"), right() - 140, y - 1, 62, 18, () -> CityActions.roadToCity(c.name())));
            }
            y += 28;
        }
    }

    /** Карточка выбранного города: рейтинг/комментарии + список построек / детали / форма сохранения. */
    private void initCityCard(int top) {
        addRenderableWidget(FancyButton.of(Component.literal("← Назад"), left(), top - 4, 70, 18, () -> {
            if (buildingFormOpen) buildingFormOpen = false;
            else if (selectedBuildingId != -1) selectedBuildingId = -1;
            else selectedCity = null;
            rebuildWidgets();
        }));

        if (buildingFormOpen) { initBuildingForm(top); return; }

        boolean dataReady = selectedCity.equals(CityData.buildingsCity);

        if (selectedBuildingId != -1) {
            CityData.BuildingInfo b = findSelectedBuilding();
            if (b != null && b.canDelete()) {
                addRenderableWidget(FancyButton.of(Component.literal("Удалить"), right() - 70, top - 4, 70, 18, () -> { CityActions.deleteBuilding(selectedCity, selectedBuildingId); selectedBuildingId = -1; }));
            }
            return;
        }

        // Рейтинг: доступен только не-жителям этого города.
        if (dataReady && CityData.cardCanRate) {
            boolean liked = CityData.cardMyVote == 1;
            boolean disliked = CityData.cardMyVote == 2;
            addRenderableWidget(FancyButton.of(Component.literal(liked ? "✔ нравится" : "Нравится"), left(), top + CARD_TOOLBAR_TOP, 88, 16, () -> CityActions.rateCity(selectedCity, liked ? (byte) 0 : (byte) 1)));
            addRenderableWidget(FancyButton.of(Component.literal(disliked ? "✔ не нравится" : "Не нравится"), left() + 92, top + CARD_TOOLBAR_TOP, 100, 16, () -> CityActions.rateCity(selectedCity, disliked ? (byte) 0 : (byte) 2)));
        }

        // Подвкладки внутри карточки.
        addRenderableWidget(FancyButton.of(Component.literal("Постройки"), right() - 262, top + CARD_TOOLBAR_TOP, 80, 16, () -> { cardSubMode = CARD_SUB_BUILDINGS; rebuildWidgets(); }));
        addRenderableWidget(FancyButton.of(Component.literal("Комментарии"), right() - 178, top + CARD_TOOLBAR_TOP, 84, 16, () -> { cardSubMode = CARD_SUB_COMMENTS; rebuildWidgets(); }));
        addRenderableWidget(FancyButton.of(Component.literal("Законы"), right() - 90, top + CARD_TOOLBAR_TOP, 90, 16, () -> { cardSubMode = CARD_SUB_LAWS; rebuildWidgets(); }));

        if (cardSubMode == CARD_SUB_COMMENTS) { initComments(top); return; }
        if (cardSubMode == CARD_SUB_LAWS) { initLaws(top); return; }

        // Список построек: жителю своего города доступны рулетка и сохранение.
        if (selectedCity.equals(CityData.cityName)) {
            addRenderableWidget(FancyButton.of(Component.literal("Получить рулетку"), left(), this.height - 40, 130, 20, () -> {
                CityActions.requestBuildingWand();
                net.minecraft.client.Minecraft.getInstance().setScreen(null);
            }));
            addRenderableWidget(FancyButton.of(Component.literal("Сохранить постройку"), left() + 134, this.height - 40, 140, 20, () -> {
                buildingFormOpen = true;
                rebuildWidgets();
            }));
        }
        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), right() - 90, this.height - 40, 90, 20, () -> CityActions.requestBuildings(selectedCity)));
    }

    private void initComments(int top) {
        int y = top + BUILDING_LIST_TOP;
        if (CityData.cardCanRate) {
            commentInput = new EditBox(this.font, left(), y, right() - left() - 90, 20, Component.literal("Комментарий"));
            commentInput.setMaxLength(200);
            commentInput.setHint(Component.literal("Комментарий (до 200)"));
            commentInput.setValue(pendingComment);
            addRenderableWidget(commentInput);
            addRenderableWidget(FancyButton.of(Component.literal("Отправить"), right() - 84, y, 84, 20, () -> {
                String text = commentInput.getValue().trim();
                if (!text.isEmpty()) {
                    CityActions.addComment(selectedCity, text);
                    pendingComment = "";
                    commentInput.setValue("");
                }
            }));
            y += 26;
        }

        int listY = y;
        int shown = Math.min(5, CityData.cardComments.size());
        for (int i = 0; i < shown; i++) {
            CityData.CommentInfo c = CityData.cardComments.get(i);
            if (c.canDelete()) {
                addRenderableWidget(FancyButton.of(Component.literal("Удалить"), right() - 60, listY + 2, 60, 16, () -> CityActions.deleteComment(selectedCity, c.id())));
            }
            listY += COMMENT_ROW_H;
        }
    }

    private void initLaws(int top) {
        int y = top + BUILDING_LIST_TOP;
        boolean canEdit = selectedCity.equals(CityData.cityName) && CityData.cardCanEditLaws;
        if (canEdit) {
            lawInput = new EditBox(this.font, left(), y, right() - left() - 90, 20, Component.literal("Закон"));
            lawInput.setMaxLength(100);
            lawInput.setHint(Component.literal("Новый закон (до 100)"));
            lawInput.setValue(pendingLaw);
            addRenderableWidget(lawInput);
            addRenderableWidget(FancyButton.of(Component.literal("Вписать"), right() - 84, y, 84, 20, () -> {
                String text = lawInput.getValue().trim();
                if (!text.isEmpty()) {
                    CityActions.addLaw(text);
                    pendingLaw = "";
                    lawInput.setValue("");
                }
            }));
            y += 26;
        }

        int listY = y;
        int shown = Math.min(6, CityData.cardLaws.size());
        for (int i = 0; i < shown; i++) {
            CityData.LawInfo l = CityData.cardLaws.get(i);
            if (canEdit) {
                addRenderableWidget(FancyButton.of(Component.literal("Убрать"), right() - 56, listY + 1, 56, 16, () -> CityActions.deleteLaw(l.id())));
            }
            listY += LAW_ROW_H;
        }
    }

    /** Форма сохранения постройки — углы уже отмечены рулеткой (сервер проверит). */
    private void initBuildingForm(int top) {
        int y = top + 40;
        buildingNameInput = new EditBox(this.font, left(), y, right() - left(), 20, Component.literal("Название"));
        buildingNameInput.setMaxLength(32);
        buildingNameInput.setHint(Component.literal("Название постройки (до 32)"));
        buildingNameInput.setValue(pendingBuildingName);
        addRenderableWidget(buildingNameInput);
        y += 26;

        buildingDescInput = new EditBox(this.font, left(), y, right() - left(), 20, Component.literal("Описание"));
        buildingDescInput.setMaxLength(200);
        buildingDescInput.setHint(Component.literal("Описание (до 200 символов)"));
        buildingDescInput.setValue(pendingBuildingDesc);
        addRenderableWidget(buildingDescInput);
        y += 34;

        addRenderableWidget(FancyButton.of(Component.literal("Сделать фото (3 сек) и сохранить"), left(), y, right() - left(), 20, () -> {
            String name = buildingNameInput.getValue().trim();
            if (name.isEmpty()) return;
            BuildingPhotoTaker.start(name, buildingDescInput.getValue().trim());
            closeBuildingForm();
            net.minecraft.client.Minecraft.getInstance().setScreen(null);
        }));
        y += 24;

        addRenderableWidget(FancyButton.of(Component.literal("Сохранить без фото"), left(), y, right() - left(), 20, () -> {
            String name = buildingNameInput.getValue().trim();
            if (name.isEmpty()) return;
            CityActions.saveBuilding(name, buildingDescInput.getValue().trim(), "");
            closeBuildingForm();
            rebuildWidgets();
        }));
    }

    private void closeBuildingForm() {
        buildingFormOpen = false;
        pendingBuildingName = "";
        pendingBuildingDesc = "";
        buildingNameInput = null;
        buildingDescInput = null;
    }

    private CityData.BuildingInfo findSelectedBuilding() {
        if (selectedCity == null || !selectedCity.equals(CityData.buildingsCity)) return null;
        for (CityData.BuildingInfo b : CityData.buildings) {
            if (b.id() == selectedBuildingId) return b;
        }
        return null;
    }

    private void initTop(int top) {
        addRenderableWidget(FancyButton.of(Component.literal("Обновить топ"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestTop()));
    }

    /** Заказ контракта — только у кого есть город; выполнить чужой контракт можно и без города. */
    private void initContracts(int top) {
        if (CityData.hasCity) {
            int y = top + CONTRACT_FORM_TOP_MARGIN;
            y = initResourcePicker(y, "contractReq");
            y = initResourcePicker(y, "contractRew");

            addRenderableWidget(FancyButton.of(Component.literal("Заказать контракт"), left(), y, right() - left(), PICKER_BUTTON_H, () -> {
                        int reqAmt = pickerAmountValue("contractReq");
                        int rewAmt = pickerAmountValue("contractRew");
                        String reqMat = pickerMaterial.get("contractReq");
                        String rewMat = pickerMaterial.get("contractRew");
                        if (reqAmt > 0 && rewAmt > 0 && reqMat != null && rewMat != null) {
                            CityActions.createContract(reqMat, reqAmt, rewMat, rewAmt);
                        }
                    }));
        }

        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestContracts()));

        int y = contractsListTop(top);
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            CityData.ContractInfo c = CityData.contracts.get(i);
            addRenderableWidget(FancyButton.of(Component.literal("Взять"), right() - 55, y, 55, 18, () -> CityActions.takeContract(c.id())));
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
    private void initBounties(int top) {
        int nickY = top + BOUNTY_NICK_TOP;
        bountyNickInput = new EditBox(this.font, left(), nickY, right() - left(), 18, Component.literal("Ник цели"));
        bountyNickInput.setMaxLength(16);
        bountyNickInput.setHint(Component.literal("Ник цели"));
        bountyNickInput.setValue(pendingBountyNick);
        addRenderableWidget(bountyNickInput);

        int pickerY = nickY + BOUNTY_NICK_ROW_H;
        int afterPicker = initResourcePicker(pickerY, "bountyReward");

        addRenderableWidget(FancyButton.of(Component.literal("Заказать розыск"), left(), afterPicker, right() - left(), PICKER_BUTTON_H, () -> {
                    String nick = bountyNickInput.getValue().trim();
                    int amount = pickerAmountValue("bountyReward");
                    String material = pickerMaterial.get("bountyReward");
                    if (!nick.isEmpty() && amount > 0 && material != null) {
                        CityActions.createBounty(nick, material, amount);
                    }
                }));

        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestBounties()));

        int y = bountyListTop(top);
        int shown = Math.min(6, CityData.bounties.size());
        for (int i = 0; i < shown; i++) {
            CityData.BountyInfo b = CityData.bounties.get(i);
            if (!b.claimed()) {
                addRenderableWidget(FancyButton.of(Component.literal("Взять"), right() - 55, y, 55, 18, () -> CityActions.takeBounty(b.id())));
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

    /**
     * Рынок: выставить любой предмет из руки (цена — свободный текст, количество — отдельное поле),
     * без эскроу — предмет остаётся в инвентаре, сервер клонирует его в момент отправки формы.
     */
    private void initMarket(int top) {
        int priceY = top + MARKET_FORM_TOP_MARGIN;
        marketPriceInput = new EditBox(this.font, left(), priceY, right() - left() - 64, MARKET_ROW_H,
                Component.literal("Цена"));
        marketPriceInput.setMaxLength(64);
        marketPriceInput.setHint(Component.literal("Цена (текст)"));
        marketPriceInput.setValue(pendingMarketPrice);
        addRenderableWidget(marketPriceInput);

        marketQuantityInput = new EditBox(this.font, right() - 56, priceY, 56, MARKET_ROW_H,
                Component.literal("Кол-во"));
        marketQuantityInput.setMaxLength(3);
        marketQuantityInput.setValue(pendingMarketQuantity);
        addRenderableWidget(marketQuantityInput);

        int buttonY = priceY + MARKET_ROW_H + MARKET_GAP;
        addRenderableWidget(FancyButton.of(Component.literal("Выставить (предмет из руки)"), left(), buttonY, right() - left(), MARKET_ROW_H, () -> {
                    String price = marketPriceInput.getValue().trim();
                    int qty = parseAmount(marketQuantityInput.getValue());
                    if (!price.isEmpty() && qty > 0) CityActions.listItem(price, qty);
                }));

        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestMarket()));

        int y = marketListTop(top);
        int shown = Math.min(6, CityData.market.size());
        for (int i = 0; i < shown; i++) {
            CityData.MarketListingInfo l = CityData.market.get(i);
            if (l.mine()) {
                addRenderableWidget(FancyButton.of(Component.literal("Снять"), right() - 70, y, 70, 18, () -> CityActions.marketCancel(l.id())));
            } else {
                addRenderableWidget(FancyButton.of(Component.literal("Купить"), right() - 55, y, 55, 18, () -> CityActions.marketInterest(l.id())));
            }
            y += 20;
        }
    }

    private int marketListTop(int top) {
        return top + MARKET_FORM_TOP_MARGIN + MARKET_ROW_H + MARKET_GAP + MARKET_ROW_H + MARKET_GAP;
    }

    /**
     * Ивенты: любой игрок создаёт название/описание/дату-время, координаты берёт сервер
     * (текущая позиция игрока) — клиент их не присылает и не показывает поле ввода для них.
     * У каждого ивента в списке — кнопка «Участвую» (переключаемая, с числом участников) и,
     * для автора/опа, «Удалить», обе в правой колонке карточки, друг под другом.
     */
    private void initEvents(int top) {
        int y = top + EVENT_FORM_TOP_MARGIN;
        eventTitleInput = new EditBox(this.font, left(), y, right() - left(), EVENT_ROW_H2,
                Component.literal("Название"));
        eventTitleInput.setMaxLength(32);
        eventTitleInput.setHint(Component.literal("Название ивента (до 32)"));
        eventTitleInput.setValue(pendingEventTitle);
        addRenderableWidget(eventTitleInput);
        y += EVENT_ROW_H2 + EVENT_GAP;

        eventDescInput = new EditBox(this.font, left(), y, right() - left(), EVENT_ROW_H2,
                Component.literal("Описание"));
        eventDescInput.setMaxLength(200);
        eventDescInput.setHint(Component.literal("Описание (необязательно, до 200)"));
        eventDescInput.setValue(pendingEventDesc);
        addRenderableWidget(eventDescInput);
        y += EVENT_ROW_H2 + EVENT_GAP;

        eventDateInput = new EditBox(this.font, left(), y, right() - left(), EVENT_ROW_H2,
                Component.literal("Дата и время"));
        eventDateInput.setMaxLength(40);
        eventDateInput.setHint(Component.literal("Дата и время, например «18 июля, 20:00»"));
        eventDateInput.setValue(pendingEventDate);
        addRenderableWidget(eventDateInput);
        y += EVENT_ROW_H2 + EVENT_GAP;

        addRenderableWidget(FancyButton.of(Component.literal("Создать ивент здесь, где стою"), left(), y, right() - left(), EVENT_ROW_H2, () -> {
                    String title = eventTitleInput.getValue().trim();
                    String dateTime = eventDateInput.getValue().trim();
                    if (!title.isEmpty() && !dateTime.isEmpty()) {
                        CityActions.createEvent(title, eventDescInput.getValue().trim(), dateTime);
                    }
                }));

        addRenderableWidget(FancyButton.of(Component.literal("Обновить"), cx() - 60, this.height - 40, 120, 20, () -> CityActions.requestEvents()));

        int ly = eventsListTop(top);
        int shown = Math.min(6, CityData.events.size());
        for (int i = 0; i < shown; i++) {
            CityData.EventInfo e = CityData.events.get(i);
            String partLabel = (e.participating() ? "✔ " : "") + "Участвую (" + e.participants() + ")";
            addRenderableWidget(FancyButton.of(Component.literal(partLabel), right() - EVENT_BTN_W, ly + 3, EVENT_BTN_W, 18, () -> CityActions.toggleEventParticipate(e.id())));
            if (e.canDelete()) {
                addRenderableWidget(FancyButton.of(Component.literal("Удалить"), right() - EVENT_BTN_W, ly + 25, EVENT_BTN_W, 16, () -> CityActions.deleteEvent(e.id())));
            }
            ly += EVENT_ROW_H;
        }
    }

    private int eventsListTop(int top) {
        return top + EVENT_FORM_TOP_MARGIN + (EVENT_ROW_H2 + EVENT_GAP) * 4;
    }

    private void bgEvents(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        int y = eventsListTop(top);
        int shown = Math.min(6, CityData.events.size());
        for (int i = 0; i < shown; i++) {
            boolean hover = mouseX >= left() - 6 && mouseX < right() + 6
                    && mouseY >= y && mouseY < y + EVENT_ROW_H - 4;
            g.fill(left() - 6, y, right() + 6, y + EVENT_ROW_H - 4, hover ? ROW_HOVER : CARD);
            g.fill(left() - 6, y, left() - 4, y + EVENT_ROW_H - 4, GOLD);
            y += EVENT_ROW_H;
        }
    }

    private void renderEvents(GuiGraphicsExtractor g, int top) {
        g.text(this.font, "СОЗДАТЬ ИВЕНТ — координаты берутся там, где ты сейчас стоишь", left(), top, DIM);

        int y = eventsListTop(top);
        if (CityData.events.isEmpty()) {
            g.text(this.font, "Ивентов пока нет — стань первым", left(), y + 4, DIM);
            return;
        }
        int shown = Math.min(6, CityData.events.size());
        int maxW = right() - left() - EVENT_BTN_W - 8;
        for (int i = 0; i < shown; i++) {
            CityData.EventInfo e = CityData.events.get(i);

            clipText(g, e.title(), left() + 4, y + 3, maxW, GOLD_BRIGHT);

            String desc = e.description().isEmpty() ? "без описания" : e.description();
            clipText(g, desc, left() + 4, y + 15, maxW, e.description().isEmpty() ? DIM : WHITE);

            String coords = e.world() + " " + e.x() + "/" + e.y() + "/" + e.z();
            int coordsW = this.font.width(coords);
            String who = "от " + e.creatorName() + (e.creatorCity().isEmpty() ? "" : " (" + e.creatorCity() + ")");
            clipText(g, who, left() + 4, y + 27, maxW - coordsW - 6, GRAY);
            rightText(g, coords, left() + 4 + maxW, y + 27, BLUE);

            int nx = seg(g, left() + 4, y + 39, "Когда: ", DIM);
            clipText(g, e.dateTime(), nx, y + 39, maxW - (nx - left() - 4), GOLD_BRIGHT);

            y += EVENT_ROW_H;
        }
        if (CityData.events.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.events.size() - shown) + " ивентов", left() + 4, y + 2, DIM);
        }
    }


    // ── Универсальный поиск-пикер ресурса ──────────────────────────────────

    /** Строка поиска ресурса + количество. Совпадения рисуются/кликаются отдельно (см. renderPickerMatches/handlePickerClick) — вживую, по текущему тексту поля, без пересборки виджетов на каждую нажатую клавишу. */
    private int initResourcePicker(int y, String key) {
        int rowY = y + 12; // верхние 12px — подпись с текущим выбором, рисуется в renderPickerLabel
        EditBox search = new EditBox(this.font, left(), rowY, 170, 18, Component.literal("Поиск ресурса"));
        search.setMaxLength(30);
        search.setHint(Component.literal("Поиск ресурса…"));
        search.setValue(pendingPickerQuery.getOrDefault(key, ""));
        pickerSearch.put(key, search);
        addRenderableWidget(search);

        EditBox amount = new EditBox(this.font, left() + 176, rowY, 48, 18, Component.literal("Кол-во"));
        amount.setMaxLength(4);
        amount.setValue(pendingPickerAmount.getOrDefault(key, "1"));
        pickerAmount.put(key, amount);
        addRenderableWidget(amount);

        return y + PICKER_BLOCK_H;
    }

    private record MatchRect(Catalogs.Resource resource, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** Совпадения поиска для пикера — читает EditBox ЖИВЬЁМ (getValue()), поэтому обновляется каждый кадр без rebuildWidgets(). Используется и рендером, и обработчиком клика — одна и та же геометрия. */
    private List<MatchRect> pickerMatchRects(int blockY, String key) {
        EditBox box = pickerSearch.get(key);
        List<MatchRect> out = new ArrayList<>();
        if (box == null) return out;
        int matchY = blockY + 12 + 20;
        for (Catalogs.Resource r : Catalogs.search(box.getValue(), PICKER_MATCH_ROWS)) {
            out.add(new MatchRect(r, left(), matchY, right() - left(), 16));
            matchY += 16;
        }
        return out;
    }

    private void bgPickerMatches(GuiGraphicsExtractor g, int blockY, String key, int mouseX, int mouseY) {
        String selected = pickerMaterial.get(key);
        for (MatchRect m : pickerMatchRects(blockY, key)) {
            boolean isSelected = m.resource().id().equals(selected);
            int bg = isSelected ? SELECT_BG : m.contains(mouseX, mouseY) ? ROW_HOVER : ROW_A;
            g.fill(m.x(), m.y(), m.x() + m.w(), m.y() + m.h() - 1, bg);
        }
    }

    private void renderPickerLabel(GuiGraphicsExtractor g, int y, String label, String key) {
        String materialId = pickerMaterial.get(key);
        int x = seg(g, left(), y, label + ":  ", DIM);
        if (materialId == null) {
            g.text(this.font, "не выбран", x, y, RED);
        } else {
            g.text(this.font, Catalogs.resourceName(materialId), x, y, GREEN);
        }
    }

    /** Тексты совпадений поиска (фоновые подложки — в bgPickerMatches). */
    private void renderPickerMatches(GuiGraphicsExtractor g, int blockY, String key) {
        String selected = pickerMaterial.get(key);
        for (MatchRect m : pickerMatchRects(blockY, key)) {
            boolean isSelected = m.resource().id().equals(selected);
            g.text(this.font, m.resource().displayName(), m.x() + 6, m.y() + 4,
                    isSelected ? GREEN : WHITE);
        }
    }

    private boolean handlePickerClick(double mouseX, double mouseY, int blockY, String key) {
        for (MatchRect m : pickerMatchRects(blockY, key)) {
            if (m.contains(mouseX, mouseY)) {
                FancyButton.uiClick();
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
        for (TabRect t : tabRects()) {
            if (t.contains(event.x(), event.y())) {
                if (mode != t.index()) {
                    FancyButton.uiClick();
                    switchMode(t.index());
                }
                return true;
            }
        }
        if (!CityData.protocolMismatch) {
            if (mode == MODE_CONTRACTS && CityData.hasCity) {
                int y = CONTENT_TOP + CONTRACT_FORM_TOP_MARGIN;
                if (handlePickerClick(event.x(), event.y(), y, "contractReq")) return true;
                y += PICKER_BLOCK_H;
                if (handlePickerClick(event.x(), event.y(), y, "contractRew")) return true;
            } else if (mode == MODE_BOUNTIES) {
                int y = CONTENT_TOP + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
                if (handlePickerClick(event.x(), event.y(), y, "bountyReward")) return true;
            } else if (mode == MODE_DIRECTORY && !buildingFormOpen) {
                if (handleDirectoryClick(event.x(), event.y())) return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Клики по строкам: город в справочнике → карточка; постройка в карточке → детали. */
    private boolean handleDirectoryClick(double mx, double my) {
        if (selectedCity == null) {
            int y = CONTENT_TOP;
            int shown = Math.min(6, CityData.directory.size());
            for (int i = 0; i < shown; i++) {
                CityData.CityInfo c = CityData.directory.get(i);
                // Правый край строки не считаем — там живёт кнопка «Вступить».
                if (mx >= left() - 6 && mx < right() - 80 && my >= y - 5 && my < y + 21) {
                    FancyButton.uiClick();
                    selectedCity = c.name();
                    selectedBuildingId = -1;
                    cardSubMode = CARD_SUB_BUILDINGS;
                    CityActions.requestBuildings(c.name());
                    rebuildWidgets();
                    return true;
                }
                y += 28;
            }
        } else if (selectedBuildingId == -1 && cardSubMode == CARD_SUB_BUILDINGS
                && selectedCity.equals(CityData.buildingsCity)) {
            int y = CONTENT_TOP + BUILDING_LIST_TOP;
            int shown = Math.min(5, CityData.buildings.size());
            for (int i = 0; i < shown; i++) {
                CityData.BuildingInfo b = CityData.buildings.get(i);
                if (mx >= left() - 6 && mx < right() + 6 && my >= y && my < y + BUILDING_ROW_H - 4) {
                    FancyButton.uiClick();
                    selectedBuildingId = b.id();
                    rebuildWidgets();
                    return true;
                }
                y += BUILDING_ROW_H;
            }
        }
        return false;
    }

    // ── Хелперы отрисовки ────────────────────────────────────────────────────

    /** Печатает сегмент текста и возвращает x сразу за ним — для «раскрашенных» строк без §-кодов. */
    private int seg(GuiGraphicsExtractor g, int x, int y, String s, int color) {
        g.text(this.font, s, x, y, color);
        return x + this.font.width(s);
    }

    /** Текст, выровненный по правому краю. */
    private void rightText(GuiGraphicsExtractor g, String s, int rightX, int y, int color) {
        g.text(this.font, s, rightX - this.font.width(s), y, color);
    }

    /** Чип-«пилюля»: цветная подложка с текстом. Возвращает x за чипом. */
    private int chip(GuiGraphicsExtractor g, int x, int y, String s, int bg, int fg) {
        int w = this.font.width(s);
        g.fill(x, y - 2, x + w + 8, y + 10, bg);
        g.text(this.font, s, x + 4, y, fg);
        return x + w + 8;
    }

    /** Текст с масштабом (для крупных заголовков/цифр). */
    private void scaledText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color, boolean centered) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        if (centered) g.centeredText(this.font, s, 0, 0, color);
        else g.text(this.font, s, 0, 0, color);
        pose.popMatrix();
    }

    // ── Фон: панель, вкладки, карточки (рисуется ПОД виджетами) ─────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        // Стеклянная панель
        g.fillGradient(px1(), PANEL_Y1, px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), PANEL_Y1, px2() - px1(), py2() - PANEL_Y1, PANEL_EDGE);
        // Шапка панели чуть темнее + золотой разделитель под вкладками
        g.fill(px1() + 1, PANEL_Y1 + 1, px2() - 1, TAB_Y + TAB_H + 3, 0x30000000);
        g.horizontalLine(px1() + 1, px2() - 2, TAB_Y + TAB_H + 3, GOLD_LINE);

        // Вкладки
        for (TabRect t : tabRects()) {
            boolean active = t.index() == mode;
            boolean hover = t.contains(mouseX, mouseY);
            if (active) {
                g.fill(t.x(), t.y(), t.x() + t.w(), t.y() + t.h(), TAB_ACTIVE_BG);
                g.fill(t.x(), t.y() + t.h() - 2, t.x() + t.w(), t.y() + t.h(), GOLD);
            } else if (hover) {
                g.fill(t.x(), t.y(), t.x() + t.w(), t.y() + t.h(), ROW_HOVER);
            }
        }

        if (CityData.protocolMismatch) {
            bgProtocolMismatch(g);
            return;
        }

        int top = CONTENT_TOP;
        if (mode == MODE_TOP) {
            bgTop(g, top);
        } else if (mode == MODE_DIRECTORY) {
            bgDirectory(g, top, mouseX, mouseY);
        } else if (mode == MODE_CONTRACTS) {
            bgContracts(g, top, mouseX, mouseY);
        } else if (mode == MODE_BOUNTIES) {
            bgBounties(g, top, mouseX, mouseY);
        } else if (mode == MODE_MARKET) {
            bgMarket(g, top);
        } else if (mode == MODE_EVENTS) {
            bgEvents(g, top, mouseX, mouseY);
        } else if (!CityData.hasCity) {
            bgNoCity(g, top);
        } else if (mode == MODE_ECONOMY) {
            bgEconomy(g, top);
        } else {
            bgCity(g, top, mouseX, mouseY);
        }

        // Подложка тоста результата
        if (!CityData.lastResult.isEmpty()) {
            int w = this.font.width(CityData.lastResult) + 26;
            int x1 = cx() - w / 2, y1 = this.height - 65, y2 = this.height - 49;
            g.fill(x1, y1, x1 + w, y2, 0xE60D0E12);
            g.fill(x1, y1, x1 + 3, y2, CityData.lastOk ? GREEN : RED);
        }
    }

    private void bgProtocolMismatch(GuiGraphicsExtractor g) {
        int midY = this.height / 2;
        g.fill(cx() - 150, midY - 40, cx() + 150, midY + 34, 0xF00E0F13);
        g.outline(cx() - 150, midY - 40, 300, 74, 0x66EB7069);
    }

    private void bgNoCity(GuiGraphicsExtractor g, int top) {
        g.fill(cx() - 130, top + 2, cx() + 130, top + 102, CARD);
        g.outline(cx() - 130, top + 2, 260, 100, PANEL_EDGE);
    }

    private void bgCity(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        g.horizontalLine(left(), right(), top + 34, LINE);
        int y = top + 44;
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            boolean hover = mouseX >= left() - 6 && mouseX < right() + 6 && mouseY >= y - 3 && mouseY < y + 14;
            if (hover) g.fill(left() - 6, y - 3, right() + 6, y + 14, ROW_HOVER);
            else if (i % 2 == 0) g.fill(left() - 6, y - 3, right() + 6, y + 14, ROW_A);
            y += 18;
        }
    }

    private void bgEconomy(GuiGraphicsExtractor g, int top) {
        int y = top + 30;
        for (int i = 0; i < Catalogs.BUFFS.size(); i++) {
            if (i % 2 == 0) g.fill(left() - 6, y - 3, right() + 6, y + 14, ROW_A);
            y += 18;
        }
        // Карточка «в сундуках»
        int ry = this.height - 170;
        g.fill(left() - 6, ry - 6, right() + 6, this.height - 100, CARD);
    }

    private void bgDirectory(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        if (selectedCity != null) { bgCityCard(g, top, mouseX, mouseY); return; }
        int y = top;
        int shown = Math.min(6, CityData.directory.size());
        for (int i = 0; i < shown; i++) {
            CityData.CityInfo c = CityData.directory.get(i);
            boolean hover = mouseX >= left() - 6 && mouseX < right() + 6 && mouseY >= y - 5 && mouseY < y + 21;
            g.fill(left() - 6, y - 5, right() + 6, y + 21, hover ? ROW_HOVER : CARD);
            g.fill(left() - 6, y - 5, left() - 4, y + 21, c.open() ? GREEN : RED);
            y += 28;
        }
    }

    private void bgCityCard(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        g.horizontalLine(left(), right(), top + BUILDING_LIST_TOP - 8, LINE);
        if (buildingFormOpen) return;

        if (selectedBuildingId != -1) { bgBuildingDetail(g, top); return; }

        if (cardSubMode == CARD_SUB_COMMENTS) { bgComments(g, top, mouseX, mouseY); return; }
        if (cardSubMode == CARD_SUB_LAWS) { bgLaws(g, top); return; }

        int y = top + BUILDING_LIST_TOP;
        int shown = Math.min(5, CityData.buildings.size());
        for (int i = 0; i < shown; i++) {
            CityData.BuildingInfo b = CityData.buildings.get(i);
            boolean hover = mouseX >= left() - 6 && mouseX < right() + 6
                    && mouseY >= y && mouseY < y + BUILDING_ROW_H - 4;
            g.fill(left() - 6, y, right() + 6, y + BUILDING_ROW_H - 4, hover ? ROW_HOVER : CARD);
            g.fill(left() - 6, y, left() - 4, y + BUILDING_ROW_H - 4, GOLD);
            Identifier tex = BuildingPhotos.get(b.photoId());
            if (tex != null) {
                int[] sz = BuildingPhotos.size(b.photoId());
                g.blit(RenderPipelines.GUI_TEXTURED, tex, left(), y + 2, 0f, 0f,
                        BUILDING_THUMB_W, BUILDING_THUMB_H, sz[0], sz[1], sz[0], sz[1]);
            } else {
                g.fill(left(), y + 2, left() + BUILDING_THUMB_W, y + 2 + BUILDING_THUMB_H, 0x30000000);
            }
            y += BUILDING_ROW_H;
        }
    }

    private void bgComments(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        int y = top + BUILDING_LIST_TOP + (CityData.cardCanRate ? 26 : 0);
        int shown = Math.min(5, CityData.cardComments.size());
        for (int i = 0; i < shown; i++) {
            boolean hover = mouseX >= left() - 6 && mouseX < right() + 6
                    && mouseY >= y && mouseY < y + COMMENT_ROW_H - 4;
            g.fill(left() - 6, y, right() + 6, y + COMMENT_ROW_H - 4, hover ? ROW_HOVER : CARD);
            g.fill(left() - 6, y, left() - 4, y + COMMENT_ROW_H - 4, BLUE);
            y += COMMENT_ROW_H;
        }
    }

    private void bgLaws(GuiGraphicsExtractor g, int top) {
        boolean canEdit = selectedCity.equals(CityData.cityName) && CityData.cardCanEditLaws;
        int y = top + BUILDING_LIST_TOP + (canEdit ? 26 : 0);
        int shown = Math.min(6, CityData.cardLaws.size());
        for (int i = 0; i < shown; i++) {
            g.fill(left() - 6, y, right() + 6, y + LAW_ROW_H - 4, CARD);
            g.fill(left() - 6, y, left() - 4, y + LAW_ROW_H - 4, GOLD);
            y += LAW_ROW_H;
        }
    }

    private void bgBuildingDetail(GuiGraphicsExtractor g, int top) {
        CityData.BuildingInfo b = findSelectedBuilding();
        if (b == null) return;
        // Крупное фото по центру (или тёмная заглушка, пока качается / если его нет)
        int pw = Math.min(320, right() - left());
        int ph = pw * 9 / 16;
        int px = cx() - pw / 2, py = top + BUILDING_LIST_TOP;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, CARD);
        Identifier tex = BuildingPhotos.get(b.photoId());
        if (tex != null) {
            int[] sz = BuildingPhotos.size(b.photoId());
            g.blit(RenderPipelines.GUI_TEXTURED, tex, px, py, 0f, 0f, pw, ph, sz[0], sz[1], sz[0], sz[1]);
        }
    }

    private void bgTop(GuiGraphicsExtractor g, int top) {
        if (CityData.top.isEmpty()) return;
        long max = 1;
        for (CityData.TopEntry e : CityData.top) max = Math.max(max, e.score());
        int y = top;
        for (CityData.TopEntry e : CityData.top) {
            int barW = (int) ((right() - left() - 20) * e.score() / (double) max);
            g.fill(left() + 14, y + 11, left() + 14 + Math.max(barW, 2), y + 13, GOLD_LINE);
            y += 18;
        }
    }

    private void bgContracts(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        if (CityData.hasCity) {
            int y = top + CONTRACT_FORM_TOP_MARGIN;
            bgPickerMatches(g, y, "contractReq", mouseX, mouseY);
            bgPickerMatches(g, y + PICKER_BLOCK_H, "contractRew", mouseX, mouseY);
        }
        int y = contractsListTop(top);
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            g.fill(left() - 6, y - 1, right() + 6, y + 18, CARD);
            g.fill(left() - 6, y - 1, left() - 4, y + 18, GOLD);
            y += 20;
        }
    }

    private void bgBounties(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        int pickerY = top + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
        bgPickerMatches(g, pickerY, "bountyReward", mouseX, mouseY);

        int y = bountyListTop(top);
        if (CityData.myHunt != null) {
            int hy = y - MY_HUNT_BLOCK_H;
            g.fill(left() - 6, hy - 5, right() + 6, hy + 33, CARD);
            g.outline(left() - 6, hy - 5, right() - left() + 12, 38, GOLD_LINE);
        }
        int shown = Math.min(6, CityData.bounties.size());
        for (int i = 0; i < shown; i++) {
            CityData.BountyInfo b = CityData.bounties.get(i);
            g.fill(left() - 6, y - 1, right() + 6, y + 18, CARD);
            g.fill(left() - 6, y - 1, left() - 4, y + 18, b.claimed() ? RED : GREEN);
            y += 20;
        }
    }

    private void bgMarket(GuiGraphicsExtractor g, int top) {
        int y = marketListTop(top);
        int shown = Math.min(6, CityData.market.size());
        for (int i = 0; i < shown; i++) {
            g.fill(left() - 6, y - 1, right() + 6, y + 18, CARD);
            g.fill(left() - 6, y - 1, left() - 4, y + 18, GOLD);
            y += 20;
        }
    }

    // ── Передний план: тексты (рисуется ПОВЕРХ виджетов) ────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hoverMouseX = mouseX; hoverMouseY = mouseY; hoverTip = null;

        // Заголовок над панелью
        scaledText(g, "ГОРОДА DOMINO CRAFT", cx(), 9, 1.15f, GOLD, true);
        int tw = (int) (this.font.width("ГОРОДА DOMINO CRAFT") * 1.15f / 2) + 10;
        g.text(this.font, "◆", cx() - tw - 8, 10, GOLD_LINE);
        g.text(this.font, "◆", cx() + tw + 2, 10, GOLD_LINE);

        // Подписи вкладок
        for (TabRect t : tabRects()) {
            boolean active = t.index() == mode;
            boolean hover = t.contains(mouseX, mouseY);
            int color = active ? GOLD_BRIGHT : hover ? WHITE : GRAY;
            g.centeredText(this.font, TAB_LABELS[t.index()], t.x() + t.w() / 2, t.y() + 7, color);
        }

        if (CityData.protocolMismatch) {
            int midY = this.height / 2;
            scaledText(g, "⚠", cx(), midY - 30, 1.6f, RED, true);
            g.centeredText(this.font, "Версия мода не совпадает с сервером", cx(), midY - 8, WHITE);
            g.centeredText(this.font, "Перезапусти лаунчер Domino Craft — он обновит сборку сам", cx(), midY + 6, GRAY);
            g.centeredText(this.font, "клиент v" + Protocol.VERSION + ", получено v" + CityData.lastReceivedVersion,
                    cx(), midY + 20, DIM);
            return;
        }

        int top = CONTENT_TOP;
        if (mode == MODE_TOP) {
            renderTop(g, top);
        } else if (mode == MODE_DIRECTORY) {
            renderDirectory(g, top);
        } else if (mode == MODE_CONTRACTS) {
            renderContracts(g, top);
        } else if (mode == MODE_BOUNTIES) {
            renderBounties(g, top);
        } else if (mode == MODE_MARKET) {
            renderMarket(g, top, mouseX, mouseY);
        } else if (mode == MODE_EVENTS) {
            renderEvents(g, top);
        } else if (!CityData.hasCity) {
            g.centeredText(this.font, "У тебя пока нет города", cx(), top + 14, WHITE);
            g.centeredText(this.font, "Оснуй свой — прямо там, где стоишь", cx(), top + 28, GRAY);
        } else if (mode == MODE_ECONOMY) {
            renderEconomy(g, top);
        } else {
            renderCity(g, top);
        }

        // Тост результата последнего действия
        if (!CityData.lastResult.isEmpty()) {
            g.centeredText(this.font, CityData.lastResult, cx() + 2, this.height - 61,
                    CityData.lastOk ? WHITE : RED);
        }

        // Подсказка с полным текстом поверх всего (если курсор над обрезанной строкой).
        if (hoverTip != null) drawWrapTooltip(g, hoverTip);
    }

    // ── Текст: перенос по словам, укорачивание, подсказка с полным текстом ────

    private String ellipsize(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        while (!s.isEmpty() && this.font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Разбивает строку по словам на строки шириной ≤ maxW, не более maxLines (последняя с «…» при переполнении). */
    private List<String> wrapLines(String text, int maxW, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        StringBuilder line = new StringBuilder();
        String[] words = text.split(" ");
        for (int w = 0; w < words.length; w++) {
            String probe = line.length() == 0 ? words[w] : line + " " + words[w];
            if (this.font.width(probe) > maxW && line.length() > 0) {
                out.add(line.toString());
                if (out.size() == maxLines - 1) {
                    StringBuilder last = new StringBuilder(words[w]);
                    for (int k = w + 1; k < words.length; k++) last.append(' ').append(words[k]);
                    out.add(ellipsize(last.toString(), maxW));
                    return out;
                }
                line = new StringBuilder(words[w]);
            } else {
                line = new StringBuilder(probe);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    /**
     * Рисует строку, укороченную до maxW. Если она реально обрезана и курсор над ней —
     * запоминает полный текст для подсказки (задача: длинные описания/поля видны целиком).
     */
    private void clipText(GuiGraphicsExtractor g, String full, int x, int y, int maxW, int color) {
        String shown = ellipsize(full, maxW);
        g.text(this.font, shown, x, y, color);
        if (!shown.equals(full) && hoverMouseX >= x && hoverMouseX <= x + maxW
                && hoverMouseY >= y - 1 && hoverMouseY <= y + 9) {
            hoverTip = full;
        }
    }

    private void drawWrapTooltip(GuiGraphicsExtractor g, String text) {
        int w = 220;
        List<String> lines = wrapLines(text, w - 12, 14);
        int h = lines.size() * 11 + 10;
        int x = Math.min(hoverMouseX + 12, this.width - w - 4);
        int y = Math.min(hoverMouseY + 8, this.height - h - 4);
        if (x < 2) x = 2;
        if (y < 2) y = 2;
        g.fill(x, y, x + w, y + h, 0xF0101216);
        g.outline(x, y, w, h, GOLD_LINE);
        int ty = y + 5;
        for (String line : lines) {
            g.text(this.font, line, x + 6, ty, WHITE);
            ty += 11;
        }
    }

    private void renderCity(GuiGraphicsExtractor g, int top) {
        // Шапка: имя города крупно + чип статуса, ниже — сводка
        scaledText(g, CityData.cityName, left(), top - 2, 1.3f, GOLD_BRIGHT, false);
        int nameEnd = left() + (int) (this.font.width(CityData.cityName) * 1.3f) + 8;
        if (CityData.open) chip(g, nameEnd, top + 1, "открытый", CHIP_GREEN_BG, GREEN);
        else chip(g, nameEnd, top + 1, "закрытый", CHIP_RED_BG, RED);

        int x = left();
        int sy = top + 18;
        x = seg(g, x, sy, CityData.mayorTitle + " ", DIM);
        x = seg(g, x, sy, CityData.mayorName, WHITE);
        x = seg(g, x, sy, "   Рейтинг ", DIM);
        x = seg(g, x, sy, String.valueOf(CityData.score), GOLD);
        x = seg(g, x, sy, "   Радиус ", DIM);
        x = seg(g, x, sy, String.valueOf(CityData.radius), GOLD);
        x = seg(g, x, sy, "   Жителей ", DIM);
        seg(g, x, sy, String.valueOf(CityData.members.size()), GOLD);

        String desc = CityData.description.isEmpty() ? "Без описания" : CityData.description;
        int descColor = CityData.description.isEmpty() ? DIM : GRAY;
        clipText(g, desc, left(), top + 30, right() - left(), descColor);

        int y = top + 44;
        int shown = Math.min(8, CityData.members.size());
        for (int i = 0; i < shown; i++) {
            CityData.Member m = CityData.members.get(i);
            String mark = m.isMayor() ? "★" : m.isOfficer() ? "◆" : "•";
            int markColor = m.isMayor() ? GOLD : m.isOfficer() ? BLUE : DIM;
            int nx = seg(g, left() + 2, y + 1, mark + " ", markColor);
            nx = seg(g, nx, y + 1, m.name(), WHITE);
            if (m.isMayor()) seg(g, nx + 6, y + 1, CityData.mayorTitle, GOLD);
            else if (m.isOfficer()) seg(g, nx + 6, y + 1, CityData.officerTitle, BLUE);
            y += 18;
        }
        if (CityData.members.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.members.size() - shown), left() + 2, y, DIM);
        }
    }

    private void renderEconomy(GuiGraphicsExtractor g, int top) {
        g.text(this.font, "КАЗНА ГОРОДА", left(), top - 2, DIM);
        int px = left();
        scaledText(g, String.valueOf(CityData.points), px, top + 9, 1.5f, GOLD_BRIGHT, false);
        px += (int) (this.font.width(String.valueOf(CityData.points)) * 1.5f) + 6;
        g.text(this.font, "очков  ·  баффы жителям навсегда, платит мэр", px, top + 15, DIM);

        int y = top + 30;
        for (Catalogs.Buff b : Catalogs.BUFFS) {
            boolean bought = CityData.buffs.contains(b.id());
            int nx = seg(g, left() + 2, y + 1, b.displayName(), WHITE);
            seg(g, nx + 8, y + 1, b.cost() + " очков", bought ? DIM : GOLD);
            if (bought) rightText(g, "✔ куплено", right() - 4, y + 1, GREEN);
            y += 18;
        }

        // Сундуки — фиксированная зона над кнопкой "Показать сундуки" (height-90).
        int ry = this.height - 170;
        g.text(this.font, "В СУНДУКАХ ГОРОДА", left(), ry, DIM);
        ry += 14;
        if (CityData.resources.isEmpty()) {
            g.text(this.font, "нажми «Показать сундуки» — посчитаю по загруженным чанкам", left(), ry, GRAY);
        } else {
            int shownRes = Math.min(4, CityData.resources.size());
            for (int i = 0; i < shownRes; i++) {
                CityData.ResourceEntry e = CityData.resources.get(i);
                g.text(this.font, Catalogs.resourceName(e.material()), left(), ry, WHITE);
                rightText(g, String.valueOf(e.count()), right() - 4, ry, GOLD);
                ry += 12;
            }
            if (CityData.resources.size() > shownRes) {
                g.text(this.font, "… ещё " + (CityData.resources.size() - shownRes) + " видов", left(), ry, DIM);
            }
        }
    }

    private void renderDirectory(GuiGraphicsExtractor g, int top) {
        if (selectedCity != null) { renderCityCard(g, top); return; }
        if (CityData.directory.isEmpty()) {
            g.centeredText(this.font, "Городов пока нет", cx(), top + 12, GRAY);
            return;
        }
        int y = top;
        int shown = Math.min(6, CityData.directory.size());
        for (int i = 0; i < shown; i++) {
            CityData.CityInfo c = CityData.directory.get(i);
            int nx = seg(g, left() + 4, y, c.name(), GOLD_BRIGHT);
            nx = c.open() ? chip(g, nx + 8, y, "открыт", CHIP_GREEN_BG, GREEN)
                          : chip(g, nx + 8, y, "закрыт", CHIP_RED_BG, RED);
            seg(g, nx + 8, y, "мэр " + c.mayor(), GRAY);
            String members = String.join(", ", c.memberNames());
            if (members.length() > 60) members = members.substring(0, 60) + "…";
            g.text(this.font, members, left() + 4, y + 12, DIM);
            y += 28;
        }
        if (CityData.directory.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.directory.size() - shown) + " городов", left() + 4, y, DIM);
        } else {
            g.text(this.font, "клик по городу — карточка с постройками", left() + 4, y + 2, DIM);
        }
    }

    private void renderCityCard(GuiGraphicsExtractor g, int top) {
        scaledText(g, selectedCity, cx(), top - 2, 1.2f, GOLD_BRIGHT, true);
        CityData.CityInfo info = null;
        for (CityData.CityInfo c : CityData.directory) if (c.name().equals(selectedCity)) info = c;
        String sub = info != null ? "мэр " + info.mayor() + "  ·  жителей " + info.memberNames().size() : "";
        g.centeredText(this.font, sub, cx(), top + 16, DIM);

        boolean dataReady = selectedCity.equals(CityData.buildingsCity);
        if (dataReady) {
            String desc = CityData.cardDescription.isEmpty() ? "Без описания" : CityData.cardDescription;
            int descColor = CityData.cardDescription.isEmpty() ? DIM : GRAY;
            clipText(g, desc, left(), top + CARD_INFO_TOP, right() - left(), descColor);
        }

        if (buildingFormOpen) {
            renderBuildingForm(g, top);
            return;
        }

        if (selectedBuildingId != -1) {
            renderBuildingDetail(g, top);
            return;
        }

        if (dataReady) {
            String rating = "+" + CityData.cardLikes + "  −" + CityData.cardDislikes;
            int rx = CityData.cardCanRate ? left() + 196 : left();
            g.text(this.font, rating, rx, top + CARD_TOOLBAR_TOP + 3, GOLD);
        }

        if (cardSubMode == CARD_SUB_COMMENTS) {
            renderComments(g, top, dataReady);
            return;
        }
        if (cardSubMode == CARD_SUB_LAWS) {
            renderLaws(g, top, dataReady);
            return;
        }

        int y = top + BUILDING_LIST_TOP;
        if (!dataReady) {
            g.centeredText(this.font, "Загрузка…", cx(), y + 8, GRAY);
            return;
        }
        if (CityData.buildings.isEmpty()) {
            g.centeredText(this.font, "Построек пока нет", cx(), y + 8, GRAY);
            if (selectedCity.equals(CityData.cityName)) {
                g.centeredText(this.font, "Возьми рулетку, отметь два угла и сохрани первую!", cx(), y + 22, DIM);
            }
            return;
        }
        int shown = Math.min(5, CityData.buildings.size());
        for (int i = 0; i < shown; i++) {
            CityData.BuildingInfo b = CityData.buildings.get(i);
            int tx = left() + BUILDING_THUMB_W + 8;
            int nx = seg(g, tx, y + 4, b.name(), WHITE);
            seg(g, nx + 8, y + 4, BUILDING_DATE.format(new Date(b.createdAt())), DIM);
            g.text(this.font, "построил " + b.ownerName(), tx, y + 16, GRAY);
            if (!b.description().isEmpty()) {
                clipText(g, b.description(), tx, y + 27, right() - tx - 8, DIM);
            }
            y += BUILDING_ROW_H;
        }
        if (CityData.buildings.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.buildings.size() - shown) + " построек", left() + 4, y + 2, DIM);
        }
    }

    private void renderComments(GuiGraphicsExtractor g, int top, boolean dataReady) {
        int y = top + BUILDING_LIST_TOP + (CityData.cardCanRate ? 26 : 0);
        if (!dataReady) {
            g.centeredText(this.font, "Загрузка…", cx(), y + 8, GRAY);
            return;
        }
        if (CityData.cardComments.isEmpty()) {
            g.centeredText(this.font, "Комментариев пока нет", cx(), y + 8, GRAY);
            return;
        }
        int shown = Math.min(5, CityData.cardComments.size());
        for (int i = 0; i < shown; i++) {
            CityData.CommentInfo c = CityData.cardComments.get(i);
            int nx = seg(g, left() + 4, y + 3, c.authorName(), GOLD_BRIGHT);
            seg(g, nx + 6, y + 3, BUILDING_DATE.format(new Date(c.createdAt())), DIM);
            int maxW = right() - left() - (c.canDelete() ? 64 : 8);
            clipText(g, c.text(), left() + 4, y + 15, maxW, WHITE);
            y += COMMENT_ROW_H;
        }
        if (CityData.cardComments.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.cardComments.size() - shown) + " комментариев", left() + 4, y + 2, DIM);
        }
    }

    private void renderLaws(GuiGraphicsExtractor g, int top, boolean dataReady) {
        boolean canEdit = selectedCity.equals(CityData.cityName) && CityData.cardCanEditLaws;
        int y = top + BUILDING_LIST_TOP + (canEdit ? 26 : 0);
        if (!dataReady) {
            g.centeredText(this.font, "Загрузка…", cx(), y + 8, GRAY);
            return;
        }
        if (CityData.cardLaws.isEmpty()) {
            g.centeredText(this.font, canEdit ? "Законов пока нет — впиши первый" : "В этом городе законов нет", cx(), y + 8, GRAY);
            return;
        }
        int shown = Math.min(6, CityData.cardLaws.size());
        for (int i = 0; i < shown; i++) {
            CityData.LawInfo l = CityData.cardLaws.get(i);
            int nx = seg(g, left() + 4, y + 6, (i + 1) + ". ", GOLD);
            int maxW = right() - left() - (canEdit ? 64 : 8) - (nx - left() - 4);
            clipText(g, l.text(), nx, y + 6, maxW, WHITE);
            y += LAW_ROW_H;
        }
        if (CityData.cardLaws.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.cardLaws.size() - shown) + " законов", left() + 4, y + 2, DIM);
        }
    }

    private void renderBuildingDetail(GuiGraphicsExtractor g, int top) {
        CityData.BuildingInfo b = findSelectedBuilding();
        if (b == null) {
            g.centeredText(this.font, "Постройка не найдена", cx(), top + BUILDING_LIST_TOP + 8, GRAY);
            return;
        }
        int pw = Math.min(320, right() - left());
        int ph = pw * 9 / 16;
        int py = top + BUILDING_LIST_TOP;
        if (BuildingPhotos.get(b.photoId()) == null) {
            String note = b.photoId().isEmpty() ? "без фото" : "фото загружается…";
            g.centeredText(this.font, note, cx(), py + ph / 2 - 4, DIM);
        }
        int y = py + ph + 8;
        int nx = seg(g, left(), y, b.name(), GOLD_BRIGHT);
        seg(g, nx + 10, y, BUILDING_DATE.format(new Date(b.createdAt())), DIM);
        rightText(g, "построил " + b.ownerName(), right(), y, GRAY);
        y += 14;
        g.text(this.font, b.world() + "  " + b.minX() + " " + b.minY() + " " + b.minZ()
                + "  →  " + b.maxX() + " " + b.maxY() + " " + b.maxZ(), left(), y, BLUE);
        y += 16;
        for (String line : wrapText(b.description(), right() - left())) {
            g.text(this.font, line, left(), y, WHITE);
            y += 11;
            if (y > py2() - 20) break; // не вылезаем за панель
        }
    }

    private void renderBuildingForm(GuiGraphicsExtractor g, int top) {
        g.text(this.font, "НОВАЯ ПОСТРОЙКА", left(), top + BUILDING_LIST_TOP - 4, DIM);
        int hintY = top + 40 + 26 + 20 + 34 + 24 + 26;
        g.text(this.font, "Контур должен быть отмечен рулеткой (два угла).", left(), hintY, GRAY);
        g.text(this.font, "«Сделать фото» закроет меню: 3 секунды, чтобы навести камеру.", left(), hintY + 12, DIM);
    }

    /** Простой перенос строк по словам под ширину панели. */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) return lines;
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(probe) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(probe);
            }
        }
        lines.add(line.toString());
        return lines;
    }

    private void renderTop(GuiGraphicsExtractor g, int top) {
        if (CityData.top.isEmpty()) {
            g.centeredText(this.font, "Городов пока нет", cx(), top + 12, GRAY);
            return;
        }
        int y = top;
        int place = 1;
        for (CityData.TopEntry e : CityData.top) {
            int placeColor = place == 1 ? GOLD_BRIGHT : place == 2 ? SILVER : place == 3 ? BRONZE : DIM;
            g.text(this.font, String.valueOf(place), left() + 2, y, placeColor);
            int nx = seg(g, left() + 16, y, e.name(), place <= 3 ? WHITE : GRAY);
            seg(g, nx + 8, y, e.members() + " жит.", DIM);
            rightText(g, String.valueOf(e.score()), right() - 4, y, placeColor);
            y += 18;
            place++;
        }
    }

    private void renderContracts(GuiGraphicsExtractor g, int top) {
        if (CityData.hasCity) {
            int y = top + CONTRACT_FORM_TOP_MARGIN;
            renderPickerLabel(g, y, "Нужно", "contractReq");
            renderPickerMatches(g, y, "contractReq");
            y += PICKER_BLOCK_H;
            renderPickerLabel(g, y, "Награда", "contractRew");
            renderPickerMatches(g, y, "contractRew");
        } else {
            g.text(this.font, "Чтобы заказывать контракты, вступи в город.", left(), top, GRAY);
            g.text(this.font, "Выполнять чужие контракты можно и без города.", left(), top + 12, DIM);
        }

        int y = contractsListTop(top);
        if (CityData.contracts.isEmpty()) {
            g.text(this.font, "Контрактов пока нет", left(), y + 4, DIM);
            return;
        }
        int shown = Math.min(6, CityData.contracts.size());
        for (int i = 0; i < shown; i++) {
            CityData.ContractInfo c = CityData.contracts.get(i);
            int nx = seg(g, left() + 4, y + 4, c.cityName() + "  ", GOLD_BRIGHT);
            nx = seg(g, nx, y + 4, c.requiredAmount() + "× " + Catalogs.resourceName(c.requiredMaterial()), WHITE);
            nx = seg(g, nx, y + 4, "  →  ", DIM);
            seg(g, nx, y + 4, c.rewardAmount() + "× " + Catalogs.resourceName(c.rewardMaterial()), GREEN);
            y += 20;
        }
        if (CityData.contracts.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.contracts.size() - shown) + " контрактов", left() + 4, y + 4, DIM);
        }
    }

    private void renderBounties(GuiGraphicsExtractor g, int top) {
        g.text(this.font, "ЗАКАЗАТЬ РОЗЫСК — ник цели и награда из твоего инвентаря", left(), top, DIM);

        int pickerY = top + BOUNTY_NICK_TOP + BOUNTY_NICK_ROW_H;
        renderPickerLabel(g, pickerY, "Награда", "bountyReward");
        renderPickerMatches(g, pickerY, "bountyReward");

        int y = bountyListTop(top);

        if (CityData.myHunt != null) {
            CityData.MyHunt hunt = CityData.myHunt;
            int hy = y - MY_HUNT_BLOCK_H;
            int nx = seg(g, left(), hy, "ТВОЯ ОХОТА   ", GOLD);
            seg(g, nx, hy, "цель — " + hunt.targetName(), WHITE);
            hy += 12;
            if (hunt.hasCoords()) {
                nx = seg(g, left(), hy, "Последние координаты  ", DIM);
                nx = seg(g, nx, hy, hunt.world() + " " + hunt.x() + " / " + hunt.y() + " / " + hunt.z(), GOLD_BRIGHT);
                seg(g, nx, hy, "  " + ((System.currentTimeMillis() - hunt.lastRevealAt()) / 60000) + " мин назад", DIM);
            } else {
                g.text(this.font, "Координаты придут через 30 минут после взятия заказа", left(), hy, GRAY);
            }
            hy += 12;
            g.text(this.font, "Погибнешь — заказ провалится и достанется другому", left(), hy, RED);
        }

        if (CityData.bounties.isEmpty()) {
            g.text(this.font, "Заказов пока нет", left(), y + 4, DIM);
            return;
        }
        int shown = Math.min(6, CityData.bounties.size());
        for (int i = 0; i < shown; i++) {
            CityData.BountyInfo b = CityData.bounties.get(i);
            int nx = seg(g, left() + 4, y + 4, b.targetName() + "  ", GOLD_BRIGHT);
            seg(g, nx, y + 4, b.rewardAmount() + "× " + Catalogs.resourceName(b.rewardMaterial()), WHITE);
            if (b.claimed()) rightText(g, "в розыске", right() - 4, y + 4, RED);
            else rightText(g, "свободен", right() - 63, y + 4, GREEN);
            y += 20;
        }
        if (CityData.bounties.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.bounties.size() - shown) + " заказов", left() + 4, y + 4, DIM);
        }
    }

    private void renderMarket(GuiGraphicsExtractor g, int top, int mouseX, int mouseY) {
        g.text(this.font, "ВЫСТАВИТЬ ЛОТ — цена текстом, количество из предмета в руке", left(), top - 2, DIM);

        int y = marketListTop(top);
        if (CityData.market.isEmpty()) {
            g.text(this.font, "Лотов пока нет", left(), y + 4, DIM);
            return;
        }
        int shown = Math.min(6, CityData.market.size());
        for (int i = 0; i < shown; i++) {
            CityData.MarketListingInfo l = CityData.market.get(i);
            int iconX = left() + 2, iconY = y;
            if (!l.item().isEmpty()) {
                g.item(l.item(), iconX, iconY);
                if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= iconY && mouseY < iconY + 16) {
                    g.setTooltipForNextFrame(this.font, l.item(), mouseX, mouseY);
                }
            }
            int nx = seg(g, iconX + 20, y + 4, l.sellerName() + "  ", GOLD_BRIGHT);
            nx = seg(g, nx, y + 4, l.priceText(), WHITE);
            if (l.mine()) {
                String interest = l.interestedNames().isEmpty() ? "пока никто не отметился"
                        : "интерес: " + String.join(", ", l.interestedNames());
                g.text(this.font, interest, iconX + 20, y + 14, l.interestedCount() > 0 ? GREEN : DIM);
            }
            y += 20;
        }
        if (CityData.market.size() > shown) {
            g.text(this.font, "… ещё " + (CityData.market.size() - shown) + " лотов", left() + 4, y + 4, DIM);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
