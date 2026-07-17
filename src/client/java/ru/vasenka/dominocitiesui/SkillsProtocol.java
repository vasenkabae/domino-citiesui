package ru.vasenka.dominocitiesui;

/**
 * Зеркало серверного ru.vasenka.dominoskills.SkillsProtocol — держать синхронно!
 * Версия НЕЗАВИСИМА от протокола городов (Protocol.VERSION).
 */
public final class SkillsProtocol {
    private SkillsProtocol() {}

    public static final int VERSION = 5;

    public static final String NS = "dominoskills";
    public static final String CH_ACTION = "action";
    public static final String CH_STATE  = "state";
    public static final String CH_XP     = "xp";
    public static final String CH_RESULT = "result";

    public static final byte A_REQUEST_STATE    = 0;
    public static final byte A_LEARN            = 1;
    public static final byte A_RESET            = 2;
    public static final byte A_ACTIVATE         = 3;
    public static final byte A_TOGGLE_LIGHTHAND = 4;
}
