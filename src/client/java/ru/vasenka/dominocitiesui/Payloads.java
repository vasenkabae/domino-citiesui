package ru.vasenka.dominocitiesui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Custom-payload каналы dominocities:*. Каждый пакет несёт «сырой» byte[], который мы
 * сериализуем/десериализуем через java.io.Data*Stream (см. {@link CityActions}/{@link CityData}),
 * чтобы формат совпадал с серверной стороной на Bukkit (та отдаёт/принимает голый byte[]).
 * Codec просто пишет/читает все оставшиеся байты — без своего length-префикса.
 */
public final class Payloads {
    private Payloads() {}

    private static byte[] readAll(FriendlyByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Protocol.NS, path);
    }

    public record Action(byte[] data) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id(Protocol.CH_ACTION));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Action(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(byte[] data) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id(Protocol.CH_STATE));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new State(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Top(byte[] data) implements CustomPacketPayload {
        public static final Type<Top> TYPE = new Type<>(id(Protocol.CH_TOP));
        public static final StreamCodec<FriendlyByteBuf, Top> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Top(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Result(byte[] data) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(id(Protocol.CH_RESULT));
        public static final StreamCodec<FriendlyByteBuf, Result> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Result(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Directory(byte[] data) implements CustomPacketPayload {
        public static final Type<Directory> TYPE = new Type<>(id(Protocol.CH_DIRECTORY));
        public static final StreamCodec<FriendlyByteBuf, Directory> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Directory(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Resources(byte[] data) implements CustomPacketPayload {
        public static final Type<Resources> TYPE = new Type<>(id(Protocol.CH_RESOURCES));
        public static final StreamCodec<FriendlyByteBuf, Resources> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Resources(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Contracts(byte[] data) implements CustomPacketPayload {
        public static final Type<Contracts> TYPE = new Type<>(id(Protocol.CH_CONTRACTS));
        public static final StreamCodec<FriendlyByteBuf, Contracts> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Contracts(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
