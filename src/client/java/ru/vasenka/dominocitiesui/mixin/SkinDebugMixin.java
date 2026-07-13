package ru.vasenka.dominocitiesui.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ВРЕМЕННАЯ диагностика бага "не видно чужие кастомные скины" — логирует РАЗ на игрока,
 * что реально вернул PlayerInfo.getSkin(): путь текстуры (кастомная vs ванильный Steve/Alex
 * fallback) и флаг secure. Убрать после того, как баг найден и исправлен.
 */
@Mixin(PlayerInfo.class)
public class SkinDebugMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("DominoCitiesUI/SkinDebug");
    private static final Set<UUID> logged = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject(method = "getSkin", at = @At("RETURN"))
    private void dominocraft$logSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerInfo self = (PlayerInfo) (Object) this;
        UUID id = self.getProfile().id();
        if (!logged.add(id)) return;
        PlayerSkin skin = cir.getReturnValue();
        var textures = self.getProfile().properties().get("textures");
        String rawInfo = textures.isEmpty() ? "NO TEXTURES PROPERTY"
                : textures.stream().map(p -> "len=" + p.value().length() + " sig=" + (p.signature() != null))
                        .reduce((a, b) -> a + "; " + b).orElse("?");
        LOGGER.info("[SkinDebug] {} ({}) -> texture={} secure={} rawProperty=[{}]",
                self.getProfile().name(), id, skin.body().texturePath(), skin.secure(), rawInfo);
    }
}
