package ru.vasenka.dominocitiesui;

/**
 * Зеркало серверного ru.vasenka.dominocities.Protocol — держать синхронно!
 * Формат байтов на обеих сторонах — java.io.DataInputStream/DataOutputStream.
 */
public final class Protocol {
    private Protocol() {}

    public static final int VERSION = 16;

    public static final String NS = "dominocities";
    public static final String CH_ACTION    = "action";
    public static final String CH_STATE     = "state";
    public static final String CH_TOP       = "top";
    public static final String CH_RESULT    = "result";
    public static final String CH_DIRECTORY = "directory";
    public static final String CH_RESOURCES = "resources";
    public static final String CH_CONTRACTS = "contracts";
    public static final String CH_BOUNTIES  = "bounties";
    public static final String CH_MARKET    = "market";
    public static final String CH_BUILDINGS = "buildings";
    public static final String CH_CITY_MAP  = "citymap";
    public static final String CH_EVENTS    = "events";

    public static final byte A_REQUEST_STATE = 0;
    public static final byte A_REQUEST_TOP   = 1;
    public static final byte A_CREATE        = 2;
    public static final byte A_INVITE        = 3;
    public static final byte A_JOIN          = 4;
    public static final byte A_LEAVE         = 5;
    public static final byte A_KICK          = 6;
    public static final byte A_DISBAND       = 7;
    public static final byte A_TOGGLE_BORDER = 8;
    public static final byte A_BUY_BUFF      = 9;
    public static final byte A_PROMOTE       = 12;
    public static final byte A_DEMOTE        = 13;
    public static final byte A_TRANSFER      = 14;
    public static final byte A_TOGGLE_OPEN   = 15;
    public static final byte A_SET_TITLE     = 16;
    public static final byte A_REQUEST_DIRECTORY = 17;
    public static final byte A_REQUEST_RESOURCES = 18;
    public static final byte A_BUILD_ROAD        = 19;
    public static final byte A_REQUEST_CONTRACTS = 20;
    public static final byte A_CREATE_CONTRACT   = 21;
    public static final byte A_TAKE_CONTRACT     = 22;
    public static final byte A_REQUEST_BOUNTIES  = 23;
    public static final byte A_CREATE_BOUNTY     = 24;
    public static final byte A_TAKE_BOUNTY       = 25;
    public static final byte A_REQUEST_MARKET    = 26;
    public static final byte A_LIST_ITEM         = 27;
    public static final byte A_MARKET_INTEREST   = 28;
    public static final byte A_MARKET_CANCEL     = 29;
    public static final byte A_REQUEST_BUILDINGS = 30;
    public static final byte A_BUILDING_WAND     = 31;
    public static final byte A_BUILDING_SAVE     = 32;
    public static final byte A_BUILDING_DELETE   = 33;
    public static final byte A_RATE_CITY         = 34;
    public static final byte A_ADD_COMMENT       = 35;
    public static final byte A_DELETE_COMMENT    = 36;
    public static final byte A_SET_CITY_DESCRIPTION = 37;
    public static final byte A_ADD_LAW    = 38;
    public static final byte A_DELETE_LAW = 39;
    public static final byte A_REQUEST_CITY_MAP = 40;
    public static final byte A_REFRESH_MAP       = 41;
    public static final byte A_ADD_MARKER    = 42;
    public static final byte A_DELETE_MARKER = 43;
    public static final byte A_EXPAND_CITY   = 44;
    public static final byte A_RENAME_CITY   = 45;
    public static final byte A_REQUEST_EVENTS = 46;
    public static final byte A_CREATE_EVENT   = 47;
    public static final byte A_DELETE_EVENT   = 48;
    public static final byte A_TOGGLE_EVENT_PARTICIPATE = 49;
}
