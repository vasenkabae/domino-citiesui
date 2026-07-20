package ru.vasenka.dominocitiesui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Custom-payload каналы dominoauth:*. Тот же приём, что в {@link Payloads}/{@link SkillsPayloads}. */
public final class AuthPayloads {
    private AuthPayloads() {}

    private static byte[] readAll(FriendlyByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AuthProtocol.NS, path);
    }

    public record Action(byte[] data) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id(AuthProtocol.CH_ACTION));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new Action(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(byte[] data) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id(AuthProtocol.CH_STATE));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.data), buf -> new State(readAll(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
