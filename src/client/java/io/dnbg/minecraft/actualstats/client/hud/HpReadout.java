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
	/** Bright red that matches the vanilla heart-sprite hue more closely. */
	private static final int COLOR_HP = 0xFFFF2A2A;
	/**
	 * Soft gold for the absorption portion of the line — same brightness
	 * envelope as {@link #COLOR_HP} so the two segments feel like a pair
	 * rather than one bright and one washed out.
	 */
	private static final int COLOR_ABSORPTION = 0xFFFFCC55;

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
		// the vanilla heart bar. Each icon gets a 1-px black outline drawn
		// by stampOutlinedSprite below.
		int iconY = y + ICON_Y_NUDGE;
		stampOutlinedSprite(extractor, HEART_ICON, x, iconY);
		int textX = x + ICON_WIDTH + OUTLINE_WIDTH + ICON_TEXT_GAP;

		String hpText = String.format("%.2f / %.0f", player.getHealth(), player.getMaxHealth());
		extractor.text(font, hpText, textX, y, COLOR_HP, true);

		// When absorption is active, append a gold segment after the HP
		// text. No icon — the heart is the label for the whole line, and
		// the gold color carries the "this is absorption" distinction.
		if (absorption > 0) {
			int absTextX = textX + font.width(hpText) + ICON_TEXT_GAP;
			String absText = String.format("+ %.2f", absorption);
			extractor.text(font, absText, absTextX, y, COLOR_ABSORPTION, true);
		}
	}

	/**
	 * Renders a HUD-atlas sprite with a 1-px black outline by stamping the
	 * sprite tinted solid black at the four cardinal +/-1 offsets, then the
	 * untinted sprite on top. The black tint preserves the sprite's alpha,
	 * so the outline traces the sprite's silhouette rather than a square
	 * box around it.
	 */
	private static void stampOutlinedSprite(GuiGraphicsExtractor extractor, Identifier sprite, int x, int y) {
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - OUTLINE_WIDTH, y,                 ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + OUTLINE_WIDTH, y,                 ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x,                  y - OUTLINE_WIDTH, ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x,                  y + OUTLINE_WIDTH, ICON_WIDTH, ICON_HEIGHT, OUTLINE_COLOR);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x,                  y,                 ICON_WIDTH, ICON_HEIGHT);
	}
}
