package createstructuresarise.mixin;

import createstructuresarise.CreateStructuresAriseMod;
import createstructuresarise.config.CreateStructuresAriseServerConfig;
import createstructuresarise.config.DynamicRandomSpreadStructurePlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin {
	@Shadow
	@Final
	@Mutable
	private List<Holder<StructureSet>> possibleStructureSets;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void createStructuresArise$wrapPlacements(RandomState randomState, BiomeSource biomeSource, long levelSeed, long concentricRingsSeed, List<Holder<StructureSet>> possibleStructureSets, CallbackInfo ci) {
		List<Holder<StructureSet>> wrapped = new ArrayList<>(this.possibleStructureSets.size());
		for (Holder<StructureSet> holder : this.possibleStructureSets) {
			StructureSet set = holder.value();
			if (set.structures().size() == 1 && set.placement() instanceof RandomSpreadStructurePlacement placement) {
				Identifier structureId = set.structures().get(0).structure().unwrapKey().map(key -> key.identifier()).orElse(null);
				if (structureId != null && CreateStructuresAriseMod.MODID.equals(structureId.getNamespace()) && CreateStructuresAriseServerConfig.defaults().containsKey(structureId)) {
					wrapped.add(Holder.direct(new StructureSet(
							set.structures(),
							new DynamicRandomSpreadStructurePlacement(structureId, placement.spacing(), placement.separation(), placement.spreadType(), ((StructurePlacementAccessor) placement).createStructuresArise$getSalt()))));
					continue;
				}
			}
			wrapped.add(holder);
		}
		this.possibleStructureSets = List.copyOf(wrapped);
	}
}
