package justfatlard.hopper_template.mixin;

import justfatlard.hopper_template.Template;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A slot that is off, or is keeping its last item, gives nothing up.
 *
 * <p>Both ways an item leaves a hopper start by reading the slot: the hopper pushing into whatever
 * is below it ({@code ejectItems}), and the hopper below reading this one ({@code
 * tryTakeInItemFromSlot}). Both then check the stack for empty. So a held-back slot is answered as
 * empty and both paths skip it, which is one idea implemented once rather than two rules that can
 * disagree.
 *
 * <p>Answered as empty rather than refused later on purpose: vanilla's eject loop moves to the next
 * slot when a slot is empty, and a hopper whose second slot is held back should still push from its
 * third.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperHoldsBackMixin {

	/** The hopper pushing what it has into whatever it points at. */
	@Redirect(
		method = "ejectItems",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
	private static ItemStack hopperTemplate$whatThisHopperWillPush(HopperBlockEntity self, int slot) {
		Template template = Template.of(self);
		return template != null && !template.releases(self, slot) ? ItemStack.EMPTY : self.getItem(slot);
	}

	/** Something reading this hopper: the next hopper down, or a minecart under it. */
	@Redirect(
		method = "tryTakeInItemFromSlot",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
	private static ItemStack hopperTemplate$whatThisHopperWillGive(Container source, int slot,
			Hopper hopper, Container container, int slotAgain, Direction direction) {
		Template template = Template.of(source);
		return template != null && !template.releases(source, slot) ? ItemStack.EMPTY : source.getItem(slot);
	}

}
