package createstructuresarise.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import createstructuresarise.CreateStructuresAriseMod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.loading.FMLPaths;

public final class CreateStructuresAriseServerConfig {
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("create_structures_arise-server.toml");
	private static final Map<Identifier, StructureDefaults> DEFAULTS = createDefaults();
	private static final Set<Identifier> AIRBORNE_STRUCTURES = Set.of(id("createairdrop"), id("createminiskyvillage"), id("pillagersteampunkairship"));
	private static final TagKey<Biome> AIRBORNE_MOUNTAIN_BIOMES = biomeTag("airborne_structure_avoid_biomes");
	private static final TagKey<Biome> AIRBORNE_TECTONIC_MOUNTAIN_BIOMES = biomeTag("airborne_structure_avoid_tectonic_biomes");
	private static final TagKey<Biome> AIRBORNE_TERRALITH_MOUNTAIN_BIOMES = biomeTag("airborne_structure_avoid_terralith_biomes");
	private static final TagKey<Biome> AIRBORNE_BIOMES_O_PLENTY_MOUNTAIN_BIOMES = biomeTag("airborne_structure_avoid_biomes_o_plenty_biomes");
	private static volatile ConfigData data = ConfigData.defaults(DEFAULTS);

	private CreateStructuresAriseServerConfig() {
	}

