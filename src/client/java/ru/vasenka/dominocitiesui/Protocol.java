package ru.vasenka.dominocitiesui;

/**
 * Зеркало серверного ru.vasenka.dominocities.Protocol — держать синхронно!
 * Формат байтов на обеих сторонах — java.io.DataInputStream/DataOutputStream.
 */
public final class Protocol {
    private Protocol() {}

    public static final int VERSION = 1;

    public static final String NS = "dominocities";
    public static final String CH_ACTION = "action";
    public static final String CH_STATE  = "state";
    public static final String CH_TOP    = "top";
    public static final String CH_RESULT = "result";

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
    public static final byte A_SET_SPEC      = 10;
    public static final byte A_COLLECT       = 11;
    public static final byte A_PROMOTE       = 12;
    public static final byte A_DEMOTE        = 13;
    public static final byte A_TRANSFER      = 14;
}
