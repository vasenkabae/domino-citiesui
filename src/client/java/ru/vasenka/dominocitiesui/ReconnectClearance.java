package ru.vasenka.dominocitiesui;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Токен устройства для кнопки «Переподключиться» — лаунчер пишет его в config/ при каждом
 * запуске игры (см. MinecraftLauncher src/main/game.js), сюда же кладёт офлайн-ник. DominoAuth
 * теперь пускает на сервер только по разовому пропуску от /auth/clear (см. onPreLogin) — прямой
 * реконнект в обход лаунчера без этого шага был бы кикнут, поэтому кнопка сама предъявляет
 * токен серверу перед тем как звать ConnectScreen.
 */
final class ReconnectClearance {
    private ReconnectClearance() {}

    private static final String AUTH_API = "http://45.93.200.45:18582"; // тот же адрес, что launcher.config.json authApi
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static Path sessionFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("dominocraft-session.txt");
    }

    /**
     * Лучшая попытка, без исключений наружу: нет файла (DominoAuth не настроен на этом
     * сервере, или сборка не через launcher.config.json authApi), нет сети, сервер отказал —
     * во всех случаях просто отдаём управление реконнекту как есть. Если гейт всё-таки не
     * пройден, сервер сам кикнет с понятной причиной — блокировать сам реконнект незачем.
     */
    static void clearBestEffort() {
        List<String> lines;
        try {
            lines = Files.readAllLines(sessionFile());
        } catch (IOException e) {
            return;
        }
        if (lines.size() < 2) return;
        String name = lines.get(0);
        String token = lines.get(1);
        try {
            String body = "name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                    + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(AUTH_API + "/auth/clear"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // сеть недоступна/таймаут — не блокируем реконнект
        }
    }
}
