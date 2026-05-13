package createstructuresarise;

import createstructuresarise.command.CreateAriseCommands;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateStructuresAriseMod.MODID)
public class CreateStructuresAriseMod {
	public static final Logger LOGGER = LogManager.getLogger(CreateStructuresAriseMod.class);
	public static final String MODID = "create_structures_arise";

	public CreateStructuresAriseMod() {
		CreateStructuresAriseServerConfig.load();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		CreateAriseCommands.register(event.getDispatcher());
	}
}
