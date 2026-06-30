package io.dnbg.minecraft.truehearts;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared entry point. Runs on both client and server, though this mod ships
 * as client-only (see {@code fabric.mod.json}'s {@code "environment"}).
 *
 * <p>Keep this thin — HUD and other client-only wiring live in
 * {@link io.dnbg.minecraft.truehearts.client.TrueHeartsClient}.
 */
public class TrueHearts implements ModInitializer {
	public static final String MOD_ID = "truehearts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loaded {}", MOD_ID);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
