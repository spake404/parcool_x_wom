package dev.spake404.epm.naturalsprinter;

import dev.spake404.epm.EPM;
import dev.spake404.epm.animation.AnimationQuery;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

public final class NaturalSprinterFastRunAnimationOverrides {
	private static final String DIRECTORY = EPM.MODID + "/fastrun";
	private static final ResourceLocation WOM_DEFAULT_WEAPON_RUN = ResourceLocation.fromNamespaceAndPath("wom", "biped/skill/biped_sprint");
	private static final ResourceLocation WOM_DEFAULT_BAREHAND_RUN = ResourceLocation.fromNamespaceAndPath("wom", "biped/skill/biped_sprint_barehand");
	private static final Gson GSON = new GsonBuilder().create();
	private static final RuleSet EMPTY_RULE_SET = RuleSet.create(List.of());
	private static final Map<ResourceLocation, AssetAccessor<? extends StaticAnimation>> CLIENT_ANIMATION_CACHE = new HashMap<>();
	private static final Set<ResourceLocation> MISSING_ANIMATION_LOGGED = new HashSet<>();
	private static volatile RuleSet serverRules = EMPTY_RULE_SET;
	private static volatile RuleSet clientRules = EMPTY_RULE_SET;
	private static volatile Map<ResourceLocation, ResourceLocation> serverWeaponTypes = Map.of();
	private static volatile Map<ResourceLocation, ResourceLocation> clientWeaponTypes = Map.of();
	private static volatile Set<ResourceLocation> clientConfiguredRunAnimationIds = Set.of();

	private NaturalSprinterFastRunAnimationOverrides() {
	}

	public static SimpleJsonResourceReloadListener reloadListener() {
		return new ReloadListener();
	}

	public static List<RuleData> serverRules() {
		return serverRules.rules();
	}

	public static Map<ResourceLocation, ResourceLocation> serverWeaponTypes() {
		return serverWeaponTypes;
	}

	public static void applyClientRules(List<RuleData> rules, Map<ResourceLocation, ResourceLocation> weaponTypes) {
		clientRules = RuleSet.create(rules);
		clientWeaponTypes = Map.copyOf(weaponTypes);
		clientConfiguredRunAnimationIds = configuredRunAnimationIds(rules);
		CLIENT_ANIMATION_CACHE.clear();
		MISSING_ANIMATION_LOGGED.clear();
		EPM.LOGGER.info(
				"Loaded {} Natural Sprinter FastRun animation override(s) and {} weapon type mapping(s) on client",
				Integer.valueOf(clientRules.rules().size()),
				Integer.valueOf(clientWeaponTypes.size()));
	}

	public static RuleData select(ResourceLocation itemId) {
		return clientRules.select(itemId, weaponTypeForItem(itemId));
	}

	static RuleData select(ResourceLocation itemId, ResourceLocation weaponType) {
		return clientRules.select(itemId, weaponType);
	}

	public static ResourceLocation weaponTypeForItem(ResourceLocation itemId) {
		return itemId == null ? null : clientWeaponTypes.get(itemId);
	}

