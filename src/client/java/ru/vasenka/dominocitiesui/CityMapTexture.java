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

/**
 * Кэш плоской карты городов: один файл на сервер (/dc/citymap.png), перекачивается заново
 * только когда сервер прислал новую версию (mapVersion). Паттерн зеркалит {@link BuildingPhotos}.
 */
public final class CityMapTexture {
    private CityMapTexture() {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static long loadedVersion = -1;
    private static Identifier texture = null;
    private static int texW = 1, texH = 1;
    private static boolean pending = false;
    private static long pendingVersion = -1;

    /** Текстура карты нужной версии или null (версии ещё нет / качается / не удалось). */
    public static Identifier get(long version) {
        if (version == 0) return null;
        if (texture != null && loadedVersion == version) return texture;
        if (pending && pendingVersion == version) return null;
        pending = true;
        pendingVersion = version;
        HttpRequest req = HttpRequest.newBuilder(URI.create(BuildingPhotos.BASE_URL + "/dc/citymap.png"))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((resp, err) -> {
            pending = false;
            if (err != null || resp.statusCode() != 200) return;
            byte[] png = resp.body();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    NativeImage img = NativeImage.read(png);
                    Identifier texId = Identifier.fromNamespaceAndPath(Protocol.NS, "city_map/" + version);
                    mc.getTextureManager().register(texId, new DynamicTexture(() -> "city map " + version, img));
                    texW = img.getWidth();
                    texH = img.getHeight();
                    texture = texId;
                    loadedVersion = version;
                } catch (Exception ignored) { }
            });
        });
        return null;
    }

    public static int width() { return texW; }
    public static int height() { return texH; }
}
