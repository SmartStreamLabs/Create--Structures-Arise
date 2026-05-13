package createstructuresarise.mixin;

import createstructuresarise.worldgen.StructureGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoolElementStructurePiece.class)
public abstract class PoolElementStructurePieceMixin {
	@Shadow
	@Final
	protected StructurePoolElement element;

	@Inject(method = "place", at = @At("HEAD"))
	private void createStructuresArise$setTemplateContext(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox box, BlockPos pos, boolean keepJigsaws, CallbackInfo ci) {
		StructureGenerationContext.setTemplate(this.element);
	}

	@Inject(method = "place", at = @At("RETURN"))
	private void createStructuresArise$clearTemplateContext(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox box, BlockPos pos, boolean keepJigsaws, CallbackInfo ci) {
		StructureGenerationContext.clearTemplate();
	}
}
