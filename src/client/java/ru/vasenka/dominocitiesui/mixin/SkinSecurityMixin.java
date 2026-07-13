package ru.vasenka.dominocitiesui.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/**
 * Найдено через javap+research (сверено с реальным поведением SkinsRestorer/официальной
 * документацией): PlayerInfo.createSkinLookup вычисляет флаг secure = !isLocalPlayer(uuid) и
 * передаёт его в SkinManager.createLookup — для ЧУЖИХ игроков (secure=true) итоговый
 * PlayerSkin проходит через Optional.filter(skin -> skin.secure()), а неподписанный (secure()
 * == false) кастомный скин отбрасывается в дефолтный Steve/Alex-подобный fallback, даже если
 * текстура успешно скачалась (см. SkinManager.lambda$createLookup$1/$4). Для локального
 * игрока (isLocalPlayer==true) requirement снимается — отсюда и асимметрия "вижу только
 * себя, чужие скины не вижу".
 *
 * DominoSkins (offline-mode сервер) инжектит НЕПОДПИСАННОЕ свойство textures — подписать его
 * настоящим Mojang-ключом мы не можем. Поэтому снимаем требование секьюрности целиком:
 * считаем любой профиль "локальным" именно в этой точке вызова.
 */
@Mixin(PlayerInfo.class)
public class SkinSecurityMixin {

    @Redirect(method = "createSkinLookup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalPlayer(Ljava/util/UUID;)Z"))
    private static boolean dominocraft$dontRequireSecureSkins(Minecraft instance, UUID id) {
        return true;
    }
}
