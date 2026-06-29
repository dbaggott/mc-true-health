package io.dnbg.minecraft.actualstats.client.hud;

import io.dnbg.minecraft.actualstats.client.mixin.FoodDataAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Single source of truth for "what is the player's exhaustion right now?".
 *
 * <p>Exhaustion is server-side state and is <em>not</em> sent down the wire
 * by {@code ClientboundSetHealthPacket}, so a pure-client read of
 * {@link FoodData#getSaturationLevel()}'s sibling {@code exhaustionLevel}
 * field returns 0.0 forever. We use two paths to recover a useful value:
 *
 * <ol>
 *   <li><strong>Single-player (exact):</strong> the integrated server runs
 *       in this same JVM. {@link Minecraft#hasSingleplayerServer()} tells
 *       us when, and {@link Minecraft#getSingleplayerServer()} gives us
 *       access. We look up the server-side {@link ServerPlayer} by the
 *       same UUID as our client player and read the real
 *       {@code exhaustionLevel} through the {@link FoodDataAccessor}
 *       mixin. The {@link Reading#exact} flag is {@code true}.</li>
 *
 *   <li><strong>Multiplayer (deduced):</strong> we can't see exhaustion
 *       directly, but we can see its <em>resets</em>: every time the
 *       server's exhaustion hits 4.0 it subtracts 4.0 and decrements
 *       saturation by 1.0 (or food level by 1 when saturation is empty).
 *       Those decrements arrive on the client via the health packet, so
 *       a client tick listener can spot them. We record the interval
 *       between recent resets, compute a rolling-average period, and
 *       report current exhaustion as
 *       {@code (time-since-last-reset / avg-period) × 4.0}. The
 *       {@link Reading#exact} flag is {@code false} so callers can render
 *       the value differently (e.g. dimmer) to signal "estimate".</li>
 * </ol>
 *
 * <p>The estimate is approximate by construction: it adapts to a recent
 * activity rhythm but lags sudden changes in pace (sprinting after sitting
 * still, etc.). The reset event itself is exact — when we see saturation
 * drop, we know exhaustion just reset, so the bar snaps empty on cue and
 * starts filling fresh, anchored to the truth at each tick.
 */
public final class ExhaustionTracker {
	public record Reading(float value, float max, boolean exact) {
	}

	public static final float MAX_EXHAUSTION = 4.0f;

	/** Size of the rolling buffer of recent inter-reset intervals. */
	private static final int RECENT_INTERVALS = 8;
	/**
	 * Period we assume before we've observed any resets — light-activity
	 * baseline. Bar will be inaccurate until the first reset arrives.
	 */
	private static final long DEFAULT_INTERVAL_MS = 30_000L;

	// Mutable single-thread (client-tick) state. Static for callback simplicity;
	// the tracker is effectively a singleton bound to the running client.
	private static float lastSat = -1f;
	private static int lastFood = -1;
	private static long lastResetMs = 0L;
	private static final long[] recentIntervalsMs = new long[RECENT_INTERVALS];
	private static int intervalsRecorded = 0;
	private static int intervalsIndex = 0;
	private static boolean wasConnected = false;

	private ExhaustionTracker() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ExhaustionTracker::onTick);
	}

	private static void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			if (wasConnected) {
				resetObservationState();
				wasConnected = false;
			}
			lastSat = -1f;
			lastFood = -1;
			return;
		}
		wasConnected = true;

		FoodData food = player.getFoodData();
		float sat = food.getSaturationLevel();
		int level = food.getFoodLevel();

		if (lastSat < 0) {
			// First observation in this session — just record, don't infer.
			lastSat = sat;
			lastFood = level;
			return;
		}

		// A reset is one of:
		//   - saturation dropped (eating only adds, so any drop is exhaustion-driven)
		//   - saturation was already 0 and food level dropped by ≥ 1
		boolean resetEvent = sat < lastSat
			|| (lastSat == 0f && level < lastFood);

		if (resetEvent) {
			long now = System.currentTimeMillis();
			if (lastResetMs > 0) {
				long interval = now - lastResetMs;
				recentIntervalsMs[intervalsIndex] = interval;
				intervalsIndex = (intervalsIndex + 1) % RECENT_INTERVALS;
				if (intervalsRecorded < RECENT_INTERVALS) {
					intervalsRecorded++;
				}
			}
			lastResetMs = now;
		}

		lastSat = sat;
		lastFood = level;
	}

	private static void resetObservationState() {
		lastResetMs = 0L;
		intervalsRecorded = 0;
		intervalsIndex = 0;
	}

	public static Reading read() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return new Reading(0f, MAX_EXHAUSTION, false);
		}

		// Single-player: read the real value from the integrated server.
		if (mc.hasSingleplayerServer()) {
			IntegratedServer server = mc.getSingleplayerServer();
			ServerPlayer sp = server.getPlayerList().getPlayer(player.getUUID());
			if (sp != null) {
				float exh = ((FoodDataAccessor) (Object) sp.getFoodData()).getExhaustionLevel();
				return new Reading(Math.min(exh, MAX_EXHAUSTION), MAX_EXHAUSTION, true);
			}
			// Server-side player not present yet (just joined / dimension change).
			// Fall through to estimate rather than reporting 0 with exact=true.
		}

		// Multiplayer (or transient single-player state): estimate from reset history.
		if (lastResetMs == 0) {
			return new Reading(0f, MAX_EXHAUSTION, false);
		}
		long timeSinceMs = System.currentTimeMillis() - lastResetMs;
		long avgIntervalMs = averageIntervalMs();
		float ratio = avgIntervalMs > 0 ? (float) timeSinceMs / avgIntervalMs : 0f;
		float value = Math.min(ratio * MAX_EXHAUSTION, MAX_EXHAUSTION);
		return new Reading(value, MAX_EXHAUSTION, false);
	}

	private static long averageIntervalMs() {
		if (intervalsRecorded == 0) {
			return DEFAULT_INTERVAL_MS;
		}
		long sum = 0;
		for (int i = 0; i < intervalsRecorded; i++) {
			sum += recentIntervalsMs[i];
		}
		return sum / intervalsRecorded;
	}
}
