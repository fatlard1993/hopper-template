package justfatlard.hopper_template.mixin;

import justfatlard.hopper_template.Template;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The settings themselves, carried on the hopper that owns them.
 *
 * <p>On the block entity rather than in a map beside the world, because a hopper's settings are
 * part of the hopper: they save with it, load with it, and come along when it is copied into a
 * schematic or moved by anything that carries block entities. A map would have to be told about
 * every one of those and would be wrong the first time it was not.
 *
 * <p>Stored as one int rather than five booleans and a flag, so the tag is one number and an old
 * hopper with no tag at all reads as every slot on and nothing kept - which is vanilla, and is
 * what a hopper placed before this mod should behave like.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperStateMixin extends BlockEntity implements Template {

	@Unique
	private static final String TAG = "hopper-template:slots";

	/** Bits 0-4: that slot is off. Bit 5: the last item stays. */
	@Unique
	private static final int KEEP_BIT = 1 << Template.SLOTS;

	@Unique
	private int hopperTemplate$flags;

	private HopperStateMixin() {
		super(null, null, null);
	}

	@Override
	public boolean keepsLast() {
		return (hopperTemplate$flags & KEEP_BIT) != 0;
	}

	@Override
	public void keepsLast(boolean keep) {
		hopperTemplate$flags = keep ? hopperTemplate$flags | KEEP_BIT : hopperTemplate$flags & ~KEEP_BIT;
		setChanged();
	}

	@Override
	public boolean slotOff(int slot) {
		return slot >= 0 && slot < Template.SLOTS && (hopperTemplate$flags & (1 << slot)) != 0;
	}

	@Override
	public void slotOff(int slot, boolean off) {
		if (slot < 0 || slot >= Template.SLOTS) return;
		hopperTemplate$flags = off ? hopperTemplate$flags | (1 << slot) : hopperTemplate$flags & ~(1 << slot);
		setChanged();
	}

	/**
	 * A slot that is off takes nothing, whoever is offering.
	 *
	 * <p>Written as a plain method rather than an injection because HopperBlockEntity does not
	 * declare this one: it inherits Container's default. A method merged in here becomes the
	 * class's own override and wins over that default, which is the same trick stackz uses to give
	 * automation containers back their stack limit. An @Inject has nothing to attach to and fails
	 * at load, which is how this was found.
	 */
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !slotOff(slot);
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void hopperTemplate$save(ValueOutput output, CallbackInfo ci) {
		// Nothing written for a hopper nobody has touched, so this mod leaves no trace in the
		// save of a world where it was installed and not used.
		if (hopperTemplate$flags != 0) output.putInt(TAG, hopperTemplate$flags);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void hopperTemplate$load(ValueInput input, CallbackInfo ci) {
		hopperTemplate$flags = input.getIntOr(TAG, 0);
	}
}
