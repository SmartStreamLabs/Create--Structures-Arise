package createstructuresarise.config;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class DynamicRandomSpreadStructurePlacement extends RandomSpreadStructurePlacement {
	private final Identifier structureId;
	private final int defaultSpacing;
	private final int defaultSeparation;
	private final RandomSpreadType defaultSpreadType;

	public DynamicRandomSpreadStructurePlacement(Identifier structureId, int defaultSpacing, int defaultSeparation, RandomSpreadType defaultSpreadType, int salt) {
		super(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, salt, java.util.Optional.empty(), defaultSpacing, defaultSeparation, defaultSpreadType);
		this.structureId = structureId;
		this.defaultSpacing = defaultSpacing;
		this.defaultSeparation = defaultSeparation;
		this.defaultSpreadType = defaultSpreadType;
	}

	@Override
	public int spacing() {
		return CreateStructuresAriseServerConfig.effectiveSpacing(this.structureId, this.defaultSpacing, this.defaultSeparation);
	}

	@Override
	public int separation() {
		return CreateStructuresAriseServerConfig.effectiveSeparation(this.structureId, this.defaultSeparation);
	}

	@Override
	public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
		int spacing = this.spacing();
		int separation = this.separation();
		int regionX = Math.floorDiv(chunkX, spacing);
		int regionZ = Math.floorDiv(chunkZ, spacing);
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, regionX, regionZ, this.salt());
		int range = spacing - separation;
		int offsetX = this.defaultSpreadType.evaluate(random, range);
		int offsetZ = this.defaultSpreadType.evaluate(random, range);
		return new ChunkPos(regionX * spacing + offsetX, regionZ * spacing + offsetZ);
	}
}
