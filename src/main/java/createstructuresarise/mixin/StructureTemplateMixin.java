package createstructuresarise.mixin;

import createstructuresarise.compat.BrassAgeCompatibility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
	@Inject(method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
	private static void createStructuresArise$replaceBrassAgeBlocks(ServerLevelAccessor level, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> infos, CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> cir) {
		List<StructureTemplate.StructureBlockInfo> original = cir.getReturnValue();
		List<StructureTemplate.StructureBlockInfo> replaced = new ArrayList<>(original.size());
		boolean changed = false;
		for (StructureTemplate.StructureBlockInfo info : original) {
			StructureTemplate.StructureBlockInfo replacement = BrassAgeCompatibility.maybeReplace(info);
			replaced.add(replacement);
			changed |= replacement != info;
		}
		if (changed) {
			cir.setReturnValue(replaced);
		}
	}
}
