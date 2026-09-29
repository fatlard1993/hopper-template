package justfatlard.hopper_template;

import justfatlard.hopper_template.screen.HopperScreen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MOD_ID = "hopper-template";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HopperScreen.register();
		LOGGER.info("Hoppers can keep a sample in each slot, and shut the slots they are not using");
	}
}
