package ru.vasenka.dominocitiesui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

/** Ненавязчивая подпись названия города вверху экрана, пока игрок внутри своей границы. */
public class CityHud implements HudElement {

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker tracker) {
        if (!CityData.hasCity || CityData.cityName.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        g.centeredText(mc.font, Component.literal("§6" + CityData.cityName), cx, 4, 0xB0FFFFFF);
    }
}
