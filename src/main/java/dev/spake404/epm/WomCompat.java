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

	AssetAccessor<? extends StaticAnimation> wallBackflip();

	boolean isMoonlessCollider(Object collider);

	boolean hasNaturalSprinter(PlayerPatch<?> playerPatch);

	void suppressNaturalSprinter(PlayerPatch<?> playerPatch);

	boolean consumeNaturalSprinterStep(PlayerPatch<?> playerPatch);

	boolean hasSpiderTechniques(PlayerPatch<?> playerPatch);

	boolean shouldBlockSpiderTechniquesAttack(PlayerPatch<?> playerPatch);

	String describeSpiderTechniquesState(PlayerPatch<?> playerPatch);
}
