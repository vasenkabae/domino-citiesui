package ru.vasenka.dominocitiesui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Отправка действий экрана правил клиент → сервер (канал dominoauth:action). */
public final class AuthActions {
    private AuthActions() {}

    private static void send(byte type, String... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(AuthProtocol.VERSION);
            out.writeByte(type);
            for (String a : args) out.writeUTF(a);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new AuthPayloads.Action(bos.toByteArray()));
    }

    public static void requestState() { send(AuthProtocol.A_REQUEST_STATE); }
    public static void acceptRules() { send(AuthProtocol.A_ACCEPT_RULES); }
}
