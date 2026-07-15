package ru.vasenka.dominocitiesui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Памятка новичка: листаемые страницы со всеми фишками сервера. Показывается
 * автоматически после входа (см. DominoCitiesUIClient), пока игрок не нажмёт
 * «Больше не показывать» — решение хранится локально в config/dominocraft-guide.txt
 * как номер версии памятки: если памятку заметно дополнить, подними GUIDE_VERSION,
 * и она разово покажется снова даже тем, кто её скрывал. Открыть вручную — «?» в K-меню.
 *
 * Визуальный стиль повторяет CityScreen: тёмная стеклянная панель, золотые акценты,
 * фон в extractBackground (под виджетами), тексты в extractRenderState (поверх).
 */
public class GuideScreen extends Screen {

    private static final int GUIDE_VERSION = 2;

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

    private static final int PANEL_Y1 = 24;
    private static final int PAD = 16;
    private static final int CONTENT_TOP = 66;

    /**
     * Разметка строк: "## " — золотой подзаголовок (с отступом сверху),
     * "> " — белая ключевая строка (команды), пустая — половинный пропуск,
     * остальное — серый текст.
     */
    private record Page(String title, String[] lines) {}

    private static final Page[] PAGES = {
            new Page("Первые шаги", new String[]{
                    "## Вход на сервер",
                    "> /register <пароль> <пароль> — при первом входе",
                    "> /login <пароль> — при каждом следующем",
                    "## Главные клавиши",
                    "> K — города, контракты, розыск и рынок",
                    "> N — профессии и таланты",
                    "> Y — живая карта мира",
                    "## Мелочи",
                    "Ники скрыты — ПКМ по игроку покажет ник и город",
                    "> /sit — присесть (повторно или Shift — встать)",
            }),
            new Page("Города", new String[]{
                    "Объединяйся с друзьями — у города своя территория,",
                    "радиус растёт с числом жителей",
                    "## Как основать или вступить",
                    "Вкладка «Мой город» в K-меню: основать (не ближе",
                    "1000 блоков от спавна), пригласить, вступить",
                    "## Жизнь города",
                    "Кровать и возрождение работают только в своём городе",
                    "Баффы, сундуки и дорога до спавна — «Хозяйство»",
                    "## Витрина",
                    "Во «Все города»: постройки с фото, оценки,",
                    "комментарии и собственные законы города",
            }),
            new Page("Профессии", new String[]{
                    "## Клавиша N",
                    "6 профессий: шахтёр, фермер, охотник,",
                    "рыбак, строитель и исследователь",
                    "XP капает за обычные дела — копай, выращивай,",
                    "сражайся, лови рыбу, строй, исследуй мир",
                    "## Таланты",
                    "Каждый уровень даёт очко таланта — вкладывай",
                    "в пассивки, новые ряды открываются с уровнем",
                    "Передумал? Сброс всего древа — 16 алмазов",
            }),
            new Page("Контракты и рынок", new String[]{
                    "## Контракты (K-меню)",
                    "Закажи нужные ресурсы за награду — награда",
                    "резервируется из твоего инвентаря сразу",
                    "Берёшь чужой контракт? Требуемое должно уже лежать",
                    "в инвентаре — обмен мгновенный",
                    "## Рынок",
                    "Доска объявлений: предмет остаётся у продавца",
                    "Кнопка «Купить» отмечает интерес — продавец увидит,",
                    "кто хочет купить, и найдёт тебя. Сделка — вживую",
            }),
            new Page("Розыск", new String[]{
                    "Закажи голову обидчика — или заработай охотой",
                    "## Заказ",
                    "Награда берётся из твоего инвентаря сразу",
                    "и вернётся, только если снимешь заказ",
                    "## Охота",
                    "Взял заказ — раз в 30 минут приходят координаты цели",
                    "(пока вы оба онлайн)",
                    "Убил цель — награда твоя, и весь сервер узнает",
                    "Погиб сам — заказ провален и вернулся на доску",
            }),
            new Page("Карта мира", new String[]{
                    "## Клавиша Y",
                    "Живая карта — дорисовывается сама, пока",
                    "игроки исследуют новые места, и растёт с миром",
                    "Колесо мыши — зум, зажми ЛКМ — двигай карту",
                    "## Метки и свои",
                    "«Поставить метку» + клик — личная метка,",
                    "видишь только ты; ПКМ по метке — убрать",
                    "Жители твоего города видны на карте живьём",
            }),
            new Page("Питомцы", new String[]{
                    "> /pet — выбери себе зверя",
                    "15 зверей: от кота до ламы. Питомец один,",
                    "выбор нового заменяет старого",
                    "## Что умеет",
                    "Ходит за тобой по пятам и не теряется",
                    "Игроки и падения ему не страшны",
                    "(а вот лава и вода — страшны, береги)",
                    "Кнопка «Дать имя» — имя будет над головой",
            }),
            new Page("Чат", new String[]{
                    "## Три канала",
                    "Обычное сообщение — локальный чат, слышно в 50 блоках",
                    "> !текст — глобальный чат, видят все на сервере",
                    "> #текст — чат города, видят только твои горожане",
                    "",
                    "Кричать на весь сервер — с восклицательным знаком,",
                    "шептаться с соседями — просто так",
            }),
            new Page("Мир и удобства", new String[]{
                    "## Дикие земли",
                    "Дальше ~5000 блоков от спавна мобы вдвое живучее",
                    "и втрое злее — ходи подготовленным",
                    "## Разное",
                    "TAB — кто онлайн, города игроков и время суток",
                    "Ночь пропускается, когда спит 20% игроков",
                    "Свой скин загружается в лаунчере — увидят все",
                    "> /mapimg <ссылка> — картинка на карте (выдаёт админ)",
                    "",
                    "Открыть памятку снова — кнопка «?» в K-меню",
            }),
    };

