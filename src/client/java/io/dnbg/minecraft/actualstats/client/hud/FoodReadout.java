package io.dnbg.minecraft.actualstats.client.hud;

import io.dnbg.minecraft.actualstats.ActualStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Renders food-related stats just above the food bar, right-aligned to
 * the hotbar's right edge so the text visually parents to the drumsticks.
 *
 * <p>The food bar already shows the integer food level (0–20) precisely,
 * so the value the player can't normally see is <strong>saturation</strong>
 * — a hidden float (0.0–20.0) that depletes silently before food level
 * drops and that gates HP regen and sprint stamina. Two formats are
 * implemented; {@link #FORMAT} picks. Flip the constant and rebuild to
 * compare. Once a format wins, the loser goes away.
 */
public final class FoodReadout {
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int FOOD_BAR_BOTTOM_OFFSET = 39;
	private static final int Y_GAP_ABOVE_BAR = 10;
	/** Soft amber — easier on the eyes than the saturated drumstick orange. */
	private static final int COLOR_FOOD = 0xFFCC9966;

	/**
	 * Layout toggle. Flip the value of {@link #FORMAT} below to switch
	 * between displays; the losers will be deleted once we pick.
	 */
	private enum FoodFormat {
		/** "18 / 20 · S 3.5" — food level in X/Y form (mirrors HP), plus saturation. */
		FULL,
		/** "Sat 3.5 / 20" — saturation only (food level already precise on the bar). */
		SATURATION_ONLY
	}

	private static final FoodFormat FORMAT = FoodFormat.FULL;

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

		FoodData food = player.getFoodData();
		String text = format(food);

		// Right-align the text to the hotbar's right edge so it parents
		// visually to the food bar, mirroring the HP readout's left edge.
		int rightX = screenWidth / 2 + HOTBAR_HALF_WIDTH;
		int x = rightX - font.width(text);
		int y = screenHeight - FOOD_BAR_BOTTOM_OFFSET - Y_GAP_ABOVE_BAR;

		extractor.text(font, text, x, y, COLOR_FOOD, true);
	}

	private static String format(FoodData food) {
		return switch (FORMAT) {
			// Food level uses the same "%d / 20" shape as HP's "%.1f / %.0f"
			// so the left and right readouts read as a matched pair. The
			// hidden saturation float follows after a middle dot.
			case FULL -> String.format("%d / 20 · S %.1f", food.getFoodLevel(), food.getSaturationLevel());
			case SATURATION_ONLY -> String.format("Sat %.1f / 20", food.getSaturationLevel());
		};
	}
}
