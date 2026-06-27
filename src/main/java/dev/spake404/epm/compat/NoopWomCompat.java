package dev.spake404.epm.compat;

import dev.spake404.epm.EPM;
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
	public AssetAccessor<? extends StaticAnimation> bipedSwimCrawl() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallBackflip() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallRunning() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallRunLeftSide() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallRunRightSide() {
		return null;
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallGlide() {
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
	public boolean consumeNaturalSprinterStep(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean hasSpiderTechniques(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean hasAquaManeuvre(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean setSpiderWallRunState(PlayerPatch<?> playerPatch, int wallRunning, boolean wallGlide, int timerRefresh, boolean jumpKeyUp) {
		return false;
	}

	@Override
	public boolean setSpiderWallGlideState(PlayerPatch<?> playerPatch, boolean started, boolean slowGlide, float yRot, boolean jumpKeyUp) {
		return false;
	}

	@Override
	public boolean isSpiderWallGlideActive(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean isSpiderWallMovementActive(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public boolean isSpiderWallBackflipActive(PlayerPatch<?> playerPatch) {
		return false;
	}

	@Override
	public void triggerSpiderWallBackflipState(PlayerPatch<?> playerPatch, float xRot, float yRot) {
	}

	@Override
	public void clearSpiderWallRunState(PlayerPatch<?> playerPatch) {
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
