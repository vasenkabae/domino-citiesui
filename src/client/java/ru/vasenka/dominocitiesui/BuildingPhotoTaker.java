package ru.vasenka.dominocitiesui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * «Фотоаппарат» сохранения постройки: после формы экран закрывается, идёт отсчёт 3-2-1
 * в оверлее, на последний кадр прячется HUD (чистый кадр), снимок ужимается до 480×270
 * (центр-кроп до 16:9 + resize средствами NativeImage/stb), грузится на VDS, и только
 * затем на сервер уходит A_BUILDING_SAVE с photoId (или "" — если загрузка не удалась).
 */
public final class BuildingPhotoTaker {
    private BuildingPhotoTaker() {}

    private static final int COUNTDOWN_TICKS = 60; // 3 секунды
    private static final int TARGET_W = 480, TARGET_H = 270;

    private static String pendingName, pendingDesc;
    private static int ticksLeft = -1; // -1 = не активен
    private static boolean hidGui;

    public static void start(String name, String description) {
        pendingName = name;
        pendingDesc = description;
        ticksLeft = COUNTDOWN_TICKS;
    }

    public static boolean active() { return ticksLeft >= 0; }

    /** Вызывается каждый клиентский тик (см. DominoCitiesUIClient). */
    public static void tick(Minecraft mc) {
        if (ticksLeft < 0) return;
        if (mc.player == null) { cancel(mc); return; }
        if (ticksLeft > 0) {
            if (ticksLeft % 20 == 0) {
                mc.gui.setOverlayMessage(
                        Component.literal("§6Фото через " + (ticksLeft / 20) + "…  §7наведи камеру на постройку"), false);
            }
            // Последний тик перед снимком — прячем HUD, чтобы кадр был чистым.
            if (ticksLeft == 1 && !mc.options.hideGui) { mc.options.hideGui = true; hidGui = true; }
            ticksLeft--;
            return;
        }
        // ticksLeft == 0: если игрок успел открыть какой-то экран — ждём, пока закроет.
        if (mc.screen != null) return;
        ticksLeft = -1;
        String name = pendingName, desc = pendingDesc;
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), img -> {
            if (hidGui) { mc.options.hideGui = false; hidGui = false; }
            CompletableFuture.runAsync(() -> uploadAndSave(mc, name, desc, img));
        });
    }

    private static void cancel(Minecraft mc) {
        ticksLeft = -1;
        if (hidGui) { mc.options.hideGui = false; hidGui = false; }
    }

    private static void uploadAndSave(Minecraft mc, String name, String desc, NativeImage source) {
        String photoId = "";
        try {
            byte[] png = shrinkToPng(source);
            String id = UUID.randomUUID().toString();
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(BuildingPhotos.BASE_URL + "/launcher/build-photo?id=" + id))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(png))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) photoId = id;
        } catch (Exception ignored) {
            // photoId останется пустым — постройка сохранится без фото
        } finally {
            source.close();
        }
        String finalPhotoId = photoId;
        mc.execute(() -> {
            if (finalPhotoId.isEmpty() && mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal("§cНе удалось загрузить фото — постройка сохранится без него."));
            }
            CityActions.saveBuilding(name, desc, finalPhotoId);
        });
    }

    /** Центр-кроп до 16:9 + ресайз до 480×270; результат — PNG-байты. */
    private static byte[] shrinkToPng(NativeImage source) throws Exception {
        int w = source.getWidth(), h = source.getHeight();
        int cropW = w, cropH = h, offX = 0, offY = 0;
        if (w * TARGET_H > h * TARGET_W) { // шире 16:9 — режем бока
            cropW = h * TARGET_W / TARGET_H;
            offX = (w - cropW) / 2;
        } else {                           // выше 16:9 — режем верх/низ
            cropH = w * TARGET_H / TARGET_W;
            offY = (h - cropH) / 2;
        }
        Path tmp = Files.createTempFile("domino-build", ".png");
        try (NativeImage target = new NativeImage(TARGET_W, TARGET_H, false)) {
            source.resizeSubRectTo(offX, offY, cropW, cropH, target);
            target.writeToFile(tmp);
            return Files.readAllBytes(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
