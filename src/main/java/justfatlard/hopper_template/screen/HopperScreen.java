package justfatlard.hopper_template.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.hopper_template.Template;
import justfatlard.pandorical.api.ComponentType;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.api.ScreenBuilder;
import justfatlard.pandorical.protocol.ComponentUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * The hopper's five slots, with a switch under each one and a switch for the lot.
 *
 * <p>Built as a Pandorical container screen rather than by bolting widgets onto vanilla's, the way
 * chest-utils builds a chest: the slots and the switches arrive in one definition, the presses come
 * back as ordinary screen actions, and none of this is client code.
 *
 * <p>A switched-off slot is shown read-only as well as being shut to automation, so a player cannot
 * put something into a slot that will never give it back.
 */
public final class HopperScreen {
	private HopperScreen() {}

	public static final String TYPE = "hopper-template:hopper";

	private static final String KEEP = "keep";
	private static final String SLOT = "slot";

	private static final int WIDTH = 176;
	private static final int MARGIN = 8;
	private static final int TITLE_Y = 6;
	private static final int CELL = 18;
	private static final int SLOT_Y = 20;
	private static final int SWITCH_Y = SLOT_Y + CELL + 2;
	private static final int SWITCH = 16;
	private static final int PACK_Y = SWITCH_Y + SWITCH + 12;
	private static final int LABEL_GAP = 10;
	private static final int COLS = 9;

	/** Which hopper a player has open, and the screen they have it open on. */
	private record Open(ServerLevel level, BlockPos pos, String screenId) {}

	private static final Map<UUID, Open> open = new ConcurrentHashMap<>();

	public static void register() {
		PandoricalApi.screens().onAction(TYPE, KEEP, (player, data) -> toggleKeep(player));
		for (int slot = 0; slot < Template.SLOTS; slot++) {
			int which = slot;
			PandoricalApi.screens().onAction(TYPE, SLOT + which, (player, data) -> toggleSlot(player, which));
		}
		PandoricalApi.screens().onClose(TYPE, player -> open.remove(player.getUUID()));
	}

	/** Whether this player's client can draw a Pandorical screen at all. */
	public static boolean canShow(ServerPlayer player) {
		return PandoricalApi.hasCapability(player, justfatlard.pandorical.api.Capabilities.SCREENS);
	}

	public static void show(ServerPlayer player, ServerLevel level, BlockPos pos,
			HopperBlockEntity hopper) {
		Template template = Template.of(hopper);
		if (template == null) return;

		ScreenBuilder screen = new ScreenBuilder(TYPE).container(Template.SLOTS, true);
		int height = PACK_Y + 3 * CELL + 4 + CELL + 8;
		screen.size(WIDTH, height);
		screen.panel("bg", 0, 0, WIDTH, height, Map.of("border", "beveled"));
		screen.text("title", MARGIN, TITLE_Y,
			Map.of("text", hopper.getDisplayName().getString(), "color", "#404040"));

		int left = (WIDTH - Template.SLOTS * CELL) / 2;
		screen.inventoryGrid("hopper", left, SLOT_Y, 1, Template.SLOTS, 0);
		for (int slot = 0; slot < Template.SLOTS; slot++) {
			screen.button(SLOT + slot, left + slot * CELL + 1, SWITCH_Y, SWITCH, SWITCH,
				slotProps(template.slotOff(slot)));
		}

		screen.button(KEEP, WIDTH - MARGIN - 54, TITLE_Y - 3, 54, 16, keepProps(template.keepsLast()));

		screen.text("pack_label", MARGIN, PACK_Y - LABEL_GAP, Map.of("text", "Inventory", "color", "#404040"));
		screen.inventoryGrid("pack", MARGIN, PACK_Y, 3, COLS, Template.SLOTS);
		screen.inventoryGrid("hotbar", MARGIN, PACK_Y + 3 * CELL + 4, 1, COLS, Template.SLOTS + 27);

		open.put(player.getUUID(), new Open(level, pos, screen.screenId()));
		PandoricalApi.screens().openContainer(player, screen.build(), hopper, readOnly(template));
	}

	/** A slot that is off cannot be reached by hand either: it would never give the item back. */
	private static Set<Integer> readOnly(Template template) {
		Set<Integer> shut = new HashSet<>();
		for (int slot = 0; slot < Template.SLOTS; slot++) {
			if (template.slotOff(slot)) shut.add(slot);
		}
		return shut;
	}

	private static Map<String, String> slotProps(boolean off) {
		return Map.of(
			ComponentType.PROP_LABEL, off ? "off" : "on",
			ComponentType.PROP_STYLE, off ? "default" : "pressed",
			ComponentType.PROP_TOOLTIP_KEY, off
				? "hopper-template.slot.off" : "hopper-template.slot.on");
	}

	private static Map<String, String> keepProps(boolean keep) {
		return Map.of(
			ComponentType.PROP_LABEL, keep ? "Keep on" : "Keep off",
			ComponentType.PROP_STYLE, keep ? "pressed" : "default",
			ComponentType.PROP_TOOLTIP_KEY, "hopper-template.keep");
	}

	private static Template at(ServerPlayer player) {
		Open now = open.get(player.getUUID());
		if (now == null) return null;
		return now.level().getBlockEntity(now.pos()) instanceof HopperBlockEntity hopper
			? Template.of(hopper) : null;
	}

	private static void toggleKeep(ServerPlayer player) {
		Template template = at(player);
		if (template == null) return;
		template.keepsLast(!template.keepsLast());
		click(player);
		PandoricalApi.screens().update(player, open.get(player.getUUID()).screenId(),
			List.of(new ComponentUpdate(KEEP, keepProps(template.keepsLast()))));
	}

	private static void toggleSlot(ServerPlayer player, int slot) {
		Template template = at(player);
		if (template == null) return;
		template.slotOff(slot, !template.slotOff(slot));
		click(player);

		// Reopened rather than updated: whether a slot is read-only is settled when the screen is
		// built, so a switch that only repainted the button would leave the slot still reachable.
		Open now = open.get(player.getUUID());
		if (now.level().getBlockEntity(now.pos()) instanceof HopperBlockEntity hopper) {
			show(player, now.level(), now.pos(), hopper);
		}
	}

	private static void click(ServerPlayer player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
			SoundSource.BLOCKS, 0.4F, 1.0F);
	}
}
