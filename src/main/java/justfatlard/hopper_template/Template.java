package justfatlard.hopper_template;

import net.minecraft.world.Container;

/**
 * What a hopper has been told about its own slots.
 *
 * <p>Two settings, and between them a filter. <b>Keep</b> stops a slot giving up its last item, so
 * whatever is left there stays as a sample of what that slot is for. <b>Off</b> shuts a slot
 * entirely: nothing goes in and nothing comes out.
 *
 * <p>A hopper merges an incoming item into a slot already holding that item, or into an empty one.
 * Leave one of each item you want through, turn Keep on, and switch off the slots you are not
 * using: there are no empty slots left to catch anything else, and the samples never leave. That
 * is a five-item filter built out of the hopper's own rules rather than a new kind of block.
 *
 * <p>Implemented by {@link justfatlard.hopper_template.mixin.HopperStateMixin} on the hopper's own
 * block entity, so the settings are saved and loaded with the hopper and travel with it in a
 * schematic. Reached through this interface rather than the mixin class, which callers cannot see.
 */
public interface Template {

	int SLOTS = 5;

	boolean keepsLast();

	void keepsLast(boolean keep);

	boolean slotOff(int slot);

	void slotOff(int slot, boolean off);

	/** Whether this slot may give up what it holds, which is the question both transfer paths ask. */
	default boolean releases(Container container, int slot) {
		if (slotOff(slot)) return false;
		return !keepsLast() || container.getItem(slot).getCount() > 1;
	}

	/** The hopper as a template, or null for any other container. */
	static Template of(Object container) {
		return container instanceof Template template ? template : null;
	}
}
