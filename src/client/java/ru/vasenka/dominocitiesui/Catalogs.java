package ru.vasenka.dominocitiesui;

import java.util.List;

/** Зеркало серверных BuffCatalog/Specializations — только то, что нужно для отрисовки окна. */
public final class Catalogs {
    private Catalogs() {}

    public record Buff(String id, String displayName, long cost) {}
    public record Spec(String id, String displayName) {}

    public static final List<Buff> BUFFS = List.of(
            new Buff("regen", "Регенерация", 300),
            new Buff("haste", "Скорость добычи", 250),
            new Buff("speed", "Скорость", 200),
            new Buff("saturation", "Сытость", 400),
            new Buff("resistance", "Стойкость", 500)
    );

    public static final List<Spec> SPECS = List.of(
            new Spec("mining", "Шахтёрский"),
            new Spec("farming", "Фермерский"),
            new Spec("lumber", "Лесной"),
            new Spec("fishing", "Рыболовный")
    );

    public static Buff buff(String id) {
        for (Buff b : BUFFS) if (b.id().equals(id)) return b;
        return null;
    }

    public static String specName(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s.displayName();
        return id;
    }
}
