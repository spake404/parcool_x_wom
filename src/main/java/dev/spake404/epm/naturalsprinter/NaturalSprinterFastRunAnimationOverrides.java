package dev.spake404.epm.naturalsprinter;

import dev.spake404.epm.EPM;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
	private static final String DIRECTORY = EPM.MODID + "/natural_sprinter_fastrun";
	private static final Gson GSON = new GsonBuilder().create();
	private static final RuleSet EMPTY_RULE_SET = RuleSet.create(List.of());
	private static final Map<ResourceLocation, AssetAccessor<? extends StaticAnimation>> CLIENT_ANIMATION_CACHE = new HashMap<>();
	private static final Set<ResourceLocation> MISSING_ANIMATION_LOGGED = new HashSet<>();
	private static volatile RuleSet serverRules = EMPTY_RULE_SET;
	private static volatile RuleSet clientRules = EMPTY_RULE_SET;

	private NaturalSprinterFastRunAnimationOverrides() {
	}

	public static SimpleJsonResourceReloadListener reloadListener() {
		return new ReloadListener();
	}

	public static List<RuleData> serverRules() {
		return serverRules.rules();
	}

	public static void applyClientRules(List<RuleData> rules) {
		clientRules = RuleSet.create(rules);
		CLIENT_ANIMATION_CACHE.clear();
		MISSING_ANIMATION_LOGGED.clear();
		EPM.LOGGER.info("Loaded {} Natural Sprinter FastRun animation override(s) on client", Integer.valueOf(clientRules.rules().size()));
	}
public static RuleData select(ResourceLocation itemId, String weaponType) {
		return clientRules.select(itemId, normalizeType(weaponType));
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

		for (RuleData rule : clientRules.rules()) {
			AssetAccessor<? extends StaticAnimation> runAnimation = resolveAnimation(rule.runAnimation());
			if (runAnimation != null && animation.equals(runAnimation)) {
				return true;
			}
		}
		return false;
	}

	private static void logMissingAnimation(ResourceLocation animationId) {
		if (MISSING_ANIMATION_LOGGED.add(animationId)) {
			EPM.LOGGER.warn("Natural Sprinter FastRun animation '{}' was not found; falling back to default animation", animationId);
		}
	}

	private static String normalizeType(String type) {
		return type == null ? null : type.trim().toUpperCase(java.util.Locale.ROOT);
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
		String type = hasType ? normalizeType(GsonHelper.getAsString(match, "type")) : null;
		if (hasType && (type == null || type.isBlank())) {
			throw new JsonParseException("match.type cannot be blank in " + id);
		}

		ResourceLocation runAnimation = optionalResourceLocation(object, "run", id);
		ResourceLocation leftStepAnimation = optionalResourceLocation(object, "left_step", id);
		ResourceLocation rightStepAnimation = optionalResourceLocation(object, "right_step", id);
		if (runAnimation == null && leftStepAnimation == null && rightStepAnimation == null) {
			throw new JsonParseException("At least one of run, left_step, or right_step is required in " + id);
		}
		StepEffects stepEffects = parseStepEffects(object);
		RunPoseSettings runPose = parseRunPose(object);

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
				runAnimation,
				leftStepAnimation,
				rightStepAnimation,
				stepEffects.startupStep(),
				stepEffects.manualStep(),
				runPose,
				fallback,
				GsonHelper.getAsInt(object, "priority", 0));
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

	public enum FallbackFamily {
		BAREHAND,
		WEAPON
	}
public static final class RuleData {
		private final ResourceLocation id;
		private final ResourceLocation item;
		private final String type;
		private final ResourceLocation runAnimation;
		private final ResourceLocation leftStepAnimation;
		private final ResourceLocation rightStepAnimation;
		private final boolean startupStepEffects;
		private final boolean manualStepEffects;
		private final RunPoseSettings runPose;
		private final FallbackFamily fallback;
		private final int priority;

