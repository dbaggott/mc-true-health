package io.dnbg.minecraft.actualstats.client.hud;

import io.dnbg.minecraft.actualstats.ActualStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;

/**
 * Renders the food-side hidden stats above the vanilla food bar:
 * <ul>
 *   <li><strong>Saturation</strong> — float, depletes silently before food
 *       level drops; gates HP regen and sprint stamina.</li>
 *   <li><strong>Exhaustion</strong> — float 0.0–4.0 that accumulates from
 *       movement/damage and resets when it hits 4.0, at which point it
 *       consumes 1 saturation (or 1 food level when saturation is 0). The
 *       bar visualizes how close the next saturation tick is.</li>
 * </ul>
 *
 * <p>Food level itself (the integer 0–20) is omitted from text because the
 * vanilla half-drumstick row already shows it precisely; duplicating it
 * adds noise without adding info.
 *
 * <p>Exhaustion is private on {@link FoodData} and read via the
 * {@link FoodDataAccessor} mixin.
 */
public final class FoodReadout {
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int FOOD_BAR_BOTTOM_OFFSET = 39;
	private static final int Y_GAP_ABOVE_BAR = 10;
	/**
	 * Height of one HUD icon row (food / oxygen / mount-health bars all use
	 * this pitch). We stack our overlay above any row vanilla is currently
	 * drawing above the food bar.
	 */
	private static final int ROW_HEIGHT = 10;

	private static final int EXHAUSTION_BAR_WIDTH = 80;
	private static final int EXHAUSTION_BAR_HEIGHT = 3;
	/** Gap between the exhaustion bar's bottom and the saturation text's top. */
	private static final int BAR_TEXT_GAP = 2;

	/** Soft amber — easier on the eyes than the saturated drumstick orange. */
	private static final int COLOR_SATURATION = 0xFFCC9966;
	/** Semi-transparent black behind the exhaustion bar fill. */
	private static final int COLOR_BAR_BG = 0x80000000;
	/** Vivid amber when we have the exact server-truth value (single-player). */
	private static final int COLOR_EXHAUSTION_FILL_EXACT = 0xFFFFAA55;
	/**
	 * Desaturated amber when the value is a multiplayer estimate — visually
	 * distinct from the exact case so the bar is honest about uncertainty.
	 */
	private static final int COLOR_EXHAUSTION_FILL_ESTIMATED = 0xFFAA8866;

	private FoodReadout() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.FOOD_BAR,
			ActualStats.id("food_readout"),
			FoodReadout::extract
		);
	}

	private static void extract(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}

		Font font = mc.font;
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		// IMPORTANT: vanilla only sends ClientboundSetHealthPacket when
		// (health changes) || (foodLevel changes) || (saturation crosses
		// zero — tracked via ServerPlayer.lastFoodSaturationZero). So the
		// client's local FoodData.saturationLevel can stay stale for many
		// server-side decrements while the player sees the bar reset
		// repeatedly with no apparent sat change, then jumps from some old
		// value directly to 0 once the server's sat actually crosses zero.
		// In single-player we read FoodData straight from the integrated
		// server's player instance to dodge that lag entirely. In
		// multiplayer we accept the stale value — there's no other source.
		FoodData food = serverSideFoodData(mc, player).orElse(player.getFoodData());

		// Stack our overlay above any row vanilla is currently drawing above
		// the food bar (oxygen bubbles when underwater / regenerating air;
		// mount-health bar when riding a LivingEntity).
		int yGap = Y_GAP_ABOVE_BAR;
		if (player.getAirSupply() < player.getMaxAirSupply()) {
			yGap += ROW_HEIGHT;
		}
		if (player.isPassenger() && player.getVehicle() instanceof LivingEntity) {
			yGap += ROW_HEIGHT;
		}

		// Saturation text — right-aligned to the food bar's right edge.
		String text = String.format("%.2f", food.getSaturationLevel());
		int rightX = screenWidth / 2 + HOTBAR_HALF_WIDTH;
		int textX = rightX - font.width(text);
		int textY = screenHeight - FOOD_BAR_BOTTOM_OFFSET - yGap;
		extractor.text(font, text, textX, textY, COLOR_SATURATION, true);

		// Exhaustion bar — directly above the saturation text, same right edge.
		// In single-player the bar fills from the real integrated-server value;
		// in multiplayer we don't have the value, so we deduce one from
		// observed reset events. The bar's color tells the two cases apart.
		ExhaustionTracker.Reading exh = ExhaustionTracker.read();
		float ratio = Math.min(exh.value() / exh.max(), 1.0f);
		int barRightX = rightX;
		int barLeftX = barRightX - EXHAUSTION_BAR_WIDTH;
		int barTopY = textY - BAR_TEXT_GAP - EXHAUSTION_BAR_HEIGHT;
		int barBottomY = barTopY + EXHAUSTION_BAR_HEIGHT;

		extractor.fill(barLeftX, barTopY, barRightX, barBottomY, COLOR_BAR_BG);
		int fillWidth = (int) (EXHAUSTION_BAR_WIDTH * ratio);
		if (fillWidth > 0) {
			int fillColor = exh.exact() ? COLOR_EXHAUSTION_FILL_EXACT : COLOR_EXHAUSTION_FILL_ESTIMATED;
			extractor.fill(barLeftX, barTopY, barLeftX + fillWidth, barBottomY, fillColor);
		}
	}

	/**
	 * Returns the server-side {@link FoodData} for the local player when
	 * we're running an integrated server (single-player). Empty otherwise.
	 * The server-side instance has the real-time {@code saturationLevel}
	 * the client never receives over the wire (see the comment in
	 * {@link #extract} for the vanilla packet-trigger detail).
	 */
	private static java.util.Optional<FoodData> serverSideFoodData(Minecraft mc, LocalPlayer player) {
		if (!mc.hasSingleplayerServer()) {
			return java.util.Optional.empty();
		}
		IntegratedServer server = mc.getSingleplayerServer();
		ServerPlayer sp = server.getPlayerList().getPlayer(player.getUUID());
		return java.util.Optional.ofNullable(sp).map(ServerPlayer::getFoodData);
	}
}
