package io.dnbg.minecraft.truhearts.client.hud;

import io.dnbg.minecraft.truhearts.TruHearts;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Transient text overlay TruHearts uses in place of vanilla's held-item
 * message box.
 *
 * <p><b>Why we render this ourselves rather than calling
 * {@code setOverlayMessage}:</b> that method lives on
 * {@code Gui.hud} in MC 26.2 but directly on {@code Gui} in 26.1 —
 * there's no source form that compiles against both. Routing through
 * {@link HudElementRegistry} keeps the whole path version-neutral, so a
 * single jar targets both MC lines.
 *
 * <p><b>Bonus:</b> we own the lifetime and fade curve here directly.
 * Vanilla's {@code setOverlayMessage} hardcodes its duration with no
 * knob; tune the constants below when the feel wants adjusting.
 *
 * <p>Positioned above the hotbar to mimic vanilla's held-item slot so
 * the "TruHearts: on/off" message reads in a familiar location.
 */
public final class ToggleToast {
	/**
	 * Total lifetime of the toast — peripheral cue for a keypress
	 * confirmation. Wall-clock rather than tick-based so the toast
	 * expires when intended even through frame-rate hitches; a client
	 * tick can be arbitrarily slow.
	 */
	private static final long DEFAULT_LIFETIME_NANOS = 500_000_000L;
	/**
	 * Trailing fade-out window inside {@link #DEFAULT_LIFETIME_NANOS}.
	 * If it equals the lifetime, the entire toast is a linear fade with
	 * no full-opacity hold plateau.
	 */
	private static final long FADE_OUT_NANOS = 500_000_000L;
	/**
	 * Y offset from the screen bottom — matches the vertical band vanilla's
	 * setOverlayMessage renders in (above the hotbar row and its bars).
	 */
	private static final int Y_ABOVE_HOTBAR = 68;

	private static Component message;
	private static long expiryNanos;

	private ToggleToast() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.HEALTH_BAR,
			TruHearts.id("toggle_toast"),
			ToggleToast::extract
		);
	}

	/**
	 * Show a message for {@link #DEFAULT_LIFETIME_NANOS}. Replaces any
	 * previous message currently on-screen.
	 */
	public static void show(Component text) {
		message = text;
		expiryNanos = System.nanoTime() + DEFAULT_LIFETIME_NANOS;
	}

	private static void extract(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		if (message == null) {
			return;
		}
		long remainingNanos = expiryNanos - System.nanoTime();
		if (remainingNanos <= 0) {
			message = null;
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();

		// Alpha: 0xFF while remaining > FADE_OUT_NANOS, linear ramp to 0 at
		// expiry. Multiplied into the color's alpha channel; the text
		// renderer applies it to both the glyph fill and the drop shadow.
		int alpha = 0xFF;
		if (remainingNanos < FADE_OUT_NANOS) {
			alpha = (int) (255L * remainingNanos / FADE_OUT_NANOS);
		}
		int color = (alpha << 24) | 0x00FFFFFF;

		int width = font.width(message);
		int x = (screenW - width) / 2;
		int y = screenH - Y_ABOVE_HOTBAR;
		extractor.text(font, message, x, y, color, true);
	}
}
