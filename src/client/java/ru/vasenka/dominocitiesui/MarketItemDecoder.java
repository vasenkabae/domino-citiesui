package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Разбирает байты предмета, присланные сервером (Bukkit {@code ItemStack.serializeAsBytes()}),
 * в настоящий клиентский {@link ItemStack} — с зачарованиями и прочим NBT, для рендера иконки и
 * ванильного тултипа ({@code GuiGraphicsExtractor.item}/{@code setTooltipForNextFrame}).
 * Формат — vanilla NBT CompoundTag (сжатый gzip или сырой, определяем по магическому байту).
 */
final class MarketItemDecoder {
    private MarketItemDecoder() {}

    static ItemStack decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || Minecraft.getInstance().level == null) return ItemStack.EMPTY;
        try {
            CompoundTag tag = readTag(bytes);
            var ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
            return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static CompoundTag readTag(byte[] bytes) throws IOException {
        boolean gzip = bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1f && (bytes[1] & 0xFF) == 0x8b;
        if (gzip) {
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return NbtIo.read(in);
        }
    }
}
