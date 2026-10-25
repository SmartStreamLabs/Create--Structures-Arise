package createstructuresarise;

import createstructuresarise.command.CreateAriseCommands;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateStructuresAriseMod.MODID)
public class CreateStructuresAriseMod {
	public static final Logger LOGGER = LogManager.getLogger(CreateStructuresAriseMod.class);
	public static final String MODID = "create_structures_arise";

	public CreateStructuresAriseMod() {
		CreateStructuresAriseServerConfig.load();
		NeoForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		CreateAriseCommands.register(event.getDispatcher());
	}
}
