package ru.vasenka.dominocitiesui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class DominoCitiesUIClient implements ClientModInitializer {

    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        // Регистрация типов пакетов (иначе клиент не заявит каналы серверу и не примет их).
        PayloadTypeRegistry.playC2S().register(Payloads.Action.TYPE, Payloads.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.State.TYPE, Payloads.State.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Top.TYPE, Payloads.Top.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Result.TYPE, Payloads.Result.CODEC);

        // Приём снапшотов (в клиентском потоке).
        ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onState(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Top.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onTop(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Result.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onResult(payload.data())));

        // Клавиша открытия окна (по умолчанию K, перебиндится в настройках управления).
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.dominocities.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.dominocities"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new CityScreen());
                    CityActions.requestState();
                    CityActions.requestTop();
                }
            }
        });
    }
}
