package ru.vasenka.dominocitiesui.mixin;

import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Клиентский патч allowlist доменов скинов.
 *
 * authlib 7.0.62 в com.mojang.authlib.yggdrasil.TextureUrlChecker разрешает грузить
 * текстуры скинов ТОЛЬКО с host'а textures.minecraft.net (ALLOWED_DOMAINS = Set.of(...)).
 * Наши скины раздаёт VDS Domino Craft (138.16.181.96:8765/dc/skins/<ник>.png), поэтому
 * ванильная проверка их отбрасывает. Разрешаем дополнительно host нашего VDS —
 * тогда серверная инъекция текстуры (плагин DominoSkins) начинает рендериться.
 *
 * remap = false: цель — класс authlib, а не Minecraft, имена не ремапятся.
 */
@Mixin(value = TextureUrlChecker.class, remap = false)
public class TextureUrlCheckerMixin {

    private static final String DOMINO_SKIN_HOST = "138.16.181.96";

    @Inject(method = "isAllowedTextureDomain", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dominocraft$allowVdsSkinHost(String url, CallbackInfoReturnable<Boolean> cir) {
        if (url != null && url.contains(DOMINO_SKIN_HOST)) {
            cir.setReturnValue(true);
        }
    }
}
