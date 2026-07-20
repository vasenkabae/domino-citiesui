package ru.vasenka.dominocitiesui;

/**
 * Зеркало серверного ru.vasenka.dominoauth.AuthProtocol — держать синхронно!
 * Версия НЕЗАВИСИМА от протоколов городов/профессий. Вход/регистрация больше не идут
 * по этому каналу — только через лаунчер; здесь остался только гейт правил сервера.
 */
public final class AuthProtocol {
    private AuthProtocol() {}

    public static final int VERSION = 3; // 2->3: убраны A_REGISTER/A_LOGIN/CH_RESULT

    public static final String NS = "dominoauth";
    public static final String CH_ACTION = "action";
    public static final String CH_STATE  = "state";

    public static final byte A_REQUEST_STATE = 0;
    public static final byte A_ACCEPT_RULES  = 3;
}
