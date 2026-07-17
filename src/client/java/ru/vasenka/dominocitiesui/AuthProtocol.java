package ru.vasenka.dominocitiesui;

/**
 * Зеркало серверного ru.vasenka.dominoauth.AuthProtocol — держать синхронно!
 * Версия НЕЗАВИСИМА от протоколов городов/профессий. Если сервер вообще не отвечает на
 * A_REQUEST_STATE (нет плагина DominoAuth или версия несовместима) — AuthData.stateReceived
 * остаётся false, и GUI-экран просто никогда не форсируется (чат-путь /login остаётся рабочим).
 */
public final class AuthProtocol {
    private AuthProtocol() {}

    public static final int VERSION = 1;

    public static final String NS = "dominoauth";
    public static final String CH_ACTION = "action";
    public static final String CH_STATE  = "state";
    public static final String CH_RESULT = "result";

    public static final byte A_REQUEST_STATE = 0;
    public static final byte A_REGISTER      = 1;
    public static final byte A_LOGIN         = 2;
}
