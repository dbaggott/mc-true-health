package io.dnbg.minecraft.truhearts.client.hud;

import io.dnbg.minecraft.truhearts.TruHearts;
import io.dnbg.minecraft.truhearts.client.config.TruHeartsConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Rolling list of the last few damage events dealt to the local player.
 * Populated from the client-side mixin on {@code LivingEntity.handleDamageEvent}
 * (see {@code DamageEventMixin}); rendered above {@link HpReadout} in the
 * same visual band so a hit's number and its source read as one unit.
 *
 * <p>Amount is computed from the total-pool delta (HP + absorption) so the
 * "damage received" figure reflects what the player actually felt — hits
 * absorbed by absorption still count. {@link #onClientTickEnd(LocalPlayer)}
 * keeps the baseline fresh between events so regen doesn't drift the delta.
 *
 * <p>Coupled to {@link VanillaHudElements#HEALTH_BAR} on purpose: when the
 * hearts row is hidden (creative, spectator) the log hides with it — same
 * "no vanilla hearts → no TruHearts overlay" rule {@link HpReadout} uses.
 */
public final class DamageLog {
	/** How many entries we keep on screen at once. Oldest evicts on overflow. */
	private static final int BUFFER_SIZE = 5;
	/** Total on-screen lifetime per entry. */
	private static final long ENTRY_LIFETIME_NANOS = 10_000_000_000L;
	/** Trailing fade window inside {@link #ENTRY_LIFETIME_NANOS}. */
	private static final long ENTRY_FADE_NANOS = 2_000_000_000L;

	// Layout constants — replicated from HpReadout rather than shared via a
	// package-private HudLayout class, to keep this PR's diff narrow. Extract
	// if a third overlay needs the same math.
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int HEART_BAR_BOTTOM_OFFSET = 39;
	private static final int Y_GAP_ABOVE_BAR = 10;
	private static final int ROW_HEIGHT = 10;

	/** Same red as {@link HpReadout}'s HP number — visually pairs the two overlays. */
	private static final int COLOR_AMOUNT = 0x00FF2A2A;
	/** Soft white for the source label; distinct from the amount without competing. */
	private static final int COLOR_LABEL = 0x00E0E0E0;

	private static final Deque<Entry> entries = new ArrayDeque<>();

	/**
	 * Baseline total (HP + absorption) captured at the end of the last
	 * client tick, used to compute damage amount when
	 * {@link #onClientTickEnd(LocalPlayer)} realizes a pending damage
	 * event. Kept fresh so healing and regen between events don't leak
	 * into the next event's delta.
	 */
	private static float baselineTotal = Float.NaN;

	/**
	 * Damage source captured by the mixin during packet processing,
	 * pending realization at end-of-tick when HP has caught up. Overwritten
	 * on repeat damage in the same tick — see {@link #onDamageEvent(DamageSource)}
	 * for the trade-off.
	 */
	private static DamageSource pendingSource;

	private DamageLog() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.HEALTH_BAR,
			TruHearts.id("damage_log"),
			DamageLog::extract
		);
	}

	/**
	 * Called by {@code DamageEventMixin} on the client's main thread when
	 * a damage event fires for the local player. We just stash the source
	 * here; the delta is computed in {@link #onClientTickEnd(LocalPlayer)}.
	 *
	 * <p><b>Why deferred:</b> the server sends {@code ClientboundDamageEventPacket}
	 * from inside {@code LivingEntity.hurtServer}, before the player's
	 * synchronized HP update goes out as a {@code ClientboundSetHealthPacket}
	 * later in the server tick. Client-side, this means the mixin fires
	 * BEFORE {@code player.getHealth()} reflects the new value — computing
	 * the delta at the mixin site gives 0 for every event and every entry
	 * gets skipped. By end-of-tick, SetHealth has been processed and the
	 * delta is accurate.
	 *
	 * <p>Multiple damage events in one client tick: the last source wins.
	 * The delta at tick end covers the total pool change, so it's attributed
	 * to the most recent source — imperfect for the rare "two hits landed
	 * in one tick" case, but simple and correct for the common case.
	 */
	public static void onDamageEvent(DamageSource source) {
		pendingSource = source;
	}

	/**
	 * End-of-tick hook. If the player's pool dropped since the last
	 * observation, log the drop against the best damage source we have:
	 * the mixin-captured source if it fired this tick, otherwise
	 * {@link LivingEntity#getLastDamageSource()} which vanilla itself
	 * populates from inside {@code handleDamageEvent}. The vanilla getter
	 * is our defense-in-depth path for any case where the mixin doesn't
	 * apply — the log still surfaces a labeled entry.
	 *
	 * <p>Then reset the baseline so between-event regen or healing doesn't
	 * get charged to the next event's delta.
	 */
	public static void onClientTickEnd(LocalPlayer player) {
		float currentTotal = totalPool(player);
		float amount = Float.isNaN(baselineTotal) ? 0f : baselineTotal - currentTotal;
		if (amount > 0.0001f) {
			DamageSource source = pendingSource != null
				? pendingSource
				: player.getLastDamageSource();
			if (source != null) {
				entries.addFirst(new Entry(amount, labelFor(source), System.nanoTime()));
				while (entries.size() > BUFFER_SIZE) {
					entries.removeLast();
				}
			}
		}
		pendingSource = null;
		baselineTotal = currentTotal;
	}

	/** Cleared on world exit / disconnect via {@code TruHeartsClient}. */
	public static void reset() {
		entries.clear();
		baselineTotal = Float.NaN;
		pendingSource = null;
	}

	private static float totalPool(LocalPlayer player) {
		return player.getHealth() + player.getAbsorptionAmount();
	}

	private static void extract(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		TruHeartsConfig cfg = TruHeartsConfig.get();
		if (!cfg.enabled || !cfg.recentDamageEnabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}

		long now = System.nanoTime();
		// Sweep expired entries lazily on the render pass — no separate
		// tick-based cleanup needed.
		entries.removeIf(e -> now - e.timestampNanos > ENTRY_LIFETIME_NANOS);
		if (entries.isEmpty()) {
			return;
		}

		Font font = mc.font;
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();

		// Match HpReadout's y-stack: gap above the heart row grows to clear
		// the armor row and absorption-heart row when present. Damage log
		// lines then stack above the HP readout.
		int yGap = Y_GAP_ABOVE_BAR;
		if (player.getAbsorptionAmount() > 0) {
			yGap += ROW_HEIGHT;
		}
		if (player.getArmorValue() > 0) {
			yGap += ROW_HEIGHT;
		}
		int hpReadoutBaselineY = screenH - HEART_BAR_BOTTOM_OFFSET - yGap;
		int x = screenW / 2 - HOTBAR_HALF_WIDTH;

		// Newest at bottom (closest to HpReadout), older stacking upward.
		int i = 0;
		for (Entry e : entries) {
			int y = hpReadoutBaselineY - ROW_HEIGHT * (i + 1);
			int alpha = alphaForAge(now - e.timestampNanos);
			int amountColor = (alpha << 24) | COLOR_AMOUNT;
			int labelColor = (alpha << 24) | COLOR_LABEL;

			String amountText = "- " + fmtAmount(e.amount);
			extractor.text(font, amountText, x, y, amountColor, true);
			int amountWidth = font.width(amountText);
			extractor.text(font, "  " + e.label, x + amountWidth, y, labelColor, true);
			i++;
		}
	}

	private static int alphaForAge(long ageNanos) {
		long remaining = ENTRY_LIFETIME_NANOS - ageNanos;
		if (remaining <= 0) {
			return 0;
		}
		if (remaining >= ENTRY_FADE_NANOS) {
			return 0xFF;
		}
		return (int) (255L * remaining / ENTRY_FADE_NANOS);
	}

	/**
	 * Mirrors {@link HpReadout#fmtHp}: two-decimal format with trailing
	 * zeros trimmed. Kept independent (rather than referenced) so a future
	 * change to either overlay's formatter can move independently.
	 */
	private static String fmtAmount(float value) {
		return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
	}

	/**
	 * Pick a short human label for the damage source. Prefer the causing
	 * entity's display name (mob/player attribution); fall back to a
	 * per-{@code msgId} bucket for elemental / environmental sources.
	 */
	private static String labelFor(DamageSource source) {
		Entity entity = source.getEntity();
		if (entity != null) {
			return entity.getDisplayName().getString();
		}
		return switch (source.getMsgId()) {
			case "fall" -> "Fell";
			case "inFire", "onFire" -> "Fire";
			case "lava" -> "Lava";
			case "drown" -> "Drowned";
			case "starve" -> "Starved";
			case "cactus" -> "Cactus";
			case "hotFloor" -> "Magma block";
			case "cramming" -> "Crushed";
			case "explosion", "explosion.player" -> "Explosion";
			case "magic", "indirectMagic" -> "Magic";
			case "wither" -> "Wither";
			case "lightningBolt" -> "Lightning";
			case "freeze" -> "Froze";
			case "sonic_boom" -> "Sonic boom";
			case "fallingBlock", "anvil", "fallingStalactite" -> "Falling block";
			case "sweetBerryBush" -> "Berry bush";
			case "stalagmite" -> "Stalagmite";
			case "outOfWorld" -> "Void";
			default -> capitalize(source.getMsgId());
		};
	}

	private static String capitalize(String s) {
		if (s.isEmpty()) {
			return s;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private record Entry(float amount, String label, long timestampNanos) {
	}
}
