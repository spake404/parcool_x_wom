package dev.spake404.epm.mixin;

import dev.spake404.epm.epicfightx.EpicFightXCombatMasteryCompat;
import dev.spake404.epm.epicfightx.EpicFightXCombatMasteryHandoff;
import dev.spake404.epm.EPM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asanginxst.epicfightx.skills.passive.CombatMastery_II;
import net.minecraft.nbt.CompoundTag;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

@Mixin(targets = "com.asanginxst.epicfightx.skills.passive.CombatMastery_II", remap = false)
public abstract class CombatMasteryIIMixin {
	private static final float COMBAT_MASTERY_II_STAMINA_COST_PER_SECOND = 0.4F;
	private static final float COMBAT_MASTERY_II_STAMINA_COST_PER_TICK =
			COMBAT_MASTERY_II_STAMINA_COST_PER_SECOND / 20.0F;

	@Shadow(remap = false)
	private int sprint_window;

	@Shadow(remap = false)
	private float stamina_cost;

	@Inject(method = "<init>", at = @At("TAIL"), require = 0)
	private void epm$setHalfNaturalSprinterStaminaCost(CombatMastery_II.Builder builder, CallbackInfo callback) {
		epm$setHalfNaturalSprinterStaminaCost();
	}

	@Inject(method = "setParams", at = @At("TAIL"), require = 0)
	private void epm$setHalfNaturalSprinterStaminaCost(CompoundTag parameters, CallbackInfo callback) {
		epm$setHalfNaturalSprinterStaminaCost();
	}

	private void epm$setHalfNaturalSprinterStaminaCost() {
		// Combat Mastery II drains EpicFight stamina per server tick.
		this.stamina_cost = COMBAT_MASTERY_II_STAMINA_COST_PER_TICK;
	}

	@Inject(method = "lambda$onInitiate$18", at = @At("HEAD"), require = 0)
	private void epm$observeCombatMasteryInput(
			SkillDataManager dataManager,
			SkillContainer container,
			MovementInputEvent event,
			CallbackInfo callback) {
		EpicFightXCombatMasteryCompat.observeCombatMasteryInput(dataManager, event, this.sprint_window);
	}

	@Inject(method = "stopSprinting", at = @At("TAIL"), require = 0)
	private void epm$cleanupCombatMasteryHandoff(SkillContainer container, CallbackInfo callback) {
		EpicFightXCombatMasteryHandoff.afterCombatMasteryStopSprinting(container);
	}
}
