package io.dnbg.minecraft.truhearts.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent client-side settings for TruHearts.
 *
 * <p>Stored as JSON at {@code <minecraft>/config/truhearts.json}. Loads
 * lazily on first {@link #get()}; each mutation is followed by {@link #save()}
 * so the file on disk is always the live truth.
 *
 * <p>Two-level toggle model: {@link #enabled} is the master switch that
 * short-circuits everything TruHearts renders; per-feature booleans
 * ({@link #recentDamageEnabled}, …) gate individual overlays when the
 * master is on.
 */
public final class TruHeartsConfig {
	private static final Path PATH =
		FabricLoader.getInstance().getConfigDir().resolve("truhearts.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Master switch — when {@code false}, nothing TruHearts renders is
	 * visible (HP readout, damage log, any future overlay). Toggled at
	 * runtime via the master keybind; persisted across sessions.
	 */
	public boolean enabled = true;

	/**
	 * Sub-toggle for the recent-damage log. Independent of {@link #enabled}:
	 * the master switch short-circuits everything, and if the master is on
	 * this flag decides whether the damage log specifically renders. Its
	 * own keybind flips it in-game; persisted across sessions.
	 */
	public boolean recentDamageEnabled = true;

	private static TruHeartsConfig instance;

	public static TruHeartsConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static TruHeartsConfig load() {
		if (!Files.exists(PATH)) {
			return new TruHeartsConfig();
		}
		try {
			TruHeartsConfig loaded = GSON.fromJson(Files.readString(PATH), TruHeartsConfig.class);
			// Gson returns null for an empty/whitespace file; treat as defaults.
			return loaded != null ? loaded : new TruHeartsConfig();
		} catch (IOException | JsonSyntaxException e) {
			// Corrupt / unreadable — fall back to defaults. The next save()
			// overwrites the bad file. Not worth surfacing to the player.
			return new TruHeartsConfig();
		}
	}

	public void save() {
		try {
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			// Best-effort: the in-memory toggle still takes effect. Silent so
			// a stuck disk doesn't spam the log on every keypress.
		}
	}
}
