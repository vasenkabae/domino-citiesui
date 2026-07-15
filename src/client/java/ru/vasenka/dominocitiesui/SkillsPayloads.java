package ru.vasenka.dominocitiesui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Custom-payload каналы dominoskills:*. Тот же приём, что в {@link Payloads}:
 * пакет несёт сырой byte[], сериализация — java.io.Data*Stream (см. SkillsData/SkillsActions).
 */
public final class SkillsPayloads {
    private SkillsPayloads() {}

    private static byte[] readAll(FriendlyByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkillsProtocol.NS, path);
    }

    public record Action(byte[] data) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id(SkillsProtocol.CH_ACTION));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Action(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(byte[] data) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id(SkillsProtocol.CH_STATE));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new State(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Xp(byte[] data) implements CustomPacketPayload {
        public static final Type<Xp> TYPE = new Type<>(id(SkillsProtocol.CH_XP));
        public static final StreamCodec<FriendlyByteBuf, Xp> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Xp(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Result(byte[] data) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(id(SkillsProtocol.CH_RESULT));
        public static final StreamCodec<FriendlyByteBuf, Result> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Result(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
