package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * Вход/регистрация в стиле K-меню — заменяет чат-команды /login и /register для игроков
 * с модом (чат-путь остаётся рабочим фолбэком, сервер валидирует оба пути одинаково).
 *
 * Принудительно открывается и НЕ закрывается: shouldCloseOnEsc()==false, плюс страж на
 * тике в DominoCitiesUIClient переоткрывает экран, если needsAuth всё ещё true (перекрывает
 * и Esc→null, и попытку открыть любой другой экран). Закрывает себя САМ, когда AuthData
 * получает needsAuth=false (см. AuthData.refresh) — руками закрыть нельзя, дождаться success можно.
 *
 * Пароль маскируется звёздочками через EditBox.addFormatter (в этом форке нет vanilla
 * setFormatter — только addFormatter(TextFormatter), сверено javap 2026-07-17). Формиттер
 * читает флаг showPassword при каждом кадре, поэтому переключение «показать/скрыть»
 * не требует rebuildWidgets и не теряет введённый текст.
 */
public class AuthScreen extends Screen {

    private static final int WHITE  = 0xFFF2F3F5;
    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int GREEN  = 0xFF7CCB6E;
    private static final int RED    = 0xFFEB7069;

    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int SCRIM        = 0xB0000000;

    private static final int PANEL_W = 250;
    private static final int FIELD_H = 20;

    private EditBox passwordBox;
    private EditBox confirmBox;
    private boolean showPassword = false;
    private int toggleY; // строка «Показать пароль» — рисуется вручную, клик в mouseClicked

    public AuthScreen() {
        super(Component.literal("Domino Craft"));
    }

    private boolean registering() { return !AuthData.registered; }

    private int panelH() { return registering() ? 196 : 162; }
    private int px1() { return this.width / 2 - PANEL_W / 2; }
    private int px2() { return this.width / 2 + PANEL_W / 2; }
    private int py1() { return this.height / 2 - panelH() / 2; }
    private int py2() { return this.height / 2 + panelH() / 2; }

    /** Маскирующий форматтер: звёздочки вместо символов, пока showPassword выключен. */
    private void mask(EditBox box) {
        box.addFormatter((text, firstIdx) -> FormattedCharSequence.forward(
                showPassword ? text : "*".repeat(text.length()), Style.EMPTY));
    }

    @Override
    protected void init() {
        boolean registering = registering();
        int fieldW = PANEL_W - 40;
        int x = px1() + 20;
        int y = py1() + 56; // под заголовком/линией + подпись поля

        passwordBox = new EditBox(this.font, x, y, fieldW, FIELD_H,
                Component.literal(registering ? "Придумай пароль" : "Пароль"));
        passwordBox.setMaxLength(64);
        passwordBox.setHint(Component.literal(registering ? "минимум 4 символа" : ""));
        mask(passwordBox);
        addRenderableWidget(passwordBox);
        setInitialFocus(passwordBox);
        y += FIELD_H + 16;

        if (registering) {
            confirmBox = new EditBox(this.font, x, y, fieldW, FIELD_H, Component.literal("Повтори пароль"));
            confirmBox.setMaxLength(64);
            mask(confirmBox);
            addRenderableWidget(confirmBox);
            y += FIELD_H + 16;
        } else {
            confirmBox = null;
        }

        toggleY = y - 12;
        y += 4;

        addRenderableWidget(Button.builder(
                Component.literal(registering ? "Зарегистрироваться" : "Войти"),
                b -> submit()).bounds(x, y, fieldW, FIELD_H).build());
    }

    private void submit() {
        String pw = passwordBox.getValue();
        if (pw.isEmpty()) return; // сервер и так откажет, но не спамим лишний пакет
        if (confirmBox != null) AuthActions.register(pw, confirmBox.getValue());
        else AuthActions.login(pw);
    }

    /** Дёргается из AuthData.onResult — ошибку/успех покажет тост из AuthData.lastResult в extractRenderState. */
    public void onResult() { }

    /** Дёргается из AuthData.onState — форма могла смениться (например, только что зарегистрировался). */
    public void refresh() { rebuildWidgets(); }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    private String toggleLabel() { return showPassword ? "Скрыть пароль" : "Показать пароль"; }

    private boolean toggleHit(double mx, double my) {
        int w = this.font.width(toggleLabel());
        int tx = px2() - 20 - w;
        return mx >= tx - 2 && mx <= px2() - 18 && my >= toggleY - 2 && my <= toggleY + 10;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (toggleHit(event.x(), event.y())) {
            showPassword = !showPassword;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void scaledText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.centeredText(this.font, s, 0, 0, color);
        pose.popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, SCRIM);
        g.fillGradient(px1(), py1(), px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), py1(), PANEL_W, panelH(), PANEL_EDGE);
        // Тонкая золотая рамка-акцент внутри основной
        g.outline(px1() + 3, py1() + 3, PANEL_W - 6, panelH() - 6, 0x14F2B94E);
        g.horizontalLine(px1() + 12, px2() - 13, py1() + 40, GOLD_LINE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        boolean registering = registering();
        int cx = this.width / 2;

        scaledText(g, "DOMINO CRAFT", cx, py1() + 12, 1.15f, GOLD);
        int tw = (int) (this.font.width("DOMINO CRAFT") * 1.15f / 2) + 10;
        g.text(this.font, "◆", cx - tw - 8, py1() + 13, GOLD_LINE);
        g.text(this.font, "◆", cx + tw + 2, py1() + 13, GOLD_LINE);

        String nick = Minecraft.getInstance().getUser().getName();
        g.centeredText(this.font, registering
                ? "Регистрация — защити свой ник"
                : "С возвращением, " + nick + "!", cx, py1() + 28, GRAY);

        // Подписи над полями
        g.text(this.font, "Пароль", px1() + 20, py1() + 46, GRAY);
        if (registering) g.text(this.font, "Повтори пароль", px1() + 20, py1() + 82, GRAY);

        // «Показать/скрыть пароль» — кликабельный текст справа
        boolean hover = toggleHit(mouseX, mouseY);
        String label = toggleLabel();
        g.text(this.font, label, px2() - 20 - this.font.width(label), toggleY,
                hover ? GOLD_BRIGHT : DIM);

        if (!AuthData.lastResult.isEmpty()) {
            g.centeredText(this.font, AuthData.lastResult, cx, py2() - 14,
                    AuthData.lastOk ? GREEN : RED);
        }
    }
}
