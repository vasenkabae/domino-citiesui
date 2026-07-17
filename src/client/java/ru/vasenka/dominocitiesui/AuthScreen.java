package ru.vasenka.dominocitiesui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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
 * Пароль НЕ маскируется точками — в этом форке у EditBox нет vanilla setFormatter (только
 * addFormatter(TextFormatter), сверено javap 2026-07-17), а обычный чат-ввод /login <пароль>
 * тоже никогда не маскировался — паритет с уже существующим UX, не регресс.
 */
public class AuthScreen extends Screen {

    private static final int WHITE  = 0xFFF2F3F5;
    private static final int GRAY   = 0xFFA7ADB8;
    private static final int DIM    = 0xFF767D8A;
    private static final int GOLD        = 0xFFF2B94E;
    private static final int GOLD_BRIGHT = 0xFFFFD37A;
    private static final int RED    = 0xFFEB7069;

    private static final int PANEL_TOP    = 0xDD14151A;
    private static final int PANEL_BOTTOM = 0xEE0B0C10;
    private static final int PANEL_EDGE   = 0x2EFFFFFF;
    private static final int GOLD_LINE    = 0x66F2B94E;
    private static final int SCRIM        = 0xB0000000;

    private static final int PANEL_W = 240;
    private static final int FIELD_H = 20;
    private static final int GAP = 8;

    private EditBox passwordBox;
    private EditBox confirmBox;

    public AuthScreen() {
        super(Component.literal("Domino Craft"));
    }

    private boolean registering() { return !AuthData.registered; }

    private int panelH() { return registering() ? 168 : 138; }
    private int px1() { return this.width / 2 - PANEL_W / 2; }
    private int px2() { return this.width / 2 + PANEL_W / 2; }
    private int py1() { return this.height / 2 - panelH() / 2; }
    private int py2() { return this.height / 2 + panelH() / 2; }

    @Override
    protected void init() {
        boolean registering = registering();
        int fieldW = PANEL_W - 40;
        int x = px1() + 20;
        int y = py1() + 48;

        passwordBox = new EditBox(this.font, x, y, fieldW, FIELD_H,
                Component.literal(registering ? "Придумай пароль" : "Пароль"));
        passwordBox.setMaxLength(64);
        passwordBox.setHint(Component.literal(registering ? "Пароль (минимум 4 символа)" : "Пароль"));
        addRenderableWidget(passwordBox);
        setInitialFocus(passwordBox);
        y += FIELD_H + GAP;

        if (registering) {
            confirmBox = new EditBox(this.font, x, y, fieldW, FIELD_H, Component.literal("Повтори пароль"));
            confirmBox.setMaxLength(64);
            confirmBox.setHint(Component.literal("Повтори пароль"));
            addRenderableWidget(confirmBox);
            y += FIELD_H + GAP;
        } else {
            confirmBox = null;
        }

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

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, SCRIM);
        g.fillGradient(px1(), py1(), px2(), py2(), PANEL_TOP, PANEL_BOTTOM);
        g.outline(px1(), py1(), PANEL_W, panelH(), PANEL_EDGE);
        g.horizontalLine(px1() + 1, px2() - 2, py1() + 36, GOLD_LINE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        boolean registering = registering();
        int cx = this.width / 2;
        g.centeredText(this.font, "DOMINO CRAFT", cx, py1() + 11, GOLD);
        g.centeredText(this.font, registering ? "Регистрация" : "Вход", cx, py1() + 23, GRAY);

        if (!AuthData.lastResult.isEmpty()) {
            g.centeredText(this.font, AuthData.lastResult, cx, py2() - 16,
                    AuthData.lastOk ? WHITE : RED);
        }
    }
}
