package dev.spake404.epm.skill.sandevistan.network;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.SandevistanManager;
import dev.spake404.epm.skill.sandevistan.SandevistanStopReason;
import dev.spake404.epm.skill.sandevistan.client.SandevistanClientState;
import dev.spake404.epm.skill.sandevistan.client.SandevistanHudCombatState;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfiles;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SandevistanNetwork {
	private static final String PROTOCOL_VERSION = "6";
	private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(ResourceLocation.fromNamespaceAndPath(EPM.MODID, "sandevistan"))
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.clientAcceptedVersions(PROTOCOL_VERSION::equals)
			.serverAcceptedVersions(PROTOCOL_VERSION::equals)
			.simpleChannel();
	private static int packetId;

	private SandevistanNetwork() {
	}

	public static void register() {
		CHANNEL.messageBuilder(ManualStopPacket.class, packetId++)
				.encoder(ManualStopPacket::encode)
				.decoder(ManualStopPacket::decode)
				.consumerMainThread(ManualStopPacket::handle)
				.add();
		CHANNEL.messageBuilder(SyncStatePacket.class, packetId++)
				.encoder(SyncStatePacket::encode)
				.decoder(SyncStatePacket::decode)
				.consumerMainThread(SyncStatePacket::handle)
				.add();
		CHANNEL.messageBuilder(CombatActivityPacket.class, packetId++)
				.encoder(CombatActivityPacket::encode)
				.decoder(CombatActivityPacket::decode)
				.consumerMainThread(CombatActivityPacket::handle)
				.add();
	}

	public static void sendManualStopRequest() {
		CHANNEL.sendToServer(ManualStopPacket.INSTANCE);
	}

	public static void sendCombatActivity(ServerPlayer player) {
		if (player != null) {
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), CombatActivityPacket.INSTANCE);
		}
	}

	public static void broadcastState(
			ServerPlayer source,
			boolean active,
			int remainingTicks,
			SandevistanStopReason reason,
			ResourceLocation profileId) {
		if (source == null) {
			return;
		}

		CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> source),
				new SyncStatePacket(
						source.getUUID(),
						active,
						remainingTicks,
						reason,
						profileId));
	}

	public static void sendSnapshot(ServerPlayer receiver, ServerPlayer source) {
		if (receiver == null || source == null) {
			return;
		}

		SandevistanProfile profile = SandevistanManager.activeProfile(source);
		ResourceLocation profileId = profile == null ? SandevistanProfiles.DINARA_MK4.id() : profile.id();
		CHANNEL.send(
				PacketDistributor.PLAYER.with(() -> receiver),
				new SyncStatePacket(
						source.getUUID(),
						SandevistanManager.isActive(source),
						SandevistanManager.remainingTicks(source),
						null,
						profileId));
	}

	private static final class ManualStopPacket {
		private static final ManualStopPacket INSTANCE = new ManualStopPacket();

		private static void encode(ManualStopPacket packet, FriendlyByteBuf buffer) {
		}

		private static ManualStopPacket decode(FriendlyByteBuf buffer) {
			return INSTANCE;
		}

		private static void handle(ManualStopPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			ServerPlayer sender = context.getSender();
			if (sender != null) {
				SandevistanManager.requestStop(sender, SandevistanStopReason.MANUAL);
			}
			context.setPacketHandled(true);
		}
	}

	private static final class CombatActivityPacket {
		private static final CombatActivityPacket INSTANCE = new CombatActivityPacket();

		private static void encode(CombatActivityPacket packet, FriendlyByteBuf buffer) {
		}

		private static CombatActivityPacket decode(FriendlyByteBuf buffer) {
			return INSTANCE;
		}

		private static void handle(CombatActivityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
					Dist.CLIENT,
					() -> SandevistanHudCombatState::markCombatActivity));
			context.setPacketHandled(true);
		}
	}

	private static final class SyncStatePacket {
		private final UUID playerId;
		private final boolean active;
		private final int remainingTicks;
		private final SandevistanStopReason reason;
		private final ResourceLocation profileId;

		private SyncStatePacket(
				UUID playerId,
				boolean active,
				int remainingTicks,
				SandevistanStopReason reason,
				ResourceLocation profileId) {
			this.playerId = playerId;
			this.active = active;
			this.remainingTicks = remainingTicks;
			this.reason = reason;
			this.profileId = profileId;
		}

		private static void encode(SyncStatePacket packet, FriendlyByteBuf buffer) {
			buffer.writeUUID(packet.playerId);
			buffer.writeBoolean(packet.active);
			buffer.writeVarInt(packet.remainingTicks);
			buffer.writeVarInt(packet.reason == null ? -1 : packet.reason.ordinal());
			buffer.writeResourceLocation(packet.profileId);
		}

		private static SyncStatePacket decode(FriendlyByteBuf buffer) {
			UUID playerId = buffer.readUUID();
			boolean active = buffer.readBoolean();
			int remainingTicks = buffer.readVarInt();
			int reasonOrdinal = buffer.readVarInt();
			SandevistanStopReason reason = reasonOrdinal < 0
					? null
					: SandevistanStopReason.values()[Math.min(reasonOrdinal, SandevistanStopReason.values().length - 1)];
			ResourceLocation profileId = buffer.readResourceLocation();
			return new SyncStatePacket(
					playerId,
					active,
					remainingTicks,
					reason,
					profileId);
		}

		private static void handle(SyncStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
					SandevistanClientState.sync(
							packet.playerId,
							packet.active,
							packet.remainingTicks,
							packet.reason,
							packet.profileId)));
			context.setPacketHandled(true);
		}
	}
}
