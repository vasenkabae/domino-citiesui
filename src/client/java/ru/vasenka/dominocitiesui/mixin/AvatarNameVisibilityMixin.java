package ru.vasenka.dominocitiesui.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vasenka.dominocitiesui.NameReveal;

/**
 * Ваниль показывает ник игрока при приближении/наведении, без учёта стен между камерой и целью.
 * Для Player-сущностей это поведение полностью заменяем: ник виден только если по игроку недавно
 * кликнули ПКМ (см. NameReveal.reveal, вызывается из UseEntityCallback) И между камерой и целью
 * нет непрозрачных блоков (raycast через Level#clip).
 *
 * ВАЖНО: этот форк рендерит игроков через AvatarRenderer (не PlayerRenderer), и AvatarRenderer
 * (а также промежуточный LivingEntityRenderer) ПЕРЕОПРЕДЕЛЯЕТ shouldShowName собственной
 * реализацией — патч базового EntityRenderer.shouldShowName для игроков никогда не вызывается
 * (виртуальный вызов уходит в переопределённый метод). Проверено javap по
 * minecraft-clientonly-deobf-26.1.2.jar: AvatarRenderer объявляет
 * shouldShowName(Avatar, double) — именно его и патчим.
 */
@Mixin(AvatarRenderer.class)
public class AvatarNameVisibilityMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void dominocraft$onlyOnReveal(Avatar entity, double distSqr, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player)) return;

        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.level == null || !NameReveal.isRevealed(entity.getUUID())) {
            cir.setReturnValue(false);
            return;
        }

        Vec3 from = camera.getEyePosition();
        Vec3 to = entity.getEyePosition();
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera);
        boolean occluded = mc.level.clip(ctx).getType() != HitResult.Type.MISS;
        cir.setReturnValue(!occluded);
    }
}