    private int page = 0;
    private Button prevBtn, nextBtn;

    public GuideScreen() {
        super(Component.literal("Памятка Domino Craft"));
    }

    // ── «Больше не показывать» ───────────────────────────────────────────────

    private static Path markerFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("dominocraft-guide.txt");
    }

    /** true, если игрок скрыл текущую (или более новую) версию памятки. */
    public static boolean dismissed() {
        try {
            return Integer.parseInt(Files.readString(markerFile()).trim()) >= GUIDE_VERSION;
        } catch (Exception e) {
            return false; // файла нет или битый — показываем
        }
    }

    private static void dismissForever() {
        try {
            Files.writeString(markerFile(), String.valueOf(GUIDE_VERSION));
        } catch (Exception ignored) {
        }
    }

    // ── Геометрия (как в CityScreen) ─────────────────────────────────────────

    private int cx() { return this.width / 2; }
    private int panelHalf() { return Math.min(230, (this.width - 12) / 2); }
    private int px1() { return cx() - panelHalf(); }
    private int px2() { return cx() + panelHalf(); }
    private int py2() { return this.height - 12; }
    private int left() { return px1() + PAD; }
    private int right() { return px2() - PAD; }

    @Override
    protected void init() {
        prevBtn = addRenderableWidget(Button.builder(Component.literal("◀ Назад"),
                b -> turn(-1)).bounds(left(), py2() - 52, 90, 20).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal("Далее ▶"),
                b -> turn(1)).bounds(right() - 90, py2() - 52, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Больше не показывать"),
                b -> { dismissForever(); onClose(); })
                .bounds(left(), py2() - 26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                b -> onClose())
                .bounds(right() - 90, py2() - 26, 90, 20).build());
        updateNav();
    }

    private void turn(int dir) {
        page = Math.max(0, Math.min(PAGES.length - 1, page + dir));
        updateNav();
    }

    private void updateNav() {
        prevBtn.active = page > 0;
        nextBtn.active = page < PAGES.length - 1;
    }

    // ── Фон ──────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        g.fillGradient(px1(), PANEL_Y1, px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), PANEL_Y1, px2() - px1(), py2() - PANEL_Y1, PANEL_EDGE);
        // Шапка с названием страницы + золотой разделитель
        g.fill(px1() + 1, PANEL_Y1 + 1, px2() - 1, PANEL_Y1 + 32, 0x30000000);
        g.horizontalLine(px1() + 1, px2() - 2, PANEL_Y1 + 32, GOLD_LINE);
        // Карточка под контент
        g.fill(left() - 6, CONTENT_TOP - 6, right() + 6, py2() - 60, CARD);
    }

    // ── Тексты ───────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Заголовок над панелью
        scaledText(g, "ПАМЯТКА DOMINO CRAFT", cx(), 9, 1.15f, GOLD, true);
        int tw = (int) (this.font.width("ПАМЯТКА DOMINO CRAFT") * 1.15f / 2) + 10;
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
            } else if (line.startsWith("> ")) {
                g.text(this.font, line.substring(2), left() + 6, y, WHITE);
            } else {
                g.text(this.font, line, left(), y, GRAY);
            }
            y += 12;
        }

        // Точки-индикатор страниц между кнопками навигации
        int dotsW = PAGES.length * 10 - 4;
        int dx = cx() - dotsW / 2;
        int dy = py2() - 45;
        for (int i = 0; i < PAGES.length; i++) {
            g.fill(dx + i * 10, dy, dx + i * 10 + 6, dy + 6, i == page ? GOLD : 0x33FFFFFF);
        }
        g.centeredText(this.font, (page + 1) + " / " + PAGES.length, cx(), py2() - 66, DIM);
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
