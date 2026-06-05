package dev.spake404.epm;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

interface WomCompat {
	AssetAccessor<? extends StaticAnimation> bipedSprint();

	AssetAccessor<? extends StaticAnimation> bipedSprintBarehand();

	AssetAccessor<? extends StaticAnimation> bipedSprintStop();

	AssetAccessor<? extends StaticAnimation> bipedSprintRightStep();

	AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep();

	AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand();

	AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand();

	AssetAccessor<? extends StaticAnimation> bipedSprintSlide();

	AssetAccessor<? extends StaticAnimation> bipedSprintJump();

	AssetAccessor<? extends StaticAnimation> bipedSwimCrawl();

	AssetAccessor<? extends StaticAnimation> wallBackflip();

	AssetAccessor<? extends StaticAnimation> wallRunning();

	AssetAccessor<? extends StaticAnimation> wallRunLeftSide();

	AssetAccessor<? extends StaticAnimation> wallRunRightSide();

	AssetAccessor<? extends StaticAnimation> wallGlide();

	boolean isMoonlessCollider(Object collider);

	boolean hasNaturalSprinter(PlayerPatch<?> playerPatch);

	void suppressNaturalSprinter(PlayerPatch<?> playerPatch);

	boolean consumeNaturalSprinterStep(PlayerPatch<?> playerPatch);

	boolean hasSpiderTechniques(PlayerPatch<?> playerPatch);

	boolean hasAquaManeuvre(PlayerPatch<?> playerPatch);

	boolean setSpiderWallRunState(PlayerPatch<?> playerPatch, int wallRunning, boolean wallGlide, int timerRefresh, boolean jumpKeyUp);

	boolean setSpiderWallGlideState(PlayerPatch<?> playerPatch, boolean started, boolean slowGlide, float yRot, boolean jumpKeyUp);

	boolean isSpiderWallGlideActive(PlayerPatch<?> playerPatch);

	boolean isSpiderWallMovementActive(PlayerPatch<?> playerPatch);

	void triggerSpiderWallBackflipState(PlayerPatch<?> playerPatch, float xRot, float yRot);

	void clearSpiderWallRunState(PlayerPatch<?> playerPatch);

	boolean shouldBlockSpiderTechniquesAttack(PlayerPatch<?> playerPatch);

	String describeSpiderTechniquesState(PlayerPatch<?> playerPatch);
}
