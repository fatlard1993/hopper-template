package justfatlard.hopper_template.gametest;

import justfatlard.pandorical.gametest.Pictures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The pictures for the readme and the mod page: a hopper made into a filter: a sample in each slot it uses, Keep on, and the slots it does not use shut.
 */
public final class Showcase implements FabricClientGameTest {
	private static final long SEED = 20261005L;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = Pictures.world(context, SEED)) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			Pictures.stage(context, server, Pictures.MORNING);

			BlockPos at = server.computeOnServer(s -> {
				BlockPos spawn = connection.getServerPlayer().blockPosition();
				return Pictures.dryGround(s.overworld(), spawn.getX(), spawn.getZ(), 16);
			});
			if (at == null) throw new AssertionError("no dry ground near spawn");
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(at, Blocks.HOPPER.defaultBlockState());
				var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(at);
				hopper.setItem(0, new ItemStack(Items.IRON_INGOT));
				hopper.setItem(1, new ItemStack(Items.GOLD_INGOT));
				hopper.setItem(2, new ItemStack(Items.REDSTONE));
				var template = justfatlard.hopper_template.Template.of(hopper);
				template.keepsLast(true);
				template.slotOff(3, true);
				template.slotOff(4, true);
				hopper.setChanged();
			});
			Pictures.carry(server, connection,
				new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_AXE), new ItemStack(Items.TORCH, 48),
				new ItemStack(Items.BREAD, 12), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.OAK_PLANKS, 30), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.STONE_SWORD));
			Pictures.open(context, server, connection, at);

			Pictures.shootScreen(context, connection, "filter-screen");
		}
	}
}
