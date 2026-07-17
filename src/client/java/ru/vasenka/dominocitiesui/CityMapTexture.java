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
import java.util.Map;

/**
 * Кэш плоских карт городов: по одному файлу на мир (/dc/citymap.png — верхний, /dc/citymap_nether.png —
 * Нижний), каждый перекачивается заново только когда сервер прислал новую версию. Паттерн зеркалит
 * {@link BuildingPhotos}. Состояние держим отдельно на каждый файл (ключ — короткое имя карты).
 */
public final class CityMapTexture {
    private CityMapTexture() {}

    /** Ключи карт = имена файлов на VDS (относительно BASE_URL/dc). */
    public static final String OVERWORLD = "citymap.png";
    public static final String NETHER    = "citymap_nether.png";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static final class Slot {
        long loadedVersion = -1;
        Identifier texture = null;
        int w = 1, h = 1;
        boolean pending = false;
        long pendingVersion = -1;
    }

    private static final Map<String, Slot> SLOTS = new HashMap<>();

    private static Slot slot(String file) {
        return SLOTS.computeIfAbsent(file, k -> new Slot());
    }

    /** Текстура карты нужной версии или null (версии ещё нет / качается / не удалось). */
    public static Identifier get(long version, String file) {
        if (version == 0) return null;
        Slot s = slot(file);
        if (s.texture != null && s.loadedVersion == version) return s.texture;
        if (s.pending && s.pendingVersion == version) return null;
        s.pending = true;
        s.pendingVersion = version;
        HttpRequest req = HttpRequest.newBuilder(URI.create(BuildingPhotos.BASE_URL + "/dc/" + file))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((resp, err) -> {
            s.pending = false;
            if (err != null || resp.statusCode() != 200) return;
            byte[] png = resp.body();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                try {
                    NativeImage img = NativeImage.read(png);
                    Identifier texId = Identifier.fromNamespaceAndPath(Protocol.NS,
                            "city_map/" + file.replace('.', '_') + "/" + version);
                    mc.getTextureManager().register(texId, new DynamicTexture(() -> "city map " + file + " " + version, img));
                    s.w = img.getWidth();
                    s.h = img.getHeight();
                    s.texture = texId;
                    s.loadedVersion = version;
                } catch (Exception ignored) { }
            });
        });
        return null;
    }

    public static int width(String file)  { return slot(file).w; }
    public static int height(String file) { return slot(file).h; }
}