	public static void load() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH).sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build()) {
				config.load();
				ConfigData loaded = read(config);
				data = loaded;
				write(config, loaded);
				config.save();
			}
		} catch (Exception exception) {
			CreateStructuresAriseMod.LOGGER.error("Failed to load Create Structures Arise server config. Keeping previous values.", exception);
		}
	}

	public static ConfigData data() {
		return data;
	}

	public static Map<Identifier, StructureDefaults> defaults() {
		return DEFAULTS;
	}

	public static boolean isAirborneStructure(Identifier structureId) {
		return AIRBORNE_STRUCTURES.contains(structureId);
	}

	public static StructureConfig structure(Identifier structureId) {
		return data.structures().getOrDefault(structureId, StructureConfig.fromDefaults(DEFAULTS.get(structureId)));
	}

	public static int effectiveSpacing(Identifier structureId, int fallbackSpacing, int fallbackSeparation) {
		ConfigData snapshot = data;
		StructureConfig config = snapshot.structures().get(structureId);
		int configuredSpacing = config != null ? config.spacing() : fallbackSpacing;
		int configuredSeparation = config != null ? config.separation() : fallbackSeparation;
		int configuredWeight = config != null ? config.weight() : 1;
		double multiplier = snapshot.globalSpawnMultiplier() * snapshot.spawnRates().globalStructureMultiplier() * Math.max(1, configuredWeight);
		if (multiplier <= 0.0D) {
			return Math.max(configuredSeparation + 1, configuredSpacing);
		}
		int scaled = (int) Math.round(configuredSpacing / multiplier);
		return Math.max(configuredSeparation + 1, Math.max(1, scaled));
	}

	public static int effectiveSeparation(Identifier structureId, int fallbackSeparation) {
		StructureConfig config = data.structures().get(structureId);
		return config != null ? config.separation() : fallbackSeparation;
	}

	public static BlockDecision canGenerate(Identifier structureId, Identifier dimensionId, Holder<Biome> biome, Registry<Biome> biomeRegistry, TerrainSample terrainSample) {
		ConfigData snapshot = data;
		StructureConfig config = snapshot.structures().get(structureId);
		if (config == null) {
			return BlockDecision.allow("not configured by Create Structures Arise");
		}
		if (!snapshot.globalEnabled()) {
			return BlockDecision.block("global generation is disabled");
		}
		if (snapshot.globalSpawnMultiplier() <= 0.0D) {
			return BlockDecision.block("global_spawn_multiplier is 0");
		}
		if (snapshot.spawnRates().globalStructureMultiplier() <= 0.0D) {
			return BlockDecision.block("global_structure_multiplier is 0");
		}
		if (!config.enabled()) {
			return BlockDecision.block("structure is disabled");
		}
		if (config.weight() == 0) {
			return BlockDecision.block("structure weight is 0");
		}
		if (!config.dimensionWhitelist().isEmpty() && !config.dimensionWhitelist().contains(dimensionId.toString())) {
			return BlockDecision.block("dimension is not in dimension_whitelist");
		}
		if (config.dimensionBlacklist().contains(dimensionId.toString())) {
			return BlockDecision.block("dimension is in dimension_blacklist");
		}
		if (!config.biomeWhitelist().isEmpty() && !matchesBiomeList(config.biomeWhitelist(), biome, biomeRegistry)) {
			return BlockDecision.block("biome is not in biome_whitelist");
		}
		if (matchesBiomeList(config.biomeBlacklist(), biome, biomeRegistry)) {
			return BlockDecision.block("biome is in biome_blacklist");
		}
		if (AIRBORNE_STRUCTURES.contains(structureId)) {
			WorldgenCompatibilityConfig compatibility = snapshot.worldgenCompatibility();
			if (compatibility.avoidMountainBiomesForAirborneStructures() && isBlockedAirborneBiome(biome, compatibility)) {
				return BlockDecision.block("airborne structure blocked in mountain/extreme terrain biome");
			}
			if (compatibility.avoidSteepTerrain() && terrainSample != null && terrainSample.heightDifference() > compatibility.maxAllowedHeightDifference()) {
				return BlockDecision.block("airborne structure blocked by steep terrain");
			}
		}
		return BlockDecision.allow("config allows generation");
	}

	private static boolean isBlockedAirborneBiome(Holder<Biome> biome, WorldgenCompatibilityConfig compatibility) {
		return biome.is(AIRBORNE_MOUNTAIN_BIOMES)
				|| compatibility.blacklistTectonicMountainBiomes() && biome.is(AIRBORNE_TECTONIC_MOUNTAIN_BIOMES)
				|| compatibility.blacklistTerralithMountainBiomes() && biome.is(AIRBORNE_TERRALITH_MOUNTAIN_BIOMES)
				|| compatibility.blacklistBiomesOPlentyMountainBiomes() && biome.is(AIRBORNE_BIOMES_O_PLENTY_MOUNTAIN_BIOMES);
	}

	private static boolean matchesBiomeList(List<String> entries, Holder<Biome> biome, Registry<Biome> biomeRegistry) {
		for (String entry : entries) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				if (trimmed.startsWith("#")) {
					Identifier tagId = Identifier.parse(trimmed.substring(1));
					if (biome.is(TagKey.create(Registries.BIOME, tagId))) {
						return true;
					}
				} else {
					Identifier biomeId = Identifier.parse(trimmed);
					if (biomeRegistry.getKey(biome.value()).equals(biomeId)) {
						return true;
					}
				}
			} catch (Exception exception) {
				CreateStructuresAriseMod.LOGGER.warn("Ignoring invalid biome config entry '{}'", trimmed);
			}
		}
		return false;
	}

	private static ConfigData read(CommentedFileConfig config) {
		boolean globalEnabled = readBoolean(config, List.of("global", "enabled"), true);
		double globalSpawnMultiplier = Math.max(0.0D, readDouble(config, List.of("global", "spawn_multiplier"), 1.0D));
		SpawnRatesConfig spawnRates = new SpawnRatesConfig(Math.max(0.0D, readDouble(config, List.of("spawn_rates", "global_structure_multiplier"), 1.0D)));
		boolean logBlocked = readBoolean(config, List.of("debug", "log_blocked"), false);
		boolean logAllowed = readBoolean(config, List.of("debug", "log_allowed"), false);
		BrassAgeCompatibilityConfig brassAgeCompatibility = new BrassAgeCompatibilityConfig(
				readBoolean(config, List.of("compatibility", "brass_age", "remove_brass_age_machines"), false),
				readBoolean(config, List.of("compatibility", "brass_age", "remove_brass_age_decorations"), false),
				readBoolean(config, List.of("compatibility", "brass_age", "replace_missing_brass_age_blocks"), true),
				readString(config, List.of("compatibility", "brass_age", "replacement_block"), "minecraft:air"));
		WorldgenCompatibilityConfig worldgenCompatibility = new WorldgenCompatibilityConfig(
				readBoolean(config, List.of("compatibility", "worldgen", "avoid_mountain_biomes_for_airborne_structures"), true),
				readBoolean(config, List.of("compatibility", "worldgen", "avoid_steep_terrain"), true),
				Math.max(1, readInt(config, List.of("compatibility", "worldgen", "max_allowed_height_difference"), 18)),
				Math.max(1, readInt(config, List.of("compatibility", "worldgen", "check_radius"), 32)),
				readBoolean(config, List.of("compatibility", "worldgen", "blacklist_tectonic_mountain_biomes"), true),
				readBoolean(config, List.of("compatibility", "worldgen", "blacklist_terralith_mountain_biomes"), true),
				readBoolean(config, List.of("compatibility", "worldgen", "blacklist_biomes_o_plenty_mountain_biomes"), true));
		Map<Identifier, StructureConfig> structures = new LinkedHashMap<>();

		for (StructureDefaults defaults : DEFAULTS.values()) {
			List<String> base = List.of("structures", defaults.id().getPath());
			boolean enabled = readBoolean(config, append(base, "enabled"), true);
			int weight = Math.max(0, readInt(config, append(base, "weight"), defaults.weight()));
			int separation = Math.max(0, readInt(config, append(base, "separation"), defaults.separation()));
			int spacing = Math.max(1, readInt(config, append(base, "spacing"), defaults.spacing()));
			if (spacing <= separation) {
				CreateStructuresAriseMod.LOGGER.warn(
						"Invalid spacing/separation for {} in server config: spacing={} separation={}. Using defaults {} / {}.",
						defaults.id(),
						spacing,
						separation,
						defaults.spacing(),
						defaults.separation());
				spacing = defaults.spacing();
				separation = defaults.separation();
			}
			structures.put(defaults.id(), new StructureConfig(
					enabled,
					weight,
					spacing,
					separation,
					readStringList(config, append(base, "dimension_whitelist")),
					readStringList(config, append(base, "dimension_blacklist")),
					readStringList(config, append(base, "biome_whitelist")),
					readStringList(config, append(base, "biome_blacklist"))));
		}

		return new ConfigData(globalEnabled, globalSpawnMultiplier, spawnRates, logBlocked, logAllowed, brassAgeCompatibility, worldgenCompatibility, structures);
	}

	private static void write(CommentedFileConfig config, ConfigData loaded) {
		config.setComment(List.of("global", "enabled"), "Global switch for all Create Structures Arise structures. Set to false to prevent every structure from generating.");
		config.set(List.of("global", "enabled"), loaded.globalEnabled());
		config.setComment(List.of("global", "spawn_multiplier"), "Global spawn density multiplier. 1.0 keeps original spacing, values above 1.0 spawn more often, values below 1.0 spawn less often, and 0 blocks generation.");
		config.set(List.of("global", "spawn_multiplier"), loaded.globalSpawnMultiplier());
		config.setComment(List.of("spawn_rates", "global_structure_multiplier"), "Additional global multiplier for all Create Structures Arise structure spawn rates. 1.0 preserves current behavior, lower values make structures less frequent, and 0 blocks generation.");
		config.set(List.of("spawn_rates", "global_structure_multiplier"), loaded.spawnRates().globalStructureMultiplier());
		config.setComment(List.of("debug", "log_blocked"), "When true, logs whenever a Create Structures Arise structure is blocked by this config.");
		config.set(List.of("debug", "log_blocked"), loaded.logBlocked());
		config.setComment(List.of("debug", "log_allowed"), "When true, logs whenever a Create Structures Arise structure is allowed by this config.");
		config.set(List.of("debug", "log_allowed"), loaded.logAllowed());
		config.setComment(List.of("compatibility", "brass_age", "remove_brass_age_machines"), "When true and Brass Age is loaded, Brass Age machine blocks and block entities in Create Structures Arise structures are replaced during placement.");
		config.set(List.of("compatibility", "brass_age", "remove_brass_age_machines"), loaded.brassAgeCompatibility().removeBrassAgeMachines());
		config.setComment(List.of("compatibility", "brass_age", "remove_brass_age_decorations"), "When true and Brass Age is loaded, decorative Brass Age blocks in Create Structures Arise structures are replaced during placement.");
		config.set(List.of("compatibility", "brass_age", "remove_brass_age_decorations"), loaded.brassAgeCompatibility().removeBrassAgeDecorations());
		config.setComment(List.of("compatibility", "brass_age", "replace_missing_brass_age_blocks"), "When true, missing or disabled Brass Age blocks are replaced with replacement_block instead of being kept when possible.");
		config.set(List.of("compatibility", "brass_age", "replace_missing_brass_age_blocks"), loaded.brassAgeCompatibility().replaceMissingBrassAgeBlocks());
		config.setComment(List.of("compatibility", "brass_age", "replacement_block"), "Block ID used when Brass Age components are removed from structures.");
		config.set(List.of("compatibility", "brass_age", "replacement_block"), loaded.brassAgeCompatibility().replacementBlock());
		config.setComment(List.of("compatibility", "worldgen", "avoid_mountain_biomes_for_airborne_structures"), "When true, airborne structures avoid known mountain, hill, peak, cliff, skyland, and extreme-terrain biome tags.");
		config.set(List.of("compatibility", "worldgen", "avoid_mountain_biomes_for_airborne_structures"), loaded.worldgenCompatibility().avoidMountainBiomesForAirborneStructures());
		config.setComment(List.of("compatibility", "worldgen", "avoid_steep_terrain"), "When true, airborne structures sample nearby terrain and refuse generation when the area is too steep.");
		config.set(List.of("compatibility", "worldgen", "avoid_steep_terrain"), loaded.worldgenCompatibility().avoidSteepTerrain());
		config.setComment(List.of("compatibility", "worldgen", "max_allowed_height_difference"), "Maximum allowed difference between sampled minimum and maximum terrain height for airborne structures.");
		config.set(List.of("compatibility", "worldgen", "max_allowed_height_difference"), loaded.worldgenCompatibility().maxAllowedHeightDifference());
		config.setComment(List.of("compatibility", "worldgen", "check_radius"), "Block radius used for airborne steep-terrain sampling. North, south, east, west, and corner points are checked.");
		config.set(List.of("compatibility", "worldgen", "check_radius"), loaded.worldgenCompatibility().checkRadius());
		config.setComment(List.of("compatibility", "worldgen", "blacklist_tectonic_mountain_biomes"), "When true, airborne structures avoid Tectonic mountain/extreme-terrain compatibility biome tags when present.");
		config.set(List.of("compatibility", "worldgen", "blacklist_tectonic_mountain_biomes"), loaded.worldgenCompatibility().blacklistTectonicMountainBiomes());
		config.setComment(List.of("compatibility", "worldgen", "blacklist_terralith_mountain_biomes"), "When true, airborne structures avoid Terralith mountain, cliff, peak, skyland, and extreme-terrain compatibility biome tags when present.");
		config.set(List.of("compatibility", "worldgen", "blacklist_terralith_mountain_biomes"), loaded.worldgenCompatibility().blacklistTerralithMountainBiomes());
		config.setComment(List.of("compatibility", "worldgen", "blacklist_biomes_o_plenty_mountain_biomes"), "When true, airborne structures avoid Biomes O' Plenty mountain, highland, cliff, and crag compatibility biome tags when present.");
		config.set(List.of("compatibility", "worldgen", "blacklist_biomes_o_plenty_mountain_biomes"), loaded.worldgenCompatibility().blacklistBiomesOPlentyMountainBiomes());

		for (StructureDefaults defaults : DEFAULTS.values()) {
			StructureConfig structure = loaded.structures().get(defaults.id());
			List<String> base = List.of("structures", defaults.id().getPath());
			config.setComment(append(base, "enabled"), "Enables this structure. Set to false to prevent this structure from generating.");
			config.set(append(base, "enabled"), structure.enabled());
			config.setComment(append(base, "weight"), "Generation weight for this structure. Cannot be negative. Set to 0 to prevent this structure from generating. Values above 1 make this structure more common.");
			config.set(append(base, "weight"), structure.weight());
			config.setComment(append(base, "spacing"), "Average chunk spacing for this structure before the global spawn multiplier is applied. Must be greater than separation.");
			config.set(append(base, "spacing"), structure.spacing());
			config.setComment(append(base, "separation"), "Minimum chunk separation for this structure. Must be lower than spacing.");
			config.set(append(base, "separation"), structure.separation());
			config.setComment(append(base, "dimension_whitelist"), "Dimension IDs where this structure may generate. Empty list means all dimensions are allowed unless blacklisted.");
			config.set(append(base, "dimension_whitelist"), structure.dimensionWhitelist());
			config.setComment(append(base, "dimension_blacklist"), "Dimension IDs where this structure must not generate. Entries here override the whitelist.");
			config.set(append(base, "dimension_blacklist"), structure.dimensionBlacklist());
			config.setComment(append(base, "biome_whitelist"), "Biome IDs or biome tags where this structure may generate. Use '#namespace:tag' for tags. Empty list means all original datapack biomes are allowed unless blacklisted.");
			config.set(append(base, "biome_whitelist"), structure.biomeWhitelist());
			config.setComment(append(base, "biome_blacklist"), "Biome IDs or biome tags where this structure must not generate. Use '#namespace:tag' for tags. Entries here override the whitelist.");
			config.set(append(base, "biome_blacklist"), structure.biomeBlacklist());
		}
	}

	private static boolean readBoolean(CommentedFileConfig config, List<String> path, boolean defaultValue) {
		Object value = config.get(path);
		return value instanceof Boolean bool ? bool : defaultValue;
	}

	private static int readInt(CommentedFileConfig config, List<String> path, int defaultValue) {
		Object value = config.get(path);
		return value instanceof Number number ? number.intValue() : defaultValue;
	}

	private static double readDouble(CommentedFileConfig config, List<String> path, double defaultValue) {
		Object value = config.get(path);
		return value instanceof Number number ? number.doubleValue() : defaultValue;
	}

	private static String readString(CommentedFileConfig config, List<String> path, String defaultValue) {
		Object value = config.get(path);
		return value instanceof String string && !string.trim().isEmpty() ? string.trim() : defaultValue;
	}

	private static List<String> readStringList(CommentedFileConfig config, List<String> path) {
		Object value = config.get(path);
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (Object entry : list) {
			if (entry instanceof String string && !string.trim().isEmpty()) {
				result.add(string.trim().toLowerCase(Locale.ROOT));
			}
		}
		return List.copyOf(result);
	}

	private static List<String> append(List<String> base, String key) {
		List<String> path = new ArrayList<>(base);
		path.add(key);
		return path;
	}

	private static Map<Identifier, StructureDefaults> createDefaults() {
		Map<Identifier, StructureDefaults> defaults = new LinkedHashMap<>();
		add(defaults, "chocolate_pit", 1, 130, 10);
		add(defaults, "create_bastion", 1, 100, 10);
		add(defaults, "create_copper_statue", 1, 90, 15);
		add(defaults, "create_dungeon_base", 1, 200, 4);
		add(defaults, "create_ruined_castle", 1, 90, 15);
		add(defaults, "createairdrop", 1, 90, 30);
		add(defaults, "createcontainer", 1, 120, 30);
		add(defaults, "createcushercrane", 1, 120, 15);
		add(defaults, "createdesertwell", 1, 120, 2);
		add(defaults, "createfluidtank", 1, 90, 30);
		add(defaults, "createhouse", 1, 120, 70);
		add(defaults, "createlittleman", 1, 120, 15);
		add(defaults, "createlosttrainstation", 1, 90, 30);
		add(defaults, "createminiskyvillage", 1, 120, 22);
		add(defaults, "createmonsterroom", 1, 90, 2);
		add(defaults, "createoldruine", 1, 90, 15);
		add(defaults, "createpickaxestatue", 1, 90, 15);
		add(defaults, "createwitchhut", 1, 120, 2);
		add(defaults, "crimsite_tower", 1, 150, 2);
		add(defaults, "darkcastle", 1, 100, 70);
		add(defaults, "deadlordshouse", 1, 50, 15);
		add(defaults, "honey_pit", 1, 130, 10);
		add(defaults, "minetrain", 1, 100, 20);
		add(defaults, "obsidiantemple", 1, 60, 15);
		add(defaults, "pillager_boat", 1, 150, 70);
		add(defaults, "pillagersteampunkairship", 1, 150, 50);
		add(defaults, "towerofochrum", 1, 180, 10);
		add(defaults, "windmill", 1, 90, 10);
		return Map.copyOf(defaults);
	}

	private static void add(Map<Identifier, StructureDefaults> defaults, String id, int weight, int spacing, int separation) {
		Identifier structureId = id(id);
		defaults.put(structureId, new StructureDefaults(structureId, weight, spacing, separation));
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(CreateStructuresAriseMod.MODID, path);
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(CreateStructuresAriseMod.MODID, path));
	}

	public record StructureDefaults(Identifier id, int weight, int spacing, int separation) {
	}

	public record StructureConfig(
			boolean enabled,
			int weight,
			int spacing,
			int separation,
			List<String> dimensionWhitelist,
			List<String> dimensionBlacklist,
			List<String> biomeWhitelist,
			List<String> biomeBlacklist) {
		static StructureConfig fromDefaults(StructureDefaults defaults) {
			return new StructureConfig(true, defaults != null ? defaults.weight() : 1, defaults != null ? defaults.spacing() : 1, defaults != null ? defaults.separation() : 0, List.of(), List.of(), List.of(), List.of());
		}
	}

	public record ConfigData(
			boolean globalEnabled,
			double globalSpawnMultiplier,
			SpawnRatesConfig spawnRates,
			boolean logBlocked,
			boolean logAllowed,
			BrassAgeCompatibilityConfig brassAgeCompatibility,
			WorldgenCompatibilityConfig worldgenCompatibility,
			Map<Identifier, StructureConfig> structures) {
		static ConfigData defaults(Map<Identifier, StructureDefaults> defaults) {
			Map<Identifier, StructureConfig> structures = new LinkedHashMap<>();
			defaults.forEach((id, value) -> structures.put(id, StructureConfig.fromDefaults(value)));
			return new ConfigData(true, 1.0D, SpawnRatesConfig.defaults(), false, false, BrassAgeCompatibilityConfig.defaults(), WorldgenCompatibilityConfig.defaults(), Map.copyOf(structures));
		}
	}

	public record SpawnRatesConfig(double globalStructureMultiplier) {
		static SpawnRatesConfig defaults() {
			return new SpawnRatesConfig(1.0D);
		}
	}

	public record BrassAgeCompatibilityConfig(
			boolean removeBrassAgeMachines,
			boolean removeBrassAgeDecorations,
			boolean replaceMissingBrassAgeBlocks,
			String replacementBlock) {
		static BrassAgeCompatibilityConfig defaults() {
			return new BrassAgeCompatibilityConfig(false, false, true, "minecraft:air");
		}
	}

	public record WorldgenCompatibilityConfig(
			boolean avoidMountainBiomesForAirborneStructures,
			boolean avoidSteepTerrain,
			int maxAllowedHeightDifference,
			int checkRadius,
			boolean blacklistTectonicMountainBiomes,
			boolean blacklistTerralithMountainBiomes,
			boolean blacklistBiomesOPlentyMountainBiomes) {
		static WorldgenCompatibilityConfig defaults() {
			return new WorldgenCompatibilityConfig(true, true, 18, 32, true, true, true);
		}
	}

	public record TerrainSample(int centerHeight, int minHeight, int maxHeight) {
		public int heightDifference() {
			return this.maxHeight - this.minHeight;
		}
	}

	public record BlockDecision(boolean allowed, String reason) {
		static BlockDecision allow(String reason) {
			return new BlockDecision(true, reason);
		}

		static BlockDecision block(String reason) {
			return new BlockDecision(false, reason);
		}
	}
}
