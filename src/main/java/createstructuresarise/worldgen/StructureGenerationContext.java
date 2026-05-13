package createstructuresarise.worldgen;

import createstructuresarise.CreateStructuresAriseMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

public final class StructureGenerationContext {
	private static final ThreadLocal<ResourceLocation> CURRENT_DIMENSION = new ThreadLocal<>();
	private static final ThreadLocal<ResourceLocation> CURRENT_TEMPLATE = new ThreadLocal<>();

	private StructureGenerationContext() {
	}

	public static void setDimension(ResourceLocation dimension) {
		CURRENT_DIMENSION.set(dimension);
	}

	public static ResourceLocation getDimension() {
		ResourceLocation dimension = CURRENT_DIMENSION.get();
		return dimension != null ? dimension : net.minecraft.world.level.Level.OVERWORLD.location();
	}

	public static void clear() {
		CURRENT_DIMENSION.remove();
	}

	public static void setTemplate(StructurePoolElement element) {
		String value = element.toString();
		int namespaceIndex = value.indexOf(CreateStructuresAriseMod.MODID + ":");
		if (namespaceIndex < 0) {
			CURRENT_TEMPLATE.remove();
			return;
		}
		int start = namespaceIndex + CreateStructuresAriseMod.MODID.length() + 1;
		int end = start;
		while (end < value.length()) {
			char character = value.charAt(end);
			if (!(Character.isLetterOrDigit(character) || character == '_' || character == '/' || character == '-' || character == '.')) {
				break;
			}
			end++;
		}
		CURRENT_TEMPLATE.set(new ResourceLocation(CreateStructuresAriseMod.MODID, value.substring(start, end)));
	}

	public static ResourceLocation getTemplate() {
		return CURRENT_TEMPLATE.get();
	}

	public static boolean isPlacingCreateStructuresAriseTemplate() {
		return CURRENT_TEMPLATE.get() != null;
	}

	public static void clearTemplate() {
		CURRENT_TEMPLATE.remove();
	}
}
