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

    /** id — имя Bukkit Material (как шлётся по сети), displayName — для UI. Для контрактов. */
    public record Resource(String id, String displayName) {}

    public static final List<Resource> RESOURCES = List.of(
            new Resource("COAL", "Уголь"),
            new Resource("RAW_IRON", "Необработанное железо"),
            new Resource("IRON_INGOT", "Железный слиток"),
            new Resource("RAW_COPPER", "Необработанная медь"),
            new Resource("COPPER_INGOT", "Медный слиток"),
            new Resource("RAW_GOLD", "Необработанное золото"),
            new Resource("GOLD_INGOT", "Золотой слиток"),
            new Resource("DIAMOND", "Алмаз"),
            new Resource("EMERALD", "Изумруд"),
            new Resource("REDSTONE", "Редстоун"),
            new Resource("LAPIS_LAZULI", "Лазурит"),
            new Resource("QUARTZ", "Кварц"),
            new Resource("NETHERITE_SCRAP", "Незеритовый лом"),
            new Resource("WHEAT", "Пшеница"),
            new Resource("BREAD", "Хлеб"),
            new Resource("CARROT", "Морковь"),
            new Resource("POTATO", "Картофель"),
            new Resource("BEETROOT", "Свёкла"),
            new Resource("APPLE", "Яблоко"),
            new Resource("COD", "Треска"),
            new Resource("SALMON", "Лосось"),
            new Resource("OAK_LOG", "Дубовое бревно"),
            new Resource("SPRUCE_LOG", "Еловое бревно"),
            new Resource("BIRCH_LOG", "Берёзовое бревно"),
            new Resource("JUNGLE_LOG", "Тропическое бревно"),
            new Resource("OAK_PLANKS", "Дубовые доски"),
            new Resource("COBBLESTONE", "Булыжник"),
            new Resource("STONE", "Камень"),
            new Resource("DIRT", "Земля"),
            new Resource("SAND", "Песок"),
            new Resource("GRAVEL", "Гравий"),
            new Resource("GLASS", "Стекло"),
            new Resource("STRING", "Нить"),
            new Resource("LEATHER", "Кожа"),
            new Resource("GUNPOWDER", "Порох"),
            new Resource("ENDER_PEARL", "Жемчуг Края"),
            new Resource("BLAZE_ROD", "Стержень ифрита"),
            new Resource("GLOWSTONE_DUST", "Светящаяся пыль")
    );

    /** Русское имя по id материала; если материала нет в каталоге — вернёт как есть (напр. для старых записей). */
    public static String resourceName(String materialId) {
        for (Resource r : RESOURCES) if (r.id().equals(materialId)) return r.displayName();
        return materialId;
    }
}
