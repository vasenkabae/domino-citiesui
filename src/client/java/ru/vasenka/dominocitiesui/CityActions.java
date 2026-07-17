package ru.vasenka.dominocitiesui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Сборка и отправка действий клиент → сервер (канал dominocities:action). */
public final class CityActions {
    private CityActions() {}

    private static byte[] build(byte type, String... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(type);
            for (String a : args) out.writeUTF(a);
        } catch (IOException e) {
            throw new RuntimeException(e); // на ByteArrayOutputStream не бывает
        }
        return bos.toByteArray();
    }

    private static void send(byte type, String... args) {
        ClientPlayNetworking.send(new Payloads.Action(build(type, args)));
    }

    public static void requestState()      { send(Protocol.A_REQUEST_STATE); }
    public static void requestTop()         { send(Protocol.A_REQUEST_TOP); }
    public static void create(String name)  { send(Protocol.A_CREATE, name); }
    public static void invite(String nick)  { send(Protocol.A_INVITE, nick); }
    public static void join(String name)    { send(Protocol.A_JOIN, name); }
    public static void leave()              { send(Protocol.A_LEAVE); }
    public static void kick(String uuid)    { send(Protocol.A_KICK, uuid); }
    public static void disband()            { send(Protocol.A_DISBAND); }
    public static void toggleBorder()       { send(Protocol.A_TOGGLE_BORDER); }
    public static void expandCity()         { send(Protocol.A_EXPAND_CITY); }
    public static void renameCity(String newName) { send(Protocol.A_RENAME_CITY, newName); }
    public static void buyBuff(String id)   { send(Protocol.A_BUY_BUFF, id); }
    public static void promote(String uuid) { send(Protocol.A_PROMOTE, uuid); }
    public static void demote(String uuid)  { send(Protocol.A_DEMOTE, uuid); }
    public static void transfer(String uuid) { send(Protocol.A_TRANSFER, uuid); }
    public static void toggleOpen()         { send(Protocol.A_TOGGLE_OPEN); }
    public static void requestDirectory()   { send(Protocol.A_REQUEST_DIRECTORY); }
    public static void requestResources()   { send(Protocol.A_REQUEST_RESOURCES); }
    public static void buildRoad()          { send(Protocol.A_BUILD_ROAD); }
    public static void roadToCity(String cityName) { send(Protocol.A_ROAD_TO_CITY, cityName); }
    public static void requestContracts()   { send(Protocol.A_REQUEST_CONTRACTS); }
    public static void requestBounties()    { send(Protocol.A_REQUEST_BOUNTIES); }
    public static void requestMarket()      { send(Protocol.A_REQUEST_MARKET); }
    public static void requestBuildings(String cityName) { send(Protocol.A_REQUEST_BUILDINGS, cityName); }
    public static void requestBuildingWand() { send(Protocol.A_BUILDING_WAND); }
    public static void saveBuilding(String name, String description, String photoId) {
        send(Protocol.A_BUILDING_SAVE, name, description, photoId);
    }
    public static void deleteBuilding(String cityName, int buildingId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_BUILDING_DELETE);
            out.writeUTF(cityName);
            out.writeInt(buildingId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    /** vote: 0 = убрать оценку, 1 = лайк, 2 = дизлайк. */
    public static void rateCity(String cityName, byte vote) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_RATE_CITY);
            out.writeUTF(cityName);
            out.writeByte(vote);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void addComment(String cityName, String text) { send(Protocol.A_ADD_COMMENT, cityName, text); }

    public static void deleteComment(String cityName, int commentId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_DELETE_COMMENT);
            out.writeUTF(cityName);
            out.writeInt(commentId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void setCityDescription(String text) { send(Protocol.A_SET_CITY_DESCRIPTION, text); }

    public static void addLaw(String text) { send(Protocol.A_ADD_LAW, text); }

    public static void requestCityMap() { send(Protocol.A_REQUEST_CITY_MAP); }
    public static void refreshMap()     { send(Protocol.A_REFRESH_MAP); }

    public static void addMarker(String world, int x, int z, String name) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_ADD_MARKER);
            out.writeUTF(world);
            out.writeInt(x);
            out.writeInt(z);
            out.writeUTF(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void deleteMarker(int markerId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_DELETE_MARKER);
            out.writeInt(markerId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void deleteLaw(int lawId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_DELETE_LAW);
            out.writeInt(lawId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }
    public static void marketInterest(int listingId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_MARKET_INTEREST);
            out.writeInt(listingId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }
    public static void marketCancel(int listingId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_MARKET_CANCEL);
            out.writeInt(listingId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }
    public static void listItem(String priceText, int quantity) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_LIST_ITEM);
            out.writeUTF(priceText);
            out.writeInt(quantity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void setTitle(byte role, String title) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_SET_TITLE);
            out.writeByte(role);
            out.writeUTF(title);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void createContract(String requiredMaterial, int requiredAmount,
                                       String rewardMaterial, int rewardAmount) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_CREATE_CONTRACT);
            out.writeUTF(requiredMaterial);
            out.writeInt(requiredAmount);
            out.writeUTF(rewardMaterial);
            out.writeInt(rewardAmount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void takeContract(int contractId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_TAKE_CONTRACT);
            out.writeInt(contractId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void createBounty(String targetNick, String rewardMaterial, int rewardAmount) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_CREATE_BOUNTY);
            out.writeUTF(targetNick);
            out.writeUTF(rewardMaterial);
            out.writeInt(rewardAmount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void takeBounty(int bountyId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_TAKE_BOUNTY);
            out.writeInt(bountyId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void requestEvents() { send(Protocol.A_REQUEST_EVENTS); }

    /** Координаты берёт сервер (текущая позиция игрока) — клиент их не присылает. */
    public static void createEvent(String title, String description, String dateTime) {
        send(Protocol.A_CREATE_EVENT, title, description, dateTime);
    }

    public static void deleteEvent(int eventId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_DELETE_EVENT);
            out.writeInt(eventId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }

    public static void toggleEventParticipate(int eventId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(Protocol.VERSION);
            out.writeByte(Protocol.A_TOGGLE_EVENT_PARTICIPATE);
            out.writeInt(eventId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPlayNetworking.send(new Payloads.Action(bos.toByteArray()));
    }
}
