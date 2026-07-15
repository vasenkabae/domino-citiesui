package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Клиентское состояние профессий (зеркало снапшотов dominoskills:*). */
public final class SkillsData {
    private SkillsData() {}

    public static final int PROF_COUNT = 6;
    public static final String[] PROF_TITLES =
            {"Шахтёр", "Фермер", "Охотник", "Рыбак", "Строитель", "Исследователь"};

    public static class ProfState {
        public long totalXp;
        public int level;
        public int xpInto;
        public int xpNeed; // 0 = максимальный уровень
    }

    public static final ProfState[] prof = new ProfState[PROF_COUNT];
    static {
        for (int i = 0; i < PROF_COUNT; i++) prof[i] = new ProfState();
    }

    public static int pointsEarned, pointsSpent, pointsCap, resetCost;
    /** talentId → изученный ранг. */
    public static final Map<String, Integer> talents = new HashMap<>();

    public static boolean lastOk;
    public static String lastResult = "";
    public static long lastResultTime;
    public static boolean protocolMismatch;
    public static int lastReceivedVersion;
    public static boolean stateLoaded;

    public static int pointsAvailable() {
        return Math.max(0, pointsEarned - pointsSpent);
    }

    public static int rank(String talentId) {
        Integer r = talents.get(talentId);
        return r == null ? 0 : r;
    }

    // ── Тосты для HUD ────────────────────────────────────────────────────────

    public static final class XpToast {
        public final int profId;
        public int amount;
        public long time;
        XpToast(int profId, int amount, long time) { this.profId = profId; this.amount = amount; this.time = time; }
    }

    public record LevelToast(int profId, int level, long time) {}

    public static final List<XpToast> xpToasts = new ArrayList<>();
    public static final List<LevelToast> levelToasts = new ArrayList<>();

    // ── Приём пакетов ────────────────────────────────────────────────────────

    public static void onState(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != SkillsProtocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; return; }
            for (int i = 0; i < PROF_COUNT; i++) {
                prof[i].totalXp = in.readLong();
                prof[i].level = in.readInt();
                prof[i].xpInto = in.readInt();
                prof[i].xpNeed = in.readInt();
            }
            pointsEarned = in.readInt();
            pointsSpent = in.readInt();
            pointsCap = in.readInt();
            resetCost = in.readInt();
            talents.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String id = in.readUTF();
                int rank = in.readByte();
                talents.put(id, rank);
            }
            stateLoaded = true;
            protocolMismatch = false;
        } catch (Exception ignored) { /* битый пакет — молча */ }
    }

    public static void onXp(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != SkillsProtocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; return; }
            long now = System.currentTimeMillis();
            int count = in.readByte();
            for (int i = 0; i < count; i++) {
                int profId = in.readByte();
                int gained = in.readInt();
                int level = in.readInt();
                int into = in.readInt();
                int need = in.readInt();
                boolean levelUp = in.readBoolean();
                if (profId >= 0 && profId < PROF_COUNT) {
                    prof[profId].level = level;
                    prof[profId].xpInto = into;
                    prof[profId].xpNeed = need;
                    prof[profId].totalXp += gained; // приблизительно; точное придёт со state
                    addXpToast(profId, gained, now);
                    if (levelUp) levelToasts.add(new LevelToast(profId, level, now));
                }
            }
            int available = in.readInt();
            pointsEarned = pointsSpent + available; // чтобы pointsAvailable() показывал живое значение
        } catch (Exception ignored) { }
    }

    /** Свежие начисления по той же профессии сливаются в один тост, а не стопку строк. */
    private static void addXpToast(int profId, int gained, long now) {
        for (XpToast t : xpToasts) {
            if (t.profId == profId && now - t.time < 1500) {
                t.amount += gained;
                t.time = now;
                return;
            }
        }
        xpToasts.add(new XpToast(profId, gained, now));
    }

    public static void onResult(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != SkillsProtocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; return; }
            lastOk = in.readBoolean();
            lastResult = in.readUTF();
            lastResultTime = System.currentTimeMillis();
        } catch (Exception ignored) { }
        // Экран читает статику каждый кадр — отдельного refresh не требуется.
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SkillScreen s) s.onResult();
    }
}
