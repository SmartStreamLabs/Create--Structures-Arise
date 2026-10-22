package createstructuresarise.command;

import com.mojang.brigadier.CommandDispatcher;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class CreateAriseCommands {
	private CreateAriseCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("createarise")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reload")
						.executes(context -> {
							CreateStructuresAriseServerConfig.load();
							context.getSource().sendSuccess(() -> Component.literal("Create Structures Arise server config reloaded."), true);
							return 1;
						})));
	}
}
