package io.dnbg.minecraft.actualstats.client.hud;

import io.dnbg.minecraft.actualstats.ActualStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Renders the player's real (un-rounded) HP just above the heart bar,
 * left-aligned to the hotbar's left edge so it visually parents to the
 * hearts rather than floating in the corner.
 *
 * <p>Attached <em>after</em> the vanilla {@code HEALTH_BAR} element so
 * our text composites on top of anything the heart bar renders in the
 * same area (e.g. absorption hearts that stack upward).
 */
public final class HpReadout {
	/** Vanilla hotbar is 182 px wide, centered horizontally. */
	private static final int HOTBAR_HALF_WIDTH = 91;
	/**
	 * Top of the vanilla heart bar relative to screen bottom. Hearts are
	 * 9 px tall and sit in this row.
	 */
	private static final int HEART_BAR_BOTTOM_OFFSET = 39;
	/**
	 * Gap between the heart bar's top and our text. 10 px clears the bar
	 * with a tiny breathing margin and leaves room for absorption hearts
	 * that stack above on the same Y.
	 */
	private static final int Y_GAP_ABOVE_BAR = 10;
	/** Soft red — closer to the heart color than pure red, easier on eyes. */
	private static final int COLOR_HP = 0xFFFF5555;

	private HpReadout() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.HEALTH_BAR,
			ActualStats.id("hp_readout"),
			HpReadout::extract
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

		int x = screenWidth / 2 - HOTBAR_HALF_WIDTH;
		int y = screenHeight - HEART_BAR_BOTTOM_OFFSET - Y_GAP_ABOVE_BAR;

		String text = String.format("%.1f / %.0f", player.getHealth(), player.getMaxHealth());
		extractor.text(font, text, x, y, COLOR_HP, true);
	}
}
