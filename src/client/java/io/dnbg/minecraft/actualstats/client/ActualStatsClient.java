package io.dnbg.minecraft.actualstats.client;

import io.dnbg.minecraft.actualstats.client.hud.HpReadout;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point. Register HUD callbacks, attack callbacks, and any
 * other client-only wiring here.
 *
 * <p>Feature classes (HUD overlay, damage tracker, …) live as siblings
 * under {@code client.hud} / sibling sub-packages — this file stays as a
 * small wiring hub so new features land as one-line additions here.
 */
public class ActualStatsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HpReadout.register();
	}
}