		private RuleData(
				ResourceLocation id,
				ResourceLocation item,
				String type,
				ResourceLocation runAnimation,
				ResourceLocation leftStepAnimation,
				ResourceLocation rightStepAnimation,
				boolean startupStepEffects,
				boolean manualStepEffects,
				RunPoseSettings runPose,
				FallbackFamily fallback,
				int priority) {
			this.id = id;
			this.item = item;
			this.type = normalizeType(type);
			this.runAnimation = runAnimation;
			this.leftStepAnimation = leftStepAnimation;
			this.rightStepAnimation = rightStepAnimation;
			this.startupStepEffects = startupStepEffects;
			this.manualStepEffects = manualStepEffects;
			this.runPose = runPose == null ? RunPoseSettings.DEFAULT : runPose;
			this.fallback = fallback;
			this.priority = priority;
		}

		ResourceLocation id() {
			return id;
		}

		ResourceLocation item() {
			return item;
		}

		String type() {
			return type;
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

		boolean startupStepEffects() {
			return startupStepEffects;
		}

		boolean manualStepEffects() {
			return manualStepEffects;
		}

		RunPoseSettings runPose() {
			return runPose;
		}

		FallbackFamily fallback() {
			return fallback;
		}

		int priority() {
			return priority;
		}
		public void encode(FriendlyByteBuf buffer) {
			writeNullableResourceLocation(buffer, id);
			writeNullableResourceLocation(buffer, item);
			writeNullableString(buffer, type);
			writeNullableResourceLocation(buffer, runAnimation);
			writeNullableResourceLocation(buffer, leftStepAnimation);
			writeNullableResourceLocation(buffer, rightStepAnimation);
			buffer.writeBoolean(startupStepEffects);
			buffer.writeBoolean(manualStepEffects);
			runPose.encode(buffer);
			writeNullableString(buffer, fallback == null ? null : fallback.name());
			buffer.writeVarInt(priority);
		}
		public static RuleData decode(FriendlyByteBuf buffer) {
			ResourceLocation id = readNullableResourceLocation(buffer);
			ResourceLocation item = readNullableResourceLocation(buffer);
			String type = readNullableString(buffer);
			ResourceLocation runAnimation = readNullableResourceLocation(buffer);
			ResourceLocation leftStepAnimation = readNullableResourceLocation(buffer);
			ResourceLocation rightStepAnimation = readNullableResourceLocation(buffer);
			boolean startupStepEffects = buffer.readBoolean();
			boolean manualStepEffects = buffer.readBoolean();
			RunPoseSettings runPose = RunPoseSettings.decode(buffer);
			String fallbackName = readNullableString(buffer);
			FallbackFamily fallback = fallbackName == null ? null : FallbackFamily.valueOf(fallbackName);
			int priority = buffer.readVarInt();
			return new RuleData(id, item, type, runAnimation, leftStepAnimation, rightStepAnimation,
					startupStepEffects, manualStepEffects, runPose, fallback, priority);
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
	}

	private record RuleSet(List<RuleData> rules, Map<ResourceLocation, RuleData> itemRules, Map<String, RuleData> typeRules) {
		private static RuleSet create(List<RuleData> inputRules) {
			List<RuleData> sortedRules = new ArrayList<>(inputRules);
			sortedRules.sort(Comparator
					.comparingInt(RuleData::priority).reversed()
					.thenComparing(rule -> rule.id() == null ? "" : rule.id().toString()));

			Map<ResourceLocation, RuleData> itemRules = new HashMap<>();
			Map<String, RuleData> typeRules = new HashMap<>();
			for (RuleData rule : sortedRules) {
				if (rule.item() != null) {
					itemRules.putIfAbsent(rule.item(), rule);
				} else if (rule.type() != null) {
					typeRules.putIfAbsent(rule.type(), rule);
				}
			}

			return new RuleSet(Collections.unmodifiableList(sortedRules), Map.copyOf(itemRules), Map.copyOf(typeRules));
		}

		private RuleData select(ResourceLocation itemId, String weaponType) {
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
			EPM.LOGGER.info("Loaded {} Natural Sprinter FastRun animation override(s)", Integer.valueOf(serverRules.rules().size()));
		}
	}
}
