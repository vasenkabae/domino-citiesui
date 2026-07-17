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
    public record ContractInfo(int id, String cityName, String requiredMaterial, int requiredAmount,
                                String rewardMaterial, int rewardAmount) {}
    public record BountyInfo(int id, String targetName, String rewardMaterial, int rewardAmount, boolean claimed) {}
    /** Моя активная охота (если я — назначенный охотник на кого-то). hasCoords=false — ещё не раскрывалось. */
    public record MyHunt(String targetName, boolean hasCoords, String world, int x, int y, int z, long lastRevealAt) {}
    /**
     * Лот рынка. item — уже разобранный на клиенте ItemStack (см. MarketItemDecoder), с зачарованиями и
     * прочим NBT — рендерится как настоящий ванильный предмет (иконка + тултип). interestedNames заполнен,
     * только если mine=true (свой лот) — иначе виден только interestedCount.
     */
    public record MarketListingInfo(int id, String sellerName, net.minecraft.world.item.ItemStack item,
                                     String priceText, int interestedCount, boolean mine,
                                     List<String> interestedNames) {}
    /** Постройка города (витрина). canDelete сервер вычисляет под получателя (владелец/мэр/офицер). */
    public record BuildingInfo(int id, String name, String ownerName, String description, long createdAt,
                                String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                String photoId, boolean canDelete) {}
    /** Комментарий к городу. canDelete сервер вычисляет под получателя (автор/мэр/офицер). */
    public record CommentInfo(int id, String authorName, String text, long createdAt, boolean canDelete) {}
    /** Пункт свода законов города. */
    public record LawInfo(int id, String text, long createdAt) {}
    /** Контур города для мини-карты. */
    public record MapCityInfo(String name, String world, int x, int z, int radius, boolean mine) {}
    /** Живая позиция онлайн-жителя своего города (не видно чужих городов). */
    public record TeammateInfo(String name, String world, int x, int z) {}
    /** Личная метка на карте — видна только владельцу. */
    public record MarkerInfo(int id, String name, String world, int x, int z, long createdAt) {}
    /** Постройка из витрины города — метка на карте мира (наведение: название + фото). */
    public record MapBuildingInfo(String name, String cityName, String world, int x, int z, String photoId) {}
    /** Ивент, созданный игроком — вкладка «Ивенты» в K-меню. */
    public record EventInfo(int id, String title, String description, String creatorName, String creatorCity,
                             String world, int x, int y, int z, long createdAt, boolean canDelete) {}

    public static boolean protocolMismatch = false;
    public static int lastReceivedVersion = -1;

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
    public static final List<String> buffs = new ArrayList<>();
    public static boolean open = false;
    public static String mayorTitle = "Мэр";
    public static String officerTitle = "Офицер";
    public static String memberTitle = "Житель";
    public static String description = "";
    // Платное расширение границы (config radius.expand): докуплено / потолок / шаг / цена.
    public static int boughtRadius = 0;
    public static int expandMax = 0;
    public static int expandStep = 16;
    public static int expandCost = 15;

    public static final List<TopEntry> top = new ArrayList<>();
    public static final List<CityInfo> directory = new ArrayList<>();
    public static final List<ResourceEntry> resources = new ArrayList<>();
    public static final List<ContractInfo> contracts = new ArrayList<>();
    public static final List<BountyInfo> bounties = new ArrayList<>();
    public static MyHunt myHunt = null; // null = сейчас ни за кем не охочусь
    public static final List<MarketListingInfo> market = new ArrayList<>();
    // Постройки/рейтинг/комментарии/описание последнего запрошенного города (карточка во «Все города»).
    public static String buildingsCity = "";
    public static final List<BuildingInfo> buildings = new ArrayList<>();
    public static String cardDescription = "";
    public static long cardLikes = 0, cardDislikes = 0;
    public static byte cardMyVote = 0; // 0 = не голосовал, 1 = лайк, 2 = дизлайк
    public static boolean cardCanRate = false;
    public static final List<CommentInfo> cardComments = new ArrayList<>();
    public static boolean cardCanEditLaws = false;
    public static final List<LawInfo> cardLaws = new ArrayList<>();

    // Плоская карта городов (вкладка «Карта»).
    public static boolean mapHasImage = false;
    public static long mapVersion = 0;
    public static String mapWorld = "";
    public static int mapMinX, mapMinZ, mapBlockSize, mapWidth, mapHeight;
    public static boolean mapInProgress = false;
    public static int mapCooldownSeconds = 0;
    public static final List<MapCityInfo> mapCities = new ArrayList<>();
    public static final List<TeammateInfo> mapTeammates = new ArrayList<>();
    public static final List<MarkerInfo> mapMarkers = new ArrayList<>();
    public static final List<MapBuildingInfo> mapBuildings = new ArrayList<>();

    // Ивенты (вкладка «Ивенты» K-меню).
    public static final List<EventInfo> events = new ArrayList<>();

    public static String lastResult = "";
    public static boolean lastOk = true;

    public static void onState(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
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
                buffs.clear();
                int buffCount = in.readInt();
                for (int i = 0; i < buffCount; i++) buffs.add(in.readUTF());
                open = in.readBoolean();
                mayorTitle = in.readUTF();
                officerTitle = in.readUTF();
                memberTitle = in.readUTF();
                description = in.readUTF();
                boughtRadius = in.readInt();
                expandMax = in.readInt();
                expandStep = in.readInt();
                expandCost = in.readInt();
            }
        } catch (Exception ignored) { /* битый пакет — молча */ }
        refresh();
    }

    public static void onTop(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
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
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            lastOk = in.readBoolean();
            lastResult = in.readUTF();
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onDirectory(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
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
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
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
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            contracts.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                String cityName = in.readUTF();
                String reqMat = in.readUTF();
                int reqAmt = in.readInt();
                String rewMat = in.readUTF();
                int rewAmt = in.readInt();
                contracts.add(new ContractInfo(id, cityName, reqMat, reqAmt, rewMat, rewAmt));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onBounties(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            bounties.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                String targetName = in.readUTF();
                String rewMat = in.readUTF();
                int rewAmt = in.readInt();
                boolean claimed = in.readBoolean();
                bounties.add(new BountyInfo(id, targetName, rewMat, rewAmt, claimed));
            }
            boolean hasHunt = in.readBoolean();
            if (hasHunt) {
                String targetName = in.readUTF();
                boolean hasCoords = in.readBoolean();
                if (hasCoords) {
                    String world = in.readUTF();
                    int x = in.readInt(), y = in.readInt(), z = in.readInt();
                    long lastRevealAt = in.readLong();
                    myHunt = new MyHunt(targetName, true, world, x, y, z, lastRevealAt);
                } else {
                    myHunt = new MyHunt(targetName, false, "", 0, 0, 0, 0);
                }
            } else {
                myHunt = null;
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onMarket(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            market.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                String sellerName = in.readUTF();
                int itemLen = in.readInt();
                byte[] itemBytes = new byte[itemLen];
                in.readFully(itemBytes);
                var item = MarketItemDecoder.decode(itemBytes);
                String priceText = in.readUTF();
                int interestedCount = in.readInt();
                boolean mine = in.readBoolean();
                List<String> names = new ArrayList<>();
                if (mine) for (int j = 0; j < interestedCount; j++) names.add(in.readUTF());
                market.add(new MarketListingInfo(id, sellerName, item, priceText, interestedCount, mine, names));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onBuildings(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            buildingsCity = in.readUTF();
            cardDescription = in.readUTF();
            buildings.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                String name = in.readUTF();
                String ownerName = in.readUTF();
                String bDesc = in.readUTF();
                long createdAt = in.readLong();
                String world = in.readUTF();
                int minX = in.readInt(), minY = in.readInt(), minZ = in.readInt();
                int maxX = in.readInt(), maxY = in.readInt(), maxZ = in.readInt();
                String photoId = in.readUTF();
                boolean canDelete = in.readBoolean();
                buildings.add(new BuildingInfo(id, name, ownerName, bDesc, createdAt,
                        world, minX, minY, minZ, maxX, maxY, maxZ, photoId, canDelete));
            }
            cardLikes = in.readLong();
            cardDislikes = in.readLong();
            cardMyVote = in.readByte();
            cardCanRate = in.readBoolean();
            cardComments.clear();
            int cn = in.readInt();
            for (int i = 0; i < cn; i++) {
                int id = in.readInt();
                String authorName = in.readUTF();
                String text = in.readUTF();
                long createdAt = in.readLong();
                boolean canDelete = in.readBoolean();
                cardComments.add(new CommentInfo(id, authorName, text, createdAt, canDelete));
            }
            cardCanEditLaws = in.readBoolean();
            cardLaws.clear();
            int ln = in.readInt();
            for (int i = 0; i < ln; i++) {
                int id = in.readInt();
                String text = in.readUTF();
                long createdAt = in.readLong();
                cardLaws.add(new LawInfo(id, text, createdAt));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onCityMap(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            mapHasImage = in.readBoolean();
            if (mapHasImage) {
                mapVersion = in.readLong();
                mapWorld = in.readUTF();
                mapMinX = in.readInt();
                mapMinZ = in.readInt();
                mapBlockSize = in.readInt();
                mapWidth = in.readInt();
                mapHeight = in.readInt();
            }
            mapInProgress = in.readBoolean();
            mapCooldownSeconds = in.readInt();
            mapCities.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String name = in.readUTF();
                String world = in.readUTF();
                int x = in.readInt(), z = in.readInt(), radius = in.readInt();
                boolean mine = in.readBoolean();
                mapCities.add(new MapCityInfo(name, world, x, z, radius, mine));
            }

            mapTeammates.clear();
            int tn = in.readInt();
            for (int i = 0; i < tn; i++) {
                String tName = in.readUTF();
                String tWorld = in.readUTF();
                int tx = in.readInt(), tz = in.readInt();
                mapTeammates.add(new TeammateInfo(tName, tWorld, tx, tz));
            }

            mapMarkers.clear();
            int mn = in.readInt();
            for (int i = 0; i < mn; i++) {
                int id = in.readInt();
                String mName = in.readUTF();
                String mWorld = in.readUTF();
                int mx = in.readInt(), mz = in.readInt();
                long createdAt = in.readLong();
                mapMarkers.add(new MarkerInfo(id, mName, mWorld, mx, mz, createdAt));
            }

            mapBuildings.clear();
            int bn = in.readInt();
            for (int i = 0; i < bn; i++) {
                String bName = in.readUTF();
                String bCity = in.readUTF();
                String bWorld = in.readUTF();
                int bx = in.readInt(), bz = in.readInt();
                String photoId = in.readUTF();
                mapBuildings.add(new MapBuildingInfo(bName, bCity, bWorld, bx, bz, photoId));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    public static void onEvents(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int ver = in.readInt();
            if (ver != Protocol.VERSION) { protocolMismatch = true; lastReceivedVersion = ver; refresh(); return; }
            events.clear();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                String title = in.readUTF();
                String description = in.readUTF();
                String creatorName = in.readUTF();
                String creatorCity = in.readUTF();
                String world = in.readUTF();
                int x = in.readInt(), y = in.readInt(), z = in.readInt();
                long createdAt = in.readLong();
                boolean canDelete = in.readBoolean();
                events.add(new EventInfo(id, title, description, creatorName, creatorCity,
                        world, x, y, z, createdAt, canDelete));
            }
        } catch (Exception ignored) { }
        refresh();
    }

    /** Если открыт экран городов — перестроить его под свежие данные. */
    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CityScreen screen) {
            screen.refresh();
        } else if (mc.screen instanceof WorldMapScreen screen) {
            screen.refresh();
        }
    }
}
