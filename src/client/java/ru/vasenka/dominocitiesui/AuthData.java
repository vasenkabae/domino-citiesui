package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/** Снапшот состояния входа/регистрации — гоняет AuthScreen: открыть/переключить форму/закрыть. */
public final class AuthData {
    private AuthData() {}

    public static boolean needsAuth = false;
    public static boolean registered = false;
    public static boolean needsRules = false;
    /** true — уже пришёл хотя бы один ответ сервера; пока false, GUI не форсируем (сервер может
     *  быть без DominoAuth вовсе — тогда останется false навсегда, и чат-путь /login работает как раньше). */
    public static boolean stateReceived = false;
    public static String lastResult = "";
    public static boolean lastOk = true;

    public static void onState(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != AuthProtocol.VERSION) return; // несовместимая версия — не форсируем битый GUI
            needsAuth = in.readBoolean();
            registered = in.readBoolean();
            needsRules = in.readBoolean();
            stateReceived = true;
        } catch (Exception ignored) { return; }
        refresh();
    }

    public static void onResult(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != AuthProtocol.VERSION) return;
            lastOk = in.readBoolean();
            lastResult = in.readUTF();
        } catch (Exception ignored) { return; }
        if (Minecraft.getInstance().screen instanceof AuthScreen screen) screen.onResult();
    }

    /** Успех — экран закрывает себя сам (или переключается на следующий gate), иначе пересобирает форму. */
    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AuthScreen screen) {
            if (needsAuth) screen.refresh();
            else mc.setScreen(needsRules ? new RulesScreen() : null);
        } else if (mc.screen instanceof RulesScreen && !needsRules) {
            mc.setScreen(null);
        }
    }

    /** Сброс на дисконнекте — новый заход должен снова дождаться состояния с сервера. */
    public static void reset() {
        needsAuth = false;
        registered = false;
        needsRules = false;
        stateReceived = false;
        lastResult = "";
        lastOk = true;
    }
}
