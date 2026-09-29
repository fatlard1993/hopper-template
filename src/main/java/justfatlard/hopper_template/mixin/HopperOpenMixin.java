package justfatlard.hopper_template.mixin;

import justfatlard.hopper_template.screen.HopperScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Serves the hopper's own screen instead of the plain one.
 *
 * <p>On the block rather than on Fabric's UseBlockCallback, for the reason chest-utils gives: the
 * event stops at the first handler that answers, so taking it would decide by mod load order
 * whether anything else watching a hopper being opened ever hears about it.
 *
 * <p>A client that cannot draw Pandorical's screens gets vanilla's, and every switch on this one
 * keeps working - the settings live on the hopper and are read by the transfer code, not by the
 * screen. Such a player simply has no way to change them.
 */
@Mixin(value = HopperBlock.class, priority = 1500)
public class HopperOpenMixin {

	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, require = 1)
	private void hopperTemplate$openWithSwitches(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.isCancelled()) return;
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;
		if (player.isSpectator()) return;
		if (!HopperScreen.canShow(serverPlayer)) return;
		if (!(level.getBlockEntity(pos) instanceof HopperBlockEntity hopper)) return;

		HopperScreen.show(serverPlayer, (ServerLevel) level, pos, hopper);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
