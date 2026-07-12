package ru.vasenka.dominocitiesui;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентский стейт «ник недавно раскрыт ПКМ» — используется миксином рендера ника
 * (см. mixin.NameTagVisibilityMixin), чтобы показывать ник игрока только по клику, не всегда.
 * Чисто визуальный эффект, на сервер ничего не шлём, кроме фонового обновления directory
 * (нужного для показа города — см. cityOf).
 */
public final class NameReveal {
    private NameReveal() {}

    private static final long DURATION_MS = 3000;
    private static final long DIRECTORY_REFRESH_MS = 5000;

    private static final Map<UUID, Long> revealedUntil = new HashMap<>();
    private static long lastDirectoryRequest = 0;

    public static void reveal(Player target) {
        revealedUntil.put(target.getUUID(), System.currentTimeMillis() + DURATION_MS);

        long now = System.currentTimeMillis();
        if (now - lastDirectoryRequest > DIRECTORY_REFRESH_MS) {
            lastDirectoryRequest = now;
            CityActions.requestDirectory();
        }
    }

    public static boolean isRevealed(UUID uuid) {
        Long until = revealedUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    /** Город игрока по нику среди уже полученного справочника; null — не в городе или данных ещё нет. */
    public static String cityOf(String playerName) {
        for (CityData.CityInfo info : CityData.directory) {
            if (info.memberNames().contains(playerName)) return info.name();
        }
        return null;
    }
}
