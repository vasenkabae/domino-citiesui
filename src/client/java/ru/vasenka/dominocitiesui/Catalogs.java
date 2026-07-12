package ru.vasenka.dominocitiesui;

import java.util.ArrayList;
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
            // Руда/слитки/самоцветы
            new Resource("COAL", "Уголь"),
            new Resource("CHARCOAL", "Древесный уголь"),
            new Resource("RAW_IRON", "Необработанное железо"),
            new Resource("IRON_INGOT", "Железный слиток"),
            new Resource("IRON_NUGGET", "Кусочек железа"),
            new Resource("RAW_COPPER", "Необработанная медь"),
            new Resource("COPPER_INGOT", "Медный слиток"),
            new Resource("RAW_GOLD", "Необработанное золото"),
            new Resource("GOLD_INGOT", "Золотой слиток"),
            new Resource("GOLD_NUGGET", "Кусочек золота"),
            new Resource("DIAMOND", "Алмаз"),
            new Resource("EMERALD", "Изумруд"),
            new Resource("REDSTONE", "Редстоун"),
            new Resource("LAPIS_LAZULI", "Лазурит"),
            new Resource("QUARTZ", "Кварц"),
            new Resource("AMETHYST_SHARD", "Осколок аметиста"),
            new Resource("NETHERITE_SCRAP", "Незеритовый лом"),
            new Resource("NETHERITE_INGOT", "Незеритовый слиток"),
            new Resource("ANCIENT_DEBRIS", "Древние обломки"),
            new Resource("GLOWSTONE_DUST", "Светящаяся пыль"),
            new Resource("GUNPOWDER", "Порох"),
            new Resource("FLINT", "Кремень"),
            new Resource("CLAY_BALL", "Ком глины"),
            new Resource("BRICK", "Кирпич"),

            // Еда
            new Resource("WHEAT", "Пшеница"),
            new Resource("BREAD", "Хлеб"),
            new Resource("CARROT", "Морковь"),
            new Resource("GOLDEN_CARROT", "Золотая морковь"),
            new Resource("POTATO", "Картофель"),
            new Resource("BAKED_POTATO", "Печёный картофель"),
            new Resource("BEETROOT", "Свёкла"),
            new Resource("APPLE", "Яблоко"),
            new Resource("GOLDEN_APPLE", "Золотое яблоко"),
            new Resource("MELON_SLICE", "Ломтик арбуза"),
            new Resource("PUMPKIN_PIE", "Тыквенный пирог"),
            new Resource("SUGAR", "Сахар"),
            new Resource("EGG", "Яйцо"),
            new Resource("MILK_BUCKET", "Ведро молока"),
            new Resource("HONEY_BOTTLE", "Бутылка мёда"),
            new Resource("COOKIE", "Печенье"),
            new Resource("CAKE", "Торт"),
            new Resource("BEEF", "Сырая говядина"),
            new Resource("COOKED_BEEF", "Стейк"),
            new Resource("PORKCHOP", "Сырая свинина"),
            new Resource("COOKED_PORKCHOP", "Жареная свинина"),
            new Resource("CHICKEN", "Сырая курятина"),
            new Resource("COOKED_CHICKEN", "Жареная курятина"),
            new Resource("MUTTON", "Сырая баранина"),
            new Resource("COOKED_MUTTON", "Жареная баранина"),
            new Resource("RABBIT", "Сырая крольчатина"),
            new Resource("COD", "Треска"),
            new Resource("COOKED_COD", "Жареная треска"),
            new Resource("SALMON", "Лосось"),
            new Resource("COOKED_SALMON", "Жареный лосось"),

            // Дерево
            new Resource("OAK_LOG", "Дубовое бревно"),
            new Resource("SPRUCE_LOG", "Еловое бревно"),
            new Resource("BIRCH_LOG", "Берёзовое бревно"),
            new Resource("JUNGLE_LOG", "Тропическое бревно"),
            new Resource("ACACIA_LOG", "Бревно акации"),
            new Resource("DARK_OAK_LOG", "Бревно тёмного дуба"),
            new Resource("MANGROVE_LOG", "Бревно мангра"),
            new Resource("CHERRY_LOG", "Бревно вишни"),
            new Resource("OAK_PLANKS", "Дубовые доски"),
            new Resource("SPRUCE_PLANKS", "Еловые доски"),
            new Resource("BIRCH_PLANKS", "Берёзовые доски"),
            new Resource("BAMBOO", "Бамбук"),
            new Resource("STICK", "Палка"),

            // Камень и стройматериалы
            new Resource("COBBLESTONE", "Булыжник"),
            new Resource("STONE", "Камень"),
            new Resource("DEEPSLATE", "Глубинный сланец"),
            new Resource("ANDESITE", "Андезит"),
            new Resource("DIORITE", "Диорит"),
            new Resource("GRANITE", "Гранит"),
            new Resource("SANDSTONE", "Песчаник"),
            new Resource("DIRT", "Земля"),
            new Resource("GRASS_BLOCK", "Блок травы"),
            new Resource("SAND", "Песок"),
            new Resource("GRAVEL", "Гравий"),
            new Resource("GLASS", "Стекло"),
            new Resource("OBSIDIAN", "Обсидиан"),
            new Resource("NETHERRACK", "Незернит"),
            new Resource("BLACKSTONE", "Чёрный камень"),
            new Resource("TERRACOTTA", "Терракота"),
            new Resource("MUD", "Грязь"),

            // Дроп мобов / редкие
            new Resource("STRING", "Нить"),
            new Resource("SPIDER_EYE", "Паучий глаз"),
            new Resource("BONE", "Кость"),
            new Resource("BONE_MEAL", "Костная мука"),
            new Resource("ROTTEN_FLESH", "Гнилая плоть"),
            new Resource("LEATHER", "Кожа"),
            new Resource("RABBIT_HIDE", "Кроличья шкура"),
            new Resource("FEATHER", "Перо"),
            new Resource("INK_SAC", "Чернильный мешок"),
            new Resource("SLIME_BALL", "Шарик слизи"),
            new Resource("MAGMA_CREAM", "Тягучая масса"),
            new Resource("ENDER_PEARL", "Жемчуг Края"),
            new Resource("ENDER_EYE", "Око Края"),
            new Resource("BLAZE_ROD", "Стержень ифрита"),
            new Resource("BLAZE_POWDER", "Огненный порошок"),
            new Resource("GHAST_TEAR", "Слеза гаста"),
            new Resource("PHANTOM_MEMBRANE", "Перепонка фантома"),
            new Resource("SHULKER_SHELL", "Панцирь шалкера"),
            new Resource("NAUTILUS_SHELL", "Раковина наутилуса"),
            new Resource("SLIME_BLOCK", "Блок слизи"),
            new Resource("PRISMARINE_SHARD", "Осколок призматина"),
            new Resource("PRISMARINE_CRYSTALS", "Кристаллы призматина"),
            new Resource("NETHER_STAR", "Звезда Незера"),
            new Resource("NETHER_WART", "Адский нарост"),
            new Resource("CHORUS_FRUIT", "Плод хоруса"),
            new Resource("COCOA_BEANS", "Какао-бобы"),
            new Resource("SUGAR_CANE", "Сахарный тростник"),
            new Resource("WHEAT_SEEDS", "Семена пшеницы"),
            new Resource("PUMPKIN_SEEDS", "Семена тыквы"),
            new Resource("MELON_SEEDS", "Семена арбуза"),

            // Редстоун-механизмы
            new Resource("PISTON", "Поршень"),
            new Resource("STICKY_PISTON", "Липкий поршень"),
            new Resource("REDSTONE_TORCH", "Редстоун-факел"),
            new Resource("REPEATER", "Повторитель"),
            new Resource("COMPARATOR", "Компаратор"),
            new Resource("OBSERVER", "Наблюдатель"),
            new Resource("HOPPER", "Воронка"),
            new Resource("DISPENSER", "Раздатчик")
    );

    /** Русское имя по id материала; если материала нет в каталоге — вернёт как есть (напр. для старых записей). */
    public static String resourceName(String materialId) {
        for (Resource r : RESOURCES) if (r.id().equals(materialId)) return r.displayName();
        return materialId;
    }

    /** Поиск по русскому названию или id (регистронезависимо, подстрока), не более limit результатов. */
    public static List<Resource> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Resource> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (Resource r : RESOURCES) {
            if (out.size() >= limit) break;
            if (r.displayName().toLowerCase().contains(q) || r.id().toLowerCase().contains(q)) out.add(r);
        }
        return out;
    }
}
