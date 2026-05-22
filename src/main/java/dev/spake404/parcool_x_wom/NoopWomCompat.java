package dev.spake404.parcool_x_wom;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

final class NoopWomCompat implements WomCompat {
	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprint() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallBackflip() {
		return null;
	}

	@Override
	public boolean isMoonlessCollider(Object collider) {
		return false;
	}

	@Override
	public boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public void suppressNaturalSprinter(PlayerPatch<?> playerPatch) {
	}

	@Override
	public boolean hasSpiderTechniques(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean shouldBlockSpiderTechniquesAttack(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public String describeSpiderTechniquesState(PlayerPatch<?> playerPatch) {
		return "wom=not_loaded";
	}
}
