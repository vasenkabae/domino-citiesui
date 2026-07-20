package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/** Снапшот состояния правил сервера — гоняет RulesScreen: открыть/закрыть. */
public final class AuthData {
    private AuthData() {}

    public static boolean needsRules = false;
    /** true — уже пришёл хотя бы один ответ сервера; пока false, GUI не форсируем (сервер может
     *  быть без DominoAuth вовсе — тогда останется false навсегда). */
    public static boolean stateReceived = false;

    public static void onState(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != AuthProtocol.VERSION) return; // несовместимая версия — не форсируем битый GUI
            needsRules = in.readBoolean();
            stateReceived = true;
        } catch (Exception ignored) { return; }
        refresh();
    }

    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RulesScreen && !needsRules) {
            mc.setScreen(null);
        }
    }

    /** Сброс на дисконнекте — новый заход должен снова дождаться состояния с сервера. */
    public static void reset() {
        needsRules = false;
        stateReceived = false;
    }
}
