package io.dnbg.minecraft.actualstats.client.hud;

import io.dnbg.minecraft.actualstats.ActualStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
	 * Base gap between the heart bar's top and our text — the "no armor,
	 * no absorption" case. Additional rows above the hearts shift our text
	 * up by {@link #ROW_HEIGHT} each.
	 */
	private static final int Y_GAP_ABOVE_BAR = 10;
	/**
	 * Height of one HUD icon row (hearts / armor / absorption all use this
	 * pitch). Used to stack our text above any row vanilla is currently
	 * drawing in the area immediately above the hearts.
	 */
	private static final int ROW_HEIGHT = 10;
	/** Soft red — closer to the heart color than pure red, easier on eyes. */
	private static final int COLOR_HP = 0xFFFF5555;

	/**
	 * Vanilla's regular full-heart sprite from the HUD atlas. Found via
	 * decompiling {@code Hud.HeartType.NORMAL} which constructs the
	 * identifier as {@code "minecraft:hud/heart/full"}. Using the same
	 * sprite as the vanilla heart bar keeps the icon visually consistent
	 * with what's already on screen.
	 */
	private static final Identifier HEART_ICON = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
	/** Vanilla heart sprite is 9×9 px. */
	private static final int ICON_WIDTH = 9;
	private static final int ICON_HEIGHT = 9;
	/** Tiny gap between the icon and the text so they don't bleed together. */
	private static final int ICON_TEXT_GAP = 2;
	/**
	 * 1-px black outline drawn by stamping the heart silhouette in solid
	 * black at four cardinal offsets before drawing the real red heart on
	 * top. Width adds to the icon's effective footprint and pushes the
	 * text right by one extra pixel so the gap stays visually constant.
	 */
	private static final int OUTLINE_WIDTH = 1;
	/** Full-opacity black, tinted onto the heart sprite for outline stamps. */
	private static final int OUTLINE_COLOR = 0xFF000000;
	/**
	 * Vertical nudge for the heart so its visual centre aligns with the
	 * text's. The 9-px heart vs 8-px font height puts the heart 0.5 px low
	 * by default; raising it 1 px reads better than no offset.
	 */
	private static final int ICON_Y_NUDGE = -1;

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

		// Stack our text above any rows the vanilla HUD is currently drawing
		// in the band immediately above the hearts (armor icons; absorption
		// hearts). Each present row takes one ROW_HEIGHT slice, so we offset
		// up by the sum to land in clean airspace above all of them.
		int yGap = Y_GAP_ABOVE_BAR;
		float absorption = player.getAbsorptionAmount();
		if (absorption > 0) {
			yGap += ROW_HEIGHT;
		}
		if (player.getArmorValue() > 0) {
			yGap += ROW_HEIGHT;
		}

		int x = screenWidth / 2 - HOTBAR_HALF_WIDTH;
		int y = screenHeight - HEART_BAR_BOTTOM_OFFSET - yGap;

		// Label: a real heart icon so the floating numbers are self-
		// identifying even when armor/absorption rows push them far above
		// the vanilla heart bar. A 1-px black outline is added by stamping
		// the heart in solid black at the four cardinal +/-1 offsets first,
		// then the real red heart on top. The tint preserves the sprite's
		// alpha, so the outline traces the heart's actual silhouette
		// rather than a square box around it.
		int iconY = y + ICON_Y_NUDGE;
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_ICON, x - OUTLINE_WIDTH, iconY,                 ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_ICON, x + OUTLINE_WIDTH, iconY,                 ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_ICON, x,                  iconY - OUTLINE_WIDTH, ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_ICON, x,                  iconY + OUTLINE_WIDTH, ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_ICON, x,                  iconY,                 ICON_WIDTH, ICON_HEIGHT);
		int textX = x + ICON_WIDTH + OUTLINE_WIDTH + ICON_TEXT_GAP;

		String text = formatHp(player.getHealth(), player.getMaxHealth(), absorption);
		extractor.text(font, text, textX, y, COLOR_HP, true);
	}

	/**
	 * Renders HP as {@code "X.XX / Y"} normally, extending to
	 * {@code "X.XX / Y + Z.ZZ"} when absorption is active so the hidden
	 * float behind the gold hearts is visible. Absorption is omitted when
	 * zero to keep the line short during regular play.
	 */
	private static String formatHp(float health, float max, float absorption) {
		if (absorption > 0) {
			return String.format("%.2f / %.0f + %.2f", health, max, absorption);
		}
		return String.format("%.2f / %.0f", health, max);
	}
}
