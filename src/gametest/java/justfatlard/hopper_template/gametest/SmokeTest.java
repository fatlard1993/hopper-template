package justfatlard.hopper_template.gametest;

import justfatlard.hopper_template.Template;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The mod loads, its mixins bind, and a hopper keeps what it is told to keep.
 *
 * <p>The settings live on the hopper's own block entity, put there by a mixin, so the first thing
 * worth proving is that a hopper in a live world can be asked at all: the cast to {@link Template}
 * is the whole of {@code HopperStateMixin} working or not, and nothing else here can pass without
 * it.
 *
 * <p>Run through {@code computeOnServer} and reported as a returned string rather than by throwing
 * where it happens. Blocks and block entities belong to the server thread - reaching for them from
 * the client's hangs the harness - and an assertion thrown on the server thread is swallowed there
 * rather than failing the test. Both were learned the hard way: the first version of this deadlocked,
 * and the version before that passed while never running a line of itself.
 */
public final class SmokeTest implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("hopper-template")) {
			throw new AssertionError("hopper-template is not loaded: the test is testing nothing");
		}

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			String failure = server.computeOnServer(instance -> check(instance.overworld()));
			if (failure != null) throw new AssertionError(failure);
		}
		// Closing a world still coming out of a pause deadlocks the harness's own tick lock.
		context.waitTicks(10);
	}

	/** Every check, as a message or null. */
	private static String check(ServerLevel level) {
		BlockPos pos = new BlockPos(0, 80, 0);
		level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
		if (!(level.getBlockEntity(pos) instanceof HopperBlockEntity hopper)) {
			return "no hopper where one was just placed";
		}

		Template template = Template.of(hopper);
		if (template == null) return "a hopper is not a Template: HopperStateMixin did not apply";

		if (template.keepsLast() || template.slotOff(0)) return "a new hopper came with settings on";

		// Keep: the last one stays, and a slot with more than one does not.
		hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
		template.keepsLast(true);
		if (template.releases(hopper, 0)) return "a lone item was released with Keep on";
		hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 2));
		if (!template.releases(hopper, 0)) return "Keep held back a slot with two in it";

		// Off: nothing moves either way, however many are in there.
		template.slotOff(1, true);
		hopper.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
		if (template.releases(hopper, 1)) return "a shut slot gave up its items";
		if (hopper.canPlaceItem(1, new ItemStack(Items.COBBLESTONE))) return "a shut slot took an item";
		if (!hopper.canPlaceItem(2, new ItemStack(Items.COBBLESTONE))) return "an open slot refused an item";

		// And the settings survive being written out and read back.
		template.slotOff(4, true);
		TagValueOutput output = TagValueOutput.createWithContext(
			ProblemReporter.DISCARDING, level.registryAccess());
		hopper.saveWithFullMetadata(output);
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
		if (!(level.getBlockEntity(pos) instanceof HopperBlockEntity reloaded)) {
			return "no hopper after replacing it";
		}
		reloaded.loadWithComponents(TagValueInput.create(
			ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
		Template back = Template.of(reloaded);
		if (back == null || !back.keepsLast() || !back.slotOff(1) || !back.slotOff(4)
				|| back.slotOff(2)) {
			return "the settings did not survive a save and load";
		}
		return null;
	}
}
