package ru.vasenka.dominocitiesui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Сборка и отправка действий клиент → сервер (канал dominocities:action). */
public final class CityActions {
    private CityActions() {}

    private static byte[] build(byte type, String... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(type);
            for (String a : args) out.writeUTF(a);
        } catch (IOException e) {
            throw new RuntimeException(e); // на ByteArrayOutputStream не бывает
        }
        return bos.toByteArray();
    }

    private static void send(byte type, String... args) {
        ClientPlayNetworking.send(new Payloads.Action(build(type, args)));
    }

    public static void requestState()      { send(Protocol.A_REQUEST_STATE); }
    public static void requestTop()         { send(Protocol.A_REQUEST_TOP); }
    public static void create(String name)  { send(Protocol.A_CREATE, name); }
    public static void invite(String nick)  { send(Protocol.A_INVITE, nick); }
    public static void join(String name)    { send(Protocol.A_JOIN, name); }
    public static void leave()              { send(Protocol.A_LEAVE); }
    public static void kick(String uuid)    { send(Protocol.A_KICK, uuid); }
    public static void disband()            { send(Protocol.A_DISBAND); }
    public static void toggleBorder()       { send(Protocol.A_TOGGLE_BORDER); }
    public static void buyBuff(String id)   { send(Protocol.A_BUY_BUFF, id); }
    public static void setSpecialization(String id) { send(Protocol.A_SET_SPEC, id); }
    public static void collect()            { send(Protocol.A_COLLECT); }
    public static void promote(String uuid) { send(Protocol.A_PROMOTE, uuid); }
    public static void demote(String uuid)  { send(Protocol.A_DEMOTE, uuid); }
    public static void transfer(String uuid) { send(Protocol.A_TRANSFER, uuid); }
    public static void toggleOpen()         { send(Protocol.A_TOGGLE_OPEN); }
    public static void requestDirectory()   { send(Protocol.A_REQUEST_DIRECTORY); }
    public static void requestResources()   { send(Protocol.A_REQUEST_RESOURCES); }

    public static void setTitle(byte role, String title) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_SET_TITLE);
            out.writeByte(role);
            out.writeUTF(title);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }
}
