package ru.vasenka.dominocitiesui.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vasenka.dominocitiesui.NameReveal;

/**
 * Дописывает город к нику (getNameTag не переопределён ни в AvatarRenderer, ни в
 * LivingEntityRenderer — проверено javap, поэтому патчить именно базовый EntityRenderer
 * достаточно). Видимость самого ника (когда его вообще показывать) — отдельный миксин
 * {@link AvatarNameVisibilityMixin}, потому что shouldShowName ПЕРЕОПРЕДЕЛЁН в AvatarRenderer
 * (и в промежуточном LivingEntityRenderer), и патч базового EntityRenderer.shouldShowName
 * никогда не вызывается для игроков — виртуальный вызов уходит в переопределённый метод.
 */
@Mixin(EntityRenderer.class)
public class NameTagVisibilityMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void dominocraft$appendCity(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player) || !NameReveal.isRevealed(entity.getUUID())) return;
        String city = NameReveal.cityOf(player.getScoreboardName());
        String suffix = city != null ? " §7— " + city : " §7(без города)";
        cir.setReturnValue(cir.getReturnValue().copy().append(suffix));
    }
}
