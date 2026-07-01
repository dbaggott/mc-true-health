package io.dnbg.minecraft.truhearts.client;

import io.dnbg.minecraft.truhearts.client.config.TruHeartsConfig;
import io.dnbg.minecraft.truhearts.client.hud.DamageLog;
import io.dnbg.minecraft.truhearts.client.hud.HpReadout;
import io.dnbg.minecraft.truhearts.client.hud.ToggleToast;
import io.dnbg.minecraft.truhearts.client.input.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client entry point. Register HUD callbacks, attack callbacks, and any
 * other client-only wiring here.
 *
 * <p>Feature classes (HUD overlay, damage tracker, …) live as siblings
 * under {@code client.hud} / sibling sub-packages — this file stays as a
 * small wiring hub so new features land as one-line additions here.
 */
public class TruHeartsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HpReadout.register();
		DamageLog.register();
		ToggleToast.register();
		KeyBindings.register();
		ClientTickEvents.END_CLIENT_TICK.register(TruHeartsClient::onClientTickEnd);
	}

	/**
	 * Per-tick client-side hook. Drains any pending toggle-key clicks
	 * (master + per-feature) and hands the tick to {@link DamageLog} so
	 * it can refresh its damage-baseline tracking.
	 *
	 * <p>{@code consumeClick()} can return true more than once per tick
	 * under lag, hence the drain loop per keybind.
	 *
	 * <p>We render our own toasts via {@link ToggleToast} rather than
	 * calling vanilla's {@code setOverlayMessage} because that method's
	 * location differs between MC 26.1 ({@code Gui.setOverlayMessage})
	 * and 26.2 ({@code Gui.hud.setOverlayMessage}) — routing through
	 * {@link ToggleToast} keeps this code path source-compatible with
	 * both MC lines from a single jar.
	 */
	private static void onClientTickEnd(Minecraft client) {
		while (KeyBindings.TOGGLE.consumeClick()) {
			TruHeartsConfig cfg = TruHeartsConfig.get();
			cfg.enabled = !cfg.enabled;
			cfg.save();
			ToggleToast.show(Component.translatable(
				"truhearts.toggle." + (cfg.enabled ? "on" : "off")));
		}
		while (KeyBindings.TOGGLE_DAMAGE_LOG.consumeClick()) {
			TruHeartsConfig cfg = TruHeartsConfig.get();
			cfg.recentDamageEnabled = !cfg.recentDamageEnabled;
			cfg.save();
			ToggleToast.show(Component.translatable(
				"truhearts.toggle.damage_log." + (cfg.recentDamageEnabled ? "on" : "off")));
		}
		if (client.player != null) {
			DamageLog.onClientTickEnd(client.player);
		} else {
			// Player disconnected / world unloaded — clear the log so a
			// re-join doesn't attribute stale damage or leak old entries.
			DamageLog.reset();
		}
	}
}
