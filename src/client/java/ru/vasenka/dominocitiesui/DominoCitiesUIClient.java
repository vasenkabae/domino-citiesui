package ru.vasenka.dominocitiesui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class DominoCitiesUIClient implements ClientModInitializer {

    private static KeyMapping openKey;
    private static KeyMapping mapKey;
    private static KeyMapping skillsKey;
    private static KeyMapping abilityKey;

    /** Сервер последнего онлайн-сеанса — своя память, не завязана на mc.getCurrentServer()
     *  (тот не гарантированно переживает дисконнект). Живёт, пока не закрыт клиент. */
    private static ServerData lastServer;

    @Override
    public void onInitializeClient() {
        // Регистрация типов пакетов (иначе клиент не заявит каналы серверу и не примет их).
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Action.TYPE, Payloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.State.TYPE, Payloads.State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Top.TYPE, Payloads.Top.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Result.TYPE, Payloads.Result.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Directory.TYPE, Payloads.Directory.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Resources.TYPE, Payloads.Resources.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Contracts.TYPE, Payloads.Contracts.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Bounties.TYPE, Payloads.Bounties.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Market.TYPE, Payloads.Market.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Buildings.TYPE, Payloads.Buildings.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.CityMap.TYPE, Payloads.CityMap.CODEC);
        // Профессии (dominoskills:*) — отдельный плагин на сервере, свой протокол.
        PayloadTypeRegistry.serverboundPlay().register(SkillsPayloads.Action.TYPE, SkillsPayloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillsPayloads.State.TYPE, SkillsPayloads.State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillsPayloads.Xp.TYPE, SkillsPayloads.Xp.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillsPayloads.Result.TYPE, SkillsPayloads.Result.CODEC);

        // Приём снапшотов (в клиентском потоке).
        ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onState(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Top.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onTop(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Result.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onResult(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Directory.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onDirectory(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Resources.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onResources(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Contracts.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onContracts(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Bounties.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onBounties(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Market.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onMarket(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Buildings.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onBuildings(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.CityMap.TYPE, (payload, context) ->
                context.client().execute(() -> CityData.onCityMap(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(SkillsPayloads.State.TYPE, (payload, context) ->
                context.client().execute(() -> SkillsData.onState(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(SkillsPayloads.Xp.TYPE, (payload, context) ->
                context.client().execute(() -> SkillsData.onXp(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(SkillsPayloads.Result.TYPE, (payload, context) ->
                context.client().execute(() -> SkillsData.onResult(payload.data())));

        // Клавиша открытия окна (по умолчанию K, перебиндится в настройках управления).
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dominocities.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyMapping.Category.MISC));
        // Отдельная клавиша карты мира — не через K-меню, чтобы его не захламлять.
        mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dominocities.map",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                KeyMapping.Category.MISC));
        // Профессии и древо талантов. НЕ P — в ванили P занята соц. взаимодействиями.
        skillsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dominoskills.skills",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                KeyMapping.Category.MISC));
        // Активка на G: короткий тап — применить выбранную, зажать — меню выбора активки.
        abilityKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dominoskills.ability",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                // Во время отсчёта фото постройки K-меню не открываем — кадр должен быть чистым.
                if (client.player != null && client.screen == null && !BuildingPhotoTaker.active()) {
                    client.setScreen(new CityScreen());
                    CityActions.requestState();
                    CityActions.requestTop();
                }
            }
            while (mapKey.consumeClick()) {
                if (client.player != null && client.screen == null && !BuildingPhotoTaker.active()) {
                    client.setScreen(new WorldMapScreen());
                    CityActions.requestCityMap();
                }
            }
            while (skillsKey.consumeClick()) {
                if (client.player != null && client.screen == null && !BuildingPhotoTaker.active()) {
                    client.setScreen(new SkillScreen());
                    SkillsActions.requestState();
                }
            }
        });

        // G: короткий тап — применить выбранную активку; удержание (≥250 мс) — меню выбора.
        final boolean[] gPrev = {false};
        final long[] gStart = {0};
        final boolean[] gMenuOpened = {false};
        final boolean[] gArmed = {false}; // цикл засчитывается, только если нажатие началось без открытого экрана
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (abilityKey.consumeClick()) { /* сбрасываем буфер: состояние читаем через isDown */ }
            boolean down = abilityKey.isDown();
            long now = System.currentTimeMillis();
            if (down && !gPrev[0]) {
                gArmed[0] = client.screen == null;
                gStart[0] = now;
                gMenuOpened[0] = false;
            }
            if (down && gArmed[0] && !gMenuOpened[0] && client.player != null && client.screen == null
                    && now - gStart[0] >= 250) {
                client.setScreen(new AbilityMenuScreen());
                gMenuOpened[0] = true;
            }
            if (!down && gPrev[0] && gArmed[0] && !gMenuOpened[0]
                    && client.player != null && client.screen == null) {
                triggerAbility(client);
            }
            gPrev[0] = down;
        });

        // Пока открыта карта мира — периодически опрашиваем сервер, чтобы точки жителей
        // своего города и список личных меток оставались живыми (не только по открытию).
        int[] mapTicks = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen instanceof WorldMapScreen && client.player != null) {
                if (mapTicks[0]++ % 20 == 0) CityActions.requestCityMap();
            } else {
                mapTicks[0] = 0;
            }
        });

        // Отсчёт и съёмка фото постройки (активируется из формы сохранения в CityScreen).
        ClientTickEvents.END_CLIENT_TICK.register(BuildingPhotoTaker::tick);

        // HUD: подпись названия города над хотбаром, пока игрок в своём городе.
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Protocol.NS, "city_hud"), new CityHud());
        // HUD: тосты опыта профессий и левел-апов.
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(SkillsProtocol.NS, "skills_hud"), new SkillsHud());

        // Периодически обновляем state (сервер сам присылает его только по действиям игрока —
        // без опроса HUD не заметит, что игрока приняли/выгнали/распустили город кем-то другим).
        int[] ticks = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) { ticks[0] = 0; return; }
            if (ticks[0]++ % 100 == 0) CityActions.requestState();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CityActions.requestState();
            SkillsActions.requestState();
            if (client.getCurrentServer() != null) lastServer = client.getCurrentServer();
        });

        // Памятка новичка: авто-показ после входа (пауза ~10 сек — успеть залогиниться через
        // DominoAuth), пока игрок не нажал «Больше не показывать». Если в момент срабатывания
        // открыт другой экран (чат, инвентарь) — ждём, пока экрана не будет.
        int[] guideDelay = {-1};
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!GuideScreen.dismissed()) guideDelay[0] = 200;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> guideDelay[0] = -1);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (guideDelay[0] > 0) {
                guideDelay[0]--;
            } else if (guideDelay[0] == 0 && client.screen == null) {
                guideDelay[0] = -1;
                client.setScreen(new GuideScreen());
            }
        });

        // ПКМ по игроку — раскрыть его ник/город на несколько секунд (см. NameReveal + мискин рендера).
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && hand == InteractionHand.MAIN_HAND && entity instanceof Player target) {
                NameReveal.reveal(target);
            }
            return InteractionResult.PASS;
        });

        // Кнопка «Переподключиться» в главном меню — чтобы не лезть в лаунчер после дисконнекта/кика/краша
        // до рабочего стола. Показывается только если в этом же запуске клиента уже был онлайн.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof TitleScreen) || lastServer == null) return;
            // Позиция — сразу под нижней кнопкой основного стека меню (Одиночная/Сетевая/Выход
            // и т.п.), не гадаем константы вёрстки ванили/форка. Мелкие иконки в углах
            // (доступность/язык, обычно 20×20) в расчёт не берём — иначе кнопка уедет к самому низу.
            int belowY = 4;
            for (AbstractWidget wdg : Screens.getWidgets(screen)) {
                if (wdg.getWidth() < 100) continue;
                belowY = Math.max(belowY, wdg.getY() + wdg.getHeight());
            }
            int y = Math.min(belowY + 6, h - 26);
            Button reconnect = Button.builder(Component.literal("Переподключиться"),
                            b -> reconnect(client, screen))
                    .bounds(w / 2 - 75, y, 150, 20).build();
            Screens.getWidgets(screen).add(reconnect);
        });
    }

    /** Тап G: применить выбранную активку (капстоун — активация, «Лёгкая рука» — вкл/выкл). */
    private static void triggerAbility(Minecraft client) {
        if (SkillsData.equippedLightHand && SkillsData.hasLightHand()) {
            SkillsActions.toggleLightHand();
            return;
        }
        int prof = SkillsData.chooseAbilityProf();
        if (prof >= 0) {
            SkillsData.lastAbilityProf = prof;
            SkillsActions.activate(prof);
        } else if (SkillsData.hasLightHand()) {
            SkillsData.equippedLightHand = true; // изучена только «Лёгкая рука» — её и переключаем
            SkillsActions.toggleLightHand();
        } else {
            client.gui.setOverlayMessage(Component.literal(
                    "Зажми G — выбери активку (сначала изучи в древе, N)"), false);
        }
    }

    /** Переподключение к последнему серверу с главного меню — без выхода в лаунчер. */
    private static void reconnect(Minecraft mc, Screen from) {
        if (lastServer == null) return;
        ConnectScreen.startConnecting(from, mc, ServerAddress.parseString(lastServer.ip), lastServer, false, null);
    }
}
