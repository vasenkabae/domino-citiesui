package ru.vasenka.dominocitiesui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Кэш фото построек: качает PNG с VDS (та же раздача статики, что и скины) и регистрирует
 * как динамическую текстуру. get() неблокирующий — пока фото не скачано, возвращает null,
 * загрузка стартует в фоне; после регистрации текстуры экран отрисует её следующим кадром.
 */
public final class BuildingPhotos {
    private BuildingPhotos() {}

    public static final String BASE_URL = "http://138.16.181.96:8765";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<String, Identifier> loaded = new HashMap<>();
    private static final Map<String, int[]> sizes = new HashMap<>(); // photoId → {w, h}
    private static final Set<String> pending = new HashSet<>();
    private static final Set<String> failed = new HashSet<>();

    /** Текстура фото или null (нет фото / ещё качается / не удалось). Можно дёргать каждый кадр. */
    public static Identifier get(String photoId) {
        if (photoId == null || photoId.isEmpty()) return null;
        String id = photoId.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9-]{1,64}")) return null;
        Identifier tex = loaded.get(id);
        if (tex != null) return tex;
        if (failed.contains(id) || !pending.add(id)) return null;
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/dc/builds/" + id + ".png"))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((resp, err) -> {
            if (err != null || resp.statusCode() != 200) {
                failed.add(id);
                pending.remove(id);
                return;
            }
            byte[] png = resp.body();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    NativeImage img = NativeImage.read(png);
                    Identifier texId = Identifier.fromNamespaceAndPath(Protocol.NS, "building_photo/" + id);
                    mc.getTextureManager().register(texId, new DynamicTexture(() -> "building photo " + id, img));
                    sizes.put(id, new int[]{img.getWidth(), img.getHeight()});
                    loaded.put(id, texId);
                } catch (Exception e) {
                    failed.add(id);
                } finally {
                    pending.remove(id);
                }
            });
        });
        return null;
    }

    /** Размер скачанного фото (валиден после того, как get() вернул не-null). */
    public static int[] size(String photoId) {
        return sizes.getOrDefault(photoId.toLowerCase(Locale.ROOT), new int[]{16, 9});
    }
}
