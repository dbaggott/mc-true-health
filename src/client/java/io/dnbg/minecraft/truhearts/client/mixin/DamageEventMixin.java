package io.dnbg.minecraft.truhearts.client.mixin;

import io.dnbg.minecraft.truhearts.client.hud.DamageLog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side hook onto {@code LivingEntity.handleDamageEvent} — the
 * method the vanilla client runs when it receives a
 * {@code ClientboundDamageEventPacket} from the server. Fires for every
 * LivingEntity that takes damage in view of the client; we filter down
 * to the local player and hand the source to {@link DamageLog}.
 *
 * <p>Chosen over a health-delta polling loop because this gives us the
 * actual {@link DamageSource} at the moment of damage — the causing
 * entity, the direct entity (projectile / trident), and the damage type.
 * Health delta alone would only tell us amount, not attribution.
 *
 * <p>We inject at {@code HEAD} (before vanilla applies any visual effect)
 * so the delta {@link DamageLog#onDamageEvent} computes against the
 * baseline captured on the last tick end is a clean "pool at previous
 * observation minus pool now" — vanilla's client-side handling of the
 * event doesn't itself change HP or absorption, so HEAD vs RETURN is
 * equivalent for the delta math; HEAD is slightly cheaper to reason
 * about.
 */
@Mixin(LivingEntity.class)
public abstract class DamageEventMixin {
	@Inject(method = "handleDamageEvent", at = @At("HEAD"))
	private void truhearts$onHandleDamageEvent(DamageSource source, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if ((Object) this != mc.player) {
			return;
		}
		DamageLog.onDamageEvent(source);
	}
}
