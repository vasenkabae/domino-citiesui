package ru.vasenka.dominocitiesui.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
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
 * нет непрозрачных блоков (raycast через Level#clip). Мобов не касается.
 */
@Mixin(EntityRenderer.class)
public class NameTagVisibilityMixin {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void dominocraft$onlyOnReveal(Entity entity, double distSqr, CallbackInfoReturnable<Boolean> cir) {
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

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void dominocraft$appendCity(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player) || !NameReveal.isRevealed(entity.getUUID())) return;
        String city = NameReveal.cityOf(player.getScoreboardName());
        String suffix = city != null ? " §7— " + city : " §7(без города)";
        cir.setReturnValue(cir.getReturnValue().copy().append(suffix));
    }
}
