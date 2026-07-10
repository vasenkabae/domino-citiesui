package ru.vasenka.dominocitiesui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

/** Ненавязчивая подпись названия города вверху экрана, ТОЛЬКО пока игрок внутри своей границы. */
public class CityHud implements HudElement {

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker tracker) {
        if (!CityData.hasCity || CityData.cityName.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        LocalPlayer p = mc.player;
        if (p == null || !insideCity(p)) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        g.centeredText(mc.font, Component.literal("§6" + CityData.cityName), cx, 4, 0xB0FFFFFF);
    }

    private boolean insideCity(LocalPlayer p) {
        if (!worldMatches(p.level().dimension(), CityData.coreWorld)) return false;
        double dx = p.getX() - CityData.coreX;
        double dz = p.getZ() - CityData.coreZ;
        double r = CityData.radius;
        return dx * dx + dz * dz <= r * r;
    }

    /** Bukkit хранит имя папки мира ("world"/"world_nether"/"world_the_end"), клиент — ResourceKey измерения. */
    private boolean worldMatches(ResourceKey<Level> dimension, String bukkitWorld) {
        return switch (bukkitWorld) {
            case "world" -> dimension.equals(Level.OVERWORLD);
            case "world_nether" -> dimension.equals(Level.NETHER);
            case "world_the_end" -> dimension.equals(Level.END);
            default -> false;
        };
    }
}
