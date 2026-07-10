package ru.vasenka.dominocitiesui;

import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Клиентский кэш состояния города (обновляется приёмниками пакетов).
 * Разбор — DataInputStream, формат зеркалит серверный PacketHandler.
 * Методы on* вызываются уже в клиентском потоке (через client.execute).
 */
public final class CityData {
    private CityData() {}

    // role: 0 = житель, 1 = офицер, 2 = мэр
    public record Member(String uuid, String name, byte role) {
        public boolean isMayor() { return role == 2; }
        public boolean isOfficer() { return role == 1; }
    }
    public record TopEntry(String name, int members, long score) {}
    public record CityInfo(String name, boolean open, String mayor, List<String> memberNames) {}
    public record ResourceEntry(String material, int count) {}
    public record ContractInfo(String cityName, String requiredMaterial, int requiredAmount,
                                String rewardMaterial, int rewardAmount,
                                String world, int x, int y, int z) {}

    public static boolean protocolMismatch = false;

    public static boolean hasCity = false;
    public static String cityName = "";
    public static String mayorName = "";
    public static int radius = 0;
    public static long score = 0;
    public static String coreWorld = "";
    public static int coreX = 0;
    public static int coreZ = 0;
    public static boolean isMayor = false;
    public static boolean isOfficer = false;
    public static final List<Member> members = new ArrayList<>();
    public static long points = 0;
    public static String specialization = ""; // пусто = не выбрана
    public static int resourceStock = 0;
    public static final List<String> buffs = new ArrayList<>();
    public static boolean open = false;
    public static String mayorTitle = "Мэр";
    public static String officerTitle = "Офицер";
    public static String memberTitle = "Житель";

    public static final List<TopEntry> top = new ArrayList<>();
    public static final List<CityInfo> directory = new ArrayList<>();
    public static final List<ResourceEntry> resources = new ArrayList<>();
    public static final List<ContractInfo> contracts = new ArrayList<>();

    public static String lastResult = "";
    public static boolean lastOk = true;

    public static void onState(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            protocolMismatch = false;
            hasCity = in.readBoolean();
            members.clear();
            if (hasCity) {
                cityName = in.readUTF();
                mayorName = in.readUTF();
                radius = in.readInt();
                score = in.readLong();
                coreWorld = in.readUTF();
                coreX = in.readInt();
                coreZ = in.readInt();
                isMayor = in.readBoolean();
                isOfficer = in.readBoolean();
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    String uuid = in.readUTF();
                    String name = in.readUTF();
                    byte role = in.readByte();
                    members.add(new Member(uuid, name, role));
                }
                points = in.readLong();
                specialization = in.readUTF();
                resourceStock = in.readInt();
                buffs.clear();
                int buffCount = in.readInt();
                for (int i = 0; i < buffCount; i++) buffs.add(in.readUTF());
                open = in.readBoolean();
                mayorTitle = in.readUTF();
                officerTitle = in.readUTF();
                memberTitle = in.readUTF();
            }
        } catch (Exception ignored) { /* битый пакет — молча */ }
        refresh();
    }

    public static void onTop(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            top.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String name = in.readUTF();
                int m = in.readInt();
                long sc = in.readLong();
                top.add(new TopEntry(name, m, sc));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onResult(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            lastOk = in.readBoolean();
            lastResult = in.readUTF();
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onDirectory(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            directory.clear();
            int cityCount = in.readInt();
            for (int i = 0; i < cityCount; i++) {
                String name = in.readUTF();
                boolean cityOpen = in.readBoolean();
                String mayor = in.readUTF();
                int memberCount = in.readInt();
                List<String> names = new ArrayList<>();
                for (int j = 0; j < memberCount; j++) names.add(in.readUTF());
                directory.add(new CityInfo(name, cityOpen, mayor, names));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onResources(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            resources.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String material = in.readUTF();
                int count = in.readInt();
                resources.add(new ResourceEntry(material, count));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onContracts(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != Protocol.VERSION) { protocolMismatch = true; refresh(); return; }
            contracts.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String cityName = in.readUTF();
                String reqMat = in.readUTF();
                int reqAmt = in.readInt();
                String rewMat = in.readUTF();
                int rewAmt = in.readInt();
                String world = in.readUTF();
                int x = in.readInt(), y = in.readInt(), z = in.readInt();
                contracts.add(new ContractInfo(cityName, reqMat, reqAmt, rewMat, rewAmt, world, x, y, z));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    /** Если открыт экран городов — перестроить его под свежие данные. */
    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CityScreen screen) {
            screen.refresh();
        }
    }
}