	public static ResourceLocation itemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		try {
			return ForgeRegistries.ITEMS.getKey(stack.getItem());
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	static boolean isWomDefaultWeaponRun(ResourceLocation animationId) {
		return WOM_DEFAULT_WEAPON_RUN.equals(animationId);
	}

	static boolean isWomDefaultBarehandRun(ResourceLocation animationId) {
		return WOM_DEFAULT_BAREHAND_RUN.equals(animationId);
	}

	static AssetAccessor<? extends StaticAnimation> resolveAnimation(ResourceLocation animationId) {
		if (animationId == null) {
			return null;
		}

		AssetAccessor<? extends StaticAnimation> cached = CLIENT_ANIMATION_CACHE.get(animationId);
		if (cached != null) {
			return cached;
		}

		try {
			AssetAccessor<? extends StaticAnimation> animation = AnimationManager.byKey(animationId);
			if (animation == null || animation.get() == null) {
				logMissingAnimation(animationId);
				return null;
			}

			CLIENT_ANIMATION_CACHE.put(animationId, animation);
			return animation;
		} catch (RuntimeException | LinkageError ignored) {
			logMissingAnimation(animationId);
			return null;
		}
	}

	public static boolean isConfiguredRunAnimation(AssetAccessor<?> animation) {
		if (animation == null) {
			return false;
		}

		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		return animationId != null && clientConfiguredRunAnimationIds.contains(animationId);
	}

	private static Set<ResourceLocation> configuredRunAnimationIds(List<RuleData> rules) {
		Set<ResourceLocation> runAnimations = new HashSet<>();
		for (RuleData rule : rules) {
			for (AnimationSet animationSet : rule.animationSets()) {
				if (isWomDefaultWeaponRun(animationSet.runAnimation()) || isWomDefaultBarehandRun(animationSet.runAnimation())) {
					continue;
				}
				runAnimations.add(animationSet.runAnimation());
			}
		}
		return Set.copyOf(runAnimations);
	}

	private static void logMissingAnimation(ResourceLocation animationId) {
		if (MISSING_ANIMATION_LOGGED.add(animationId)) {
			EPM.LOGGER.warn("Natural Sprinter FastRun animation '{}' was not found; falling back to default animation", animationId);
		}
	}

	private static ResourceLocation optionalResourceLocation(JsonObject object, String key, ResourceLocation sourceId) {
		if (!object.has(key)) {
			return null;
		}

		String value = GsonHelper.getAsString(object, key).trim();
		ResourceLocation resourceLocation = ResourceLocation.tryParse(value);
		if (resourceLocation == null) {
			throw new JsonParseException("Invalid resource location '" + value + "' in " + sourceId + " field '" + key + "'");
		}
		return resourceLocation;
	}

	private static ResourceLocation requiredNamespacedResourceLocation(JsonObject object, String key, ResourceLocation sourceId) {
		String value = GsonHelper.getAsString(object, key).trim();
		if (!value.contains(":")) {
			throw new JsonParseException("Field '" + key + "' in " + sourceId + " must include a namespace, for example 'epicfight:sword'");
		}
		ResourceLocation resourceLocation = ResourceLocation.tryParse(value);
		if (resourceLocation == null) {
			throw new JsonParseException("Invalid resource location '" + value + "' in " + sourceId + " field '" + key + "'");
		}
		return resourceLocation;
	}

	private static RuleData parseRule(ResourceLocation id, JsonElement element) {
		JsonObject object = GsonHelper.convertToJsonObject(element, "Natural Sprinter FastRun animation override");
		if (object.has("enabled") && !GsonHelper.getAsBoolean(object, "enabled")) {
			return null;
		}

		JsonObject match = GsonHelper.getAsJsonObject(object, "match");
		boolean hasItem = match.has("item");
		boolean hasType = match.has("type");
		if (hasItem == hasType) {
			throw new JsonParseException("Exactly one of match.item or match.type is required in " + id);
		}

		ResourceLocation item = hasItem ? optionalResourceLocation(match, "item", id) : null;
		ResourceLocation type = hasType ? requiredNamespacedResourceLocation(match, "type", id) : null;

		List<AnimationSet> animationSets = parseAnimationSets(id, object);
		StepEffects stepEffects = parseStepEffects(object);
		RunPoseSettings runPose = parseRunPose(object);
		StepPoseSettings stepPose = parseStepPose(object);

		FallbackFamily fallback = null;
		if (object.has("fallback")) {
			String fallbackName = GsonHelper.getAsString(object, "fallback").trim().toUpperCase(java.util.Locale.ROOT);
			try {
				fallback = FallbackFamily.valueOf(fallbackName);
			} catch (IllegalArgumentException exception) {
				throw new JsonParseException("Invalid fallback '" + fallbackName + "' in " + id + "; expected barehand or weapon", exception);
			}
		}

		return new RuleData(
				id,
				item,
				type,
				animationSets,
				stepEffects.startupStep(),
				stepEffects.manualStep(),
				runPose,
				stepPose,
				fallback,
				GsonHelper.getAsInt(object, "main_priority", 0));
	}

	private static List<AnimationSet> parseAnimationSets(ResourceLocation id, JsonObject object) {
		if (!object.has("animations")) {
			throw new JsonParseException("animations array is required in " + id);
		}

		JsonArray animations = GsonHelper.getAsJsonArray(object, "animations");
		if (animations.isEmpty()) {
			throw new JsonParseException("animations array cannot be empty in " + id);
		}

		List<AnimationSet> animationSets = new ArrayList<>();
		for (int index = 0; index < animations.size(); index++) {
			JsonObject animationObject = GsonHelper.convertToJsonObject(animations.get(index), "animation entry " + index + " in " + id);
			ResourceLocation runAnimation = optionalResourceLocation(animationObject, "run", id);
			if (runAnimation == null) {
				throw new JsonParseException("animations[" + index + "].run is required in " + id);
			}

			ResourceLocation leftStepAnimation = optionalResourceLocation(animationObject, "left_step", id);
			ResourceLocation rightStepAnimation = optionalResourceLocation(animationObject, "right_step", id);
			if ((leftStepAnimation == null) != (rightStepAnimation == null)) {
				throw new JsonParseException("animations[" + index + "] must define both left_step and right_step, or neither, in " + id);
			}

			Set<String> styles = parseAnimationStyles(id, animationObject, index);
			animationSets.add(new AnimationSet(
					runAnimation,
					leftStepAnimation,
					rightStepAnimation,
					styles,
					GsonHelper.getAsInt(animationObject, "priority", 0)));
		}

		animationSets.sort(Comparator
				.comparingInt(AnimationSet::priority).reversed()
				.thenComparing(animationSet -> animationSet.runAnimation().toString()));
		return List.copyOf(animationSets);
	}

	private static Set<String> parseAnimationStyles(ResourceLocation id, JsonObject animationObject, int index) {
		boolean hasStyle = animationObject.has("style");
		boolean hasStyles = animationObject.has("styles");
		if (hasStyle && hasStyles) {
			throw new JsonParseException("animations[" + index + "] cannot define both style and styles in " + id);
		}
		if (!hasStyle && !hasStyles) {
			return Set.of();
		}

		Set<String> styles = new HashSet<>();
		if (hasStyle) {
			addAnimationStyle(styles, GsonHelper.getAsString(animationObject, "style"), id, index);
		} else {
			JsonArray styleArray = GsonHelper.getAsJsonArray(animationObject, "styles");
			if (styleArray.isEmpty()) {
				throw new JsonParseException("animations[" + index + "].styles cannot be empty in " + id);
			}
			for (JsonElement styleElement : styleArray) {
				addAnimationStyle(styles, GsonHelper.convertToString(styleElement, "style entry in animations[" + index + "]"), id, index);
			}
		}
		return Set.copyOf(styles);
	}

	private static void addAnimationStyle(Set<String> styles, String value, ResourceLocation id, int index) {
		String style = value == null ? "" : value.trim();
		if (style.isEmpty()) {
			throw new JsonParseException("animations[" + index + "] style cannot be empty in " + id);
		}
		styles.add(style.toUpperCase(java.util.Locale.ROOT));
	}

	private static StepEffects parseStepEffects(JsonObject object) {
		if (!object.has("effects")) {
			return StepEffects.NONE;
		}

		JsonObject effects = GsonHelper.getAsJsonObject(object, "effects");
		return new StepEffects(
				GsonHelper.getAsBoolean(effects, "startup_step", false),
				GsonHelper.getAsBoolean(effects, "manual_step", false));
	}

	private static RunPoseSettings parseRunPose(JsonObject object) {
		if (!object.has("run_pose")) {
			return RunPoseSettings.DEFAULT;
		}

		JsonObject runPose = GsonHelper.getAsJsonObject(object, "run_pose");
		boolean enabled = !runPose.has("enabled") || GsonHelper.getAsBoolean(runPose, "enabled");
		float scale = runPose.has("scale")
				? finiteNonNegativeFloat(GsonHelper.getAsFloat(runPose, "scale"), "run_pose.scale")
				: RunPoseSettings.DEFAULT.scale();
		float blendTicks = runPose.has("blend_ticks")
				? finitePositiveFloat(GsonHelper.getAsFloat(runPose, "blend_ticks"), "run_pose.blend_ticks")
				: RunPoseSettings.DEFAULT.blendTicks();
		return new RunPoseSettings(enabled, scale, blendTicks);
	}

	private static StepPoseSettings parseStepPose(JsonObject object) {
		if (!object.has("step_pose")) {
			return StepPoseSettings.DEFAULT;
		}

		JsonObject stepPose = GsonHelper.getAsJsonObject(object, "step_pose");
		float scale = stepPose.has("scale")
				? finiteNonNegativeFloat(GsonHelper.getAsFloat(stepPose, "scale"), "step_pose.scale")
				: StepPoseSettings.DEFAULT.scale();
		float durationTicks = stepPose.has("duration_ticks")
				? finitePositiveFloat(GsonHelper.getAsFloat(stepPose, "duration_ticks"), "step_pose.duration_ticks")
				: StepPoseSettings.DEFAULT.durationTicks();
		float forwardImpulse = stepPose.has("forward_impulse")
				? finiteNonNegativeFloat(GsonHelper.getAsFloat(stepPose, "forward_impulse"), "step_pose.forward_impulse")
				: StepPoseSettings.DEFAULT.forwardImpulse();
		return new StepPoseSettings(scale, durationTicks, forwardImpulse);
	}

	private static float finiteNonNegativeFloat(float value, String name) {
		if (!Float.isFinite(value) || value < 0.0F) {
			throw new JsonParseException(name + " must be a finite non-negative number");
		}
		return value;
	}

	private static float finitePositiveFloat(float value, String name) {
		if (!Float.isFinite(value) || value <= 0.0F) {
			throw new JsonParseException(name + " must be a finite positive number");
		}
		return value;
	}

	private record StepEffects(boolean startupStep, boolean manualStep) {
		private static final StepEffects NONE = new StepEffects(false, false);
	}

	public static record RunPoseSettings(boolean enabled, float scale, float blendTicks) {
		public static final RunPoseSettings DEFAULT = new RunPoseSettings(true, 0.4F, 3.0F);
		public static final RunPoseSettings DISABLED = new RunPoseSettings(false, 0.0F, 3.0F);

		public void encode(FriendlyByteBuf buffer) {
			buffer.writeBoolean(enabled);
			buffer.writeFloat(scale);
			buffer.writeFloat(blendTicks);
		}

		static RunPoseSettings decode(FriendlyByteBuf buffer) {
			return new RunPoseSettings(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
		}
	}

	public static record StepPoseSettings(float scale, float durationTicks, float forwardImpulse) {
		public static final StepPoseSettings DEFAULT = new StepPoseSettings(1.0F, 10.0F, 0.8F);

		public void encode(FriendlyByteBuf buffer) {
			buffer.writeFloat(scale);
			buffer.writeFloat(durationTicks);
			buffer.writeFloat(forwardImpulse);
		}

		static StepPoseSettings decode(FriendlyByteBuf buffer) {
			return new StepPoseSettings(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}
	}

	public enum FallbackFamily {
		BAREHAND,
		WEAPON
	}

	public static final class AnimationSet {
		private final ResourceLocation runAnimation;
		private final ResourceLocation leftStepAnimation;
		private final ResourceLocation rightStepAnimation;
		private final Set<String> styles;
		private final int priority;

		private AnimationSet(
				ResourceLocation runAnimation,
				ResourceLocation leftStepAnimation,
				ResourceLocation rightStepAnimation,
				Set<String> styles,
				int priority) {
			this.runAnimation = runAnimation;
			this.leftStepAnimation = leftStepAnimation;
			this.rightStepAnimation = rightStepAnimation;
			this.styles = styles == null ? Set.of() : Set.copyOf(styles);
			this.priority = priority;
		}

		ResourceLocation runAnimation() {
			return runAnimation;
		}

		ResourceLocation leftStepAnimation() {
			return leftStepAnimation;
		}

		ResourceLocation rightStepAnimation() {
			return rightStepAnimation;
		}

		boolean matchesStyle(String currentStyle) {
			return styles.isEmpty() || currentStyle != null && styles.contains(currentStyle);
		}

		Set<String> styles() {
			return styles;
		}

		int priority() {
			return priority;
		}

		private void encode(FriendlyByteBuf buffer) {
			buffer.writeResourceLocation(runAnimation);
			writeNullableResourceLocation(buffer, leftStepAnimation);
			writeNullableResourceLocation(buffer, rightStepAnimation);
			buffer.writeVarInt(styles.size());
			for (String style : styles) {
				buffer.writeUtf(style);
			}
			buffer.writeVarInt(priority);
		}

		private static AnimationSet decode(FriendlyByteBuf buffer) {
			ResourceLocation runAnimation = buffer.readResourceLocation();
			ResourceLocation leftStepAnimation = readNullableResourceLocation(buffer);
			ResourceLocation rightStepAnimation = readNullableResourceLocation(buffer);
			int styleCount = buffer.readVarInt();
			Set<String> styles = new HashSet<>();
			for (int index = 0; index < styleCount; index++) {
				styles.add(buffer.readUtf().trim().toUpperCase(java.util.Locale.ROOT));
			}
			int priority = buffer.readVarInt();
			return new AnimationSet(runAnimation, leftStepAnimation, rightStepAnimation, styles, priority);
		}
	}

	public static final class RuleData {
		private final ResourceLocation id;
		private final ResourceLocation item;
		private final ResourceLocation type;
		private final List<AnimationSet> animationSets;
		private final boolean startupStepEffects;
		private final boolean manualStepEffects;
		private final RunPoseSettings runPose;
		private final StepPoseSettings stepPose;
		private final FallbackFamily fallback;
		private final int mainPriority;

		private RuleData(
				ResourceLocation id,
				ResourceLocation item,
				ResourceLocation type,
				List<AnimationSet> animationSets,
				boolean startupStepEffects,
				boolean manualStepEffects,
				RunPoseSettings runPose,
				StepPoseSettings stepPose,
				FallbackFamily fallback,
				int mainPriority) {
			this.id = id;
			this.item = item;
			this.type = type;
			this.animationSets = List.copyOf(animationSets);
			this.startupStepEffects = startupStepEffects;
			this.manualStepEffects = manualStepEffects;
			this.runPose = runPose == null ? RunPoseSettings.DEFAULT : runPose;
			this.stepPose = stepPose == null ? StepPoseSettings.DEFAULT : stepPose;
			this.fallback = fallback;
			this.mainPriority = mainPriority;
		}

		ResourceLocation id() {
			return id;
		}

		ResourceLocation item() {
			return item;
		}

		ResourceLocation type() {
			return type;
		}

		List<AnimationSet> animationSets() {
			return animationSets;
		}

		boolean startupStepEffects() {
			return startupStepEffects;
		}

		boolean manualStepEffects() {
			return manualStepEffects;
		}

		RunPoseSettings runPose() {
			return runPose;
		}

		StepPoseSettings stepPose() {
			return stepPose;
		}

		FallbackFamily fallback() {
			return fallback;
		}

		int mainPriority() {
			return mainPriority;
		}

		public void encode(FriendlyByteBuf buffer) {
			writeNullableResourceLocation(buffer, id);
			writeNullableResourceLocation(buffer, item);
			writeNullableResourceLocation(buffer, type);
			buffer.writeVarInt(animationSets.size());
			for (AnimationSet animationSet : animationSets) {
				animationSet.encode(buffer);
			}
			buffer.writeBoolean(startupStepEffects);
			buffer.writeBoolean(manualStepEffects);
			runPose.encode(buffer);
			stepPose.encode(buffer);
			writeNullableString(buffer, fallback == null ? null : fallback.name());
			buffer.writeVarInt(mainPriority);
		}

		public static RuleData decode(FriendlyByteBuf buffer) {
			ResourceLocation id = readNullableResourceLocation(buffer);
			ResourceLocation item = readNullableResourceLocation(buffer);
			ResourceLocation type = readNullableResourceLocation(buffer);
			int animationSetCount = buffer.readVarInt();
			List<AnimationSet> animationSets = new ArrayList<>(animationSetCount);
			for (int index = 0; index < animationSetCount; index++) {
				animationSets.add(AnimationSet.decode(buffer));
			}
			boolean startupStepEffects = buffer.readBoolean();
			boolean manualStepEffects = buffer.readBoolean();
			RunPoseSettings runPose = RunPoseSettings.decode(buffer);
			StepPoseSettings stepPose = StepPoseSettings.decode(buffer);
			String fallbackName = readNullableString(buffer);
			FallbackFamily fallback = fallbackName == null ? null : FallbackFamily.valueOf(fallbackName);
			int mainPriority = buffer.readVarInt();
			return new RuleData(id, item, type, animationSets,
					startupStepEffects, manualStepEffects, runPose, stepPose, fallback, mainPriority);
		}
	}

	private static void writeNullableResourceLocation(FriendlyByteBuf buffer, ResourceLocation value) {
		buffer.writeBoolean(value != null);
		if (value != null) {
			buffer.writeResourceLocation(value);
		}
	}

	private static ResourceLocation readNullableResourceLocation(FriendlyByteBuf buffer) {
		return buffer.readBoolean() ? buffer.readResourceLocation() : null;
	}

	private static void writeNullableString(FriendlyByteBuf buffer, String value) {
		buffer.writeBoolean(value != null);
		if (value != null) {
			buffer.writeUtf(value);
		}
	}

	private static String readNullableString(FriendlyByteBuf buffer) {
		return buffer.readBoolean() ? buffer.readUtf() : null;
	}

	private record RuleSet(List<RuleData> rules, Map<ResourceLocation, RuleData> itemRules, Map<ResourceLocation, RuleData> typeRules) {
		private static RuleSet create(List<RuleData> inputRules) {
			List<RuleData> sortedRules = new ArrayList<>(inputRules);
			sortedRules.sort(Comparator
					.comparingInt(RuleData::mainPriority).reversed()
					.thenComparing(rule -> rule.id() == null ? "" : rule.id().toString()));

			Map<ResourceLocation, RuleData> itemRules = new HashMap<>();
			Map<ResourceLocation, RuleData> typeRules = new HashMap<>();
			for (RuleData rule : sortedRules) {
				if (rule.item() != null) {
					itemRules.putIfAbsent(rule.item(), rule);
				} else if (rule.type() != null) {
					typeRules.putIfAbsent(rule.type(), rule);
				}
			}

			return new RuleSet(Collections.unmodifiableList(sortedRules), Map.copyOf(itemRules), Map.copyOf(typeRules));
		}

		private RuleData select(ResourceLocation itemId, ResourceLocation weaponType) {
			if (itemId != null) {
				RuleData itemRule = itemRules.get(itemId);
				if (itemRule != null) {
					return itemRule;
				}
			}
			return weaponType == null ? null : typeRules.get(weaponType);
		}
	}

	private static final class ReloadListener extends SimpleJsonResourceReloadListener {
		private ReloadListener() {
			super(GSON, DIRECTORY);
		}

		@Override
		protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
			List<RuleData> rules = new ArrayList<>();
			Map<ResourceLocation, ResourceLocation> weaponTypes = loadWeaponTypes(resourceManager);
			objects.entrySet().stream()
					.sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
					.forEach(entry -> {
						try {
							RuleData rule = parseRule(entry.getKey(), entry.getValue());
							if (rule != null) {
								rules.add(rule);
							}
						} catch (JsonParseException | IllegalArgumentException exception) {
							EPM.LOGGER.warn("Skipping Natural Sprinter FastRun animation override '{}': {}", entry.getKey(), exception.getMessage());
						}
					});

			serverRules = RuleSet.create(rules);
			serverWeaponTypes = Map.copyOf(weaponTypes);
			EPM.LOGGER.info(
					"Loaded {} Natural Sprinter FastRun animation override(s) and {} weapon type mapping(s)",
					Integer.valueOf(serverRules.rules().size()),
					Integer.valueOf(serverWeaponTypes.size()));
		}

		private static Map<ResourceLocation, ResourceLocation> loadWeaponTypes(ResourceManager resourceManager) {
			Map<ResourceLocation, ResourceLocation> weaponTypes = new HashMap<>();
			Map<ResourceLocation, Resource> resources = resourceManager.listResources(
					"capabilities/weapons",
					resourceLocation -> resourceLocation.getPath().endsWith(".json"));
			for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
				loadWeaponType(weaponTypes, entry.getKey(), entry.getValue());
			}
			return weaponTypes;
		}

		private static void loadWeaponType(Map<ResourceLocation, ResourceLocation> weaponTypes, ResourceLocation resourceId, Resource resource) {
			String path = resourceId.getPath();
			String prefix = "capabilities/weapons/";
			String suffix = ".json";
			if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
				return;
			}

			String itemPath = path.substring(prefix.length(), path.length() - suffix.length());
			if (itemPath.isBlank() || itemPath.startsWith("types/") || itemPath.startsWith("item_keyword/")) {
				return;
			}

			try (Reader reader = resource.openAsReader()) {
				JsonObject object = GSON.fromJson(reader, JsonObject.class);
				if (object == null || !object.has("type")) {
					return;
				}

				String value = GsonHelper.getAsString(object, "type").trim();
				ResourceLocation weaponType = ResourceLocation.tryParse(value);
				if (weaponType == null) {
					EPM.LOGGER.warn("Ignoring invalid Epic Fight weapon type '{}' in '{}'", value, resourceId);
					return;
				}

				weaponTypes.put(ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), itemPath), weaponType);
			} catch (Exception exception) {
				EPM.LOGGER.warn("Could not read Epic Fight weapon capability '{}': {}", resourceId, exception.getMessage());
			}
		}
	}
}
