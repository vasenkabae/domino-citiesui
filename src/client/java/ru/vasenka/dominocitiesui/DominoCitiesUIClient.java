package ru.vasenka.dominocitiesui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DominoCitiesUIClient implements ClientModInitializer {

    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        // Регистрация типов пакетов (иначе клиент не заявит каналы серверу и не примет их).
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Action.TYPE, Payloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.State.TYPE, Payloads.State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Top.TYPE, Payloads.Top.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Result.TYPE, Payloads.Result.CODEC);

        // Приём снапшотов (в клиентском потоке).
        ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onState(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Top.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onTop(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Result.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onResult(payload.data())));

        // Клавиша открытия окна (по умолчанию K, перебиндится в настройках управления).
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dominocities.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new CityScreen());
                    CityActions.requestState();
                    CityActions.requestTop();
                }
            }
        });

        // HUD: подпись названия города над хотбаром, пока игрок в своём городе.
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Protocol.NS, "city_hud"), new CityHud());

        // Периодически обновляем state (сервер сам присылает его только по действиям игрока —
        // без опроса HUD не заметит, что игрока приняли/выгнали/распустили город кем-то другим).
        int[] ticks = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) { ticks[0] = 0; return; }
            if (ticks[0]++ % 100 == 0) CityActions.requestState();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CityActions.requestState());
    }
}
