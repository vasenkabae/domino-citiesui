package ru.vasenka.dominocitiesui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Отправка действий профессий клиент → сервер (канал dominoskills:action). */
public final class SkillsActions {
    private SkillsActions() {}

    private static void send(byte type, String... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(SkillsProtocol.VERSION);
            out.writeByte(type);
            for (String a : args) out.writeUTF(a);
        } catch (IOException e) {
            throw new RuntimeException(e); // на ByteArrayOutputStream не бывает
        }
        ClientPlayNetworking.send(new SkillsPayloads.Action(bos.toByteArray()));
    }

    public static void requestState()      { send(SkillsProtocol.A_REQUEST_STATE); }
    public static void learn(String id)    { send(SkillsProtocol.A_LEARN, id); }
    public static void reset()             { send(SkillsProtocol.A_RESET); }

    public static void activate(int profId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(SkillsProtocol.VERSION);
            out.writeByte(SkillsProtocol.A_ACTIVATE);
            out.writeByte(profId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new SkillsPayloads.Action(bos.toByteArray()));
    }
}
