package createstructuresarise.mixin;

import createstructuresarise.CreateStructuresAriseMod;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import createstructuresarise.config.CreateStructuresAriseServerConfig.BlockDecision;
import createstructuresarise.config.CreateStructuresAriseServerConfig.TerrainSample;
import createstructuresarise.worldgen.StructureGenerationContext;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JigsawStructure.class)
public abstract class StructureDebugMixin {
	@Shadow
	@Final
	private Holder<StructureTemplatePool> startPool;

	@Shadow
	@Final
	private Optional<ResourceLocation> startJigsawName;

	@Shadow
	@Final
	private HeightProvider startHeight;

	@Shadow
	@Final
	private Optional<Heightmap.Types> projectStartToHeightmap;

	@Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true)
	private void createStructuresArise$logJigsawSelection(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
		Registry<Structure> structureRegistry = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
		ResourceLocation structureId = structureRegistry.getKey((Structure) (Object) this);
		if (structureId == null || !CreateStructuresAriseMod.MODID.equals(structureId.getNamespace())) {
			return;
		}

		ChunkPos chunkPos = context.chunkPos();
		int sampleX = chunkPos.getMiddleBlockX();
		int sampleZ = chunkPos.getMiddleBlockZ();
		int sampledStartY = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
		String heightmapName = this.projectStartToHeightmap.map(Enum::name).orElse("none");
		String heightmapY = this.projectStartToHeightmap
				.map(type -> Integer.toString(context.chunkGenerator().getFirstFreeHeight(sampleX, sampleZ, type, context.heightAccessor(), context.randomState())))
				.orElse("not_projected");
		Holder<Biome> biome = context.biomeSource()
				.getNoiseBiome(QuartPos.fromBlock(sampleX), QuartPos.fromBlock(sampledStartY), QuartPos.fromBlock(sampleZ), context.randomState().sampler());
		Registry<Biome> biomeRegistry = context.registryAccess().registryOrThrow(Registries.BIOME);
		ResourceLocation biomeId = biomeRegistry.getKey(biome.value());
		String poolId = this.startPool.unwrapKey().map(key -> key.location().toString()).orElse("<unregistered>");
		ResourceLocation dimensionId = StructureGenerationContext.getDimension();
		TerrainSample terrainSample = CreateStructuresAriseServerConfig.isAirborneStructure(structureId) ? sampleTerrain(context, sampleX, sampleZ) : null;
		BlockDecision decision = CreateStructuresAriseServerConfig.canGenerate(structureId, dimensionId, biome, biomeRegistry, terrainSample);

		if (!decision.allowed()) {
			if (CreateStructuresAriseServerConfig.data().logBlocked()) {
				CreateStructuresAriseMod.LOGGER.info(
						"CSA structure blocked by config: id={}, chunk={}, dimension={}, biome={}, sampled_start_y={}, heightmap={}, heightmap_y={}, terrain_center_y={}, terrain_min_y={}, terrain_max_y={}, start_pool={}, reason={}",
						structureId,
						chunkPos,
						dimensionId,
						biomeId,
						sampledStartY,
						heightmapName,
						heightmapY,
						terrainSample != null ? terrainSample.centerHeight() : "not_sampled",
						terrainSample != null ? terrainSample.minHeight() : "not_sampled",
						terrainSample != null ? terrainSample.maxHeight() : "not_sampled",
						poolId,
						decision.reason());
			}
			cir.setReturnValue(Optional.empty());
			return;
		}

		if (CreateStructuresAriseServerConfig.data().logAllowed()) {
			CreateStructuresAriseMod.LOGGER.info(
					"CSA structure allowed by config: id={}, chunk={}, dimension={}, biome={}, sampled_start_y={}, heightmap={}, heightmap_y={}, terrain_center_y={}, terrain_min_y={}, terrain_max_y={}, start_pool={}, start_jigsaw_name={}, reason={}",
					structureId,
					chunkPos,
					dimensionId,
					biomeId,
					sampledStartY,
					heightmapName,
					heightmapY,
					terrainSample != null ? terrainSample.centerHeight() : "not_sampled",
					terrainSample != null ? terrainSample.minHeight() : "not_sampled",
					terrainSample != null ? terrainSample.maxHeight() : "not_sampled",
					poolId,
					this.startJigsawName.map(ResourceLocation::toString).orElse("<none>"),
					decision.reason());
		}
	}

	private static TerrainSample sampleTerrain(Structure.GenerationContext context, int centerX, int centerZ) {
		int radius = CreateStructuresAriseServerConfig.data().worldgenCompatibility().checkRadius();
		int[][] offsets = {
				{0, 0},
				{0, -radius},
				{0, radius},
				{-radius, 0},
				{radius, 0},
				{-radius, -radius},
				{-radius, radius},
				{radius, -radius},
				{radius, radius}
		};
		int centerHeight = heightAt(context, centerX, centerZ);
		int minHeight = centerHeight;
		int maxHeight = centerHeight;
		for (int[] offset : offsets) {
			int height = heightAt(context, centerX + offset[0], centerZ + offset[1]);
			minHeight = Math.min(minHeight, height);
			maxHeight = Math.max(maxHeight, height);
		}
		return new TerrainSample(centerHeight, minHeight, maxHeight);
	}

	private static int heightAt(Structure.GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
	}
}
