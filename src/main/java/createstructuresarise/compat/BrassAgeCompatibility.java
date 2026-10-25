package createstructuresarise.compat;

import createstructuresarise.CreateStructuresAriseMod;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import createstructuresarise.config.CreateStructuresAriseServerConfig.BrassAgeCompatibilityConfig;
import createstructuresarise.worldgen.StructureGenerationContext;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;

public final class BrassAgeCompatibility {
	private static final Set<String> BRASS_AGE_MOD_IDS = Set.of("brass_age", "brassage", "the_brass_age", "brass-age");
	private static final Set<String> BRASS_AGE_NAMESPACES = Set.of("brass_age", "brassage", "the_brass_age");
	private static final List<String> MACHINE_KEYWORDS = List.of(
			"machine",
			"engine",
			"motor",
			"press",
			"crusher",
			"mixer",
			"mill",
			"lathe",
			"drill",
			"grinder",
			"conveyor",
			"deployer",
			"funnel",
			"chute",
			"gearbox",
			"cog",
			"shaft",
			"belt",
			"pump",
			"pipe",
			"boiler",
			"burner",
			"generator",
			"controller",
			"assembler",
			"crafter",
			"farm",
			"saw",
			"fan",
			"valve",
			"tank");

	private BrassAgeCompatibility() {
	}

	public static boolean isLoaded() {
		return BRASS_AGE_MOD_IDS.stream().anyMatch(ModList.get()::isLoaded);
	}

	public static StructureTemplate.StructureBlockInfo maybeReplace(StructureTemplate.StructureBlockInfo info) {
		if (!StructureGenerationContext.isPlacingCreateStructuresAriseTemplate()) {
			return info;
		}
		BrassAgeCompatibilityConfig config = CreateStructuresAriseServerConfig.data().brassAgeCompatibility();
		if (!config.removeBrassAgeMachines() && !config.removeBrassAgeDecorations() && !config.replaceMissingBrassAgeBlocks()) {
			return info;
		}
		if (!isLoaded() && !config.replaceMissingBrassAgeBlocks()) {
			return info;
		}

		Identifier blockId = BuiltInRegistries.BLOCK.getKey(info.state().getBlock());
		if (!BRASS_AGE_NAMESPACES.contains(blockId.getNamespace())) {
			return info;
		}

		boolean machine = isMachine(blockId.getPath(), info.nbt() != null);
		boolean shouldReplace = machine && config.removeBrassAgeMachines() || !machine && config.removeBrassAgeDecorations();
		if (!shouldReplace && config.replaceMissingBrassAgeBlocks() && !isLoaded()) {
			shouldReplace = true;
		}
		if (!shouldReplace) {
			return info;
		}

		BlockState replacement = replacementState(config.replacementBlock());
		if (CreateStructuresAriseServerConfig.data().logBlocked() || CreateStructuresAriseServerConfig.data().logAllowed()) {
			Identifier templateId = StructureGenerationContext.getTemplate();
			CreateStructuresAriseMod.LOGGER.info(
					"CSA Brass Age replacement: template={}, pos={}, original_block={}, replacement_block={}, category={}",
					templateId != null ? templateId : "<unknown>",
					info.pos(),
					blockId,
					BuiltInRegistries.BLOCK.getKey(replacement.getBlock()),
					machine ? "machine" : "decoration");
		}
		return new StructureTemplate.StructureBlockInfo(info.pos(), replacement, null);
	}

	private static boolean isMachine(String path, boolean hasBlockEntity) {
		if (hasBlockEntity) {
			return true;
		}
		String lower = path.toLowerCase(Locale.ROOT);
		return MACHINE_KEYWORDS.stream().anyMatch(lower::contains);
	}

	private static BlockState replacementState(String configuredBlock) {
		try {
			Identifier id = Identifier.parse(configuredBlock);
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			if (block != Blocks.AIR || "minecraft:air".equals(id.toString())) {
				return block.defaultBlockState();
			}
		} catch (Exception exception) {
			CreateStructuresAriseMod.LOGGER.warn("Invalid Brass Age replacement_block '{}', using minecraft:air.", configuredBlock);
		}
		return Blocks.AIR.defaultBlockState();
	}
}
