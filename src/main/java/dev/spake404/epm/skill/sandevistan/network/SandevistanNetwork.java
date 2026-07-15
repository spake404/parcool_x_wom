package dev.spake404.epm.skill.sandevistan.network;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.SandevistanManager;
import dev.spake404.epm.skill.sandevistan.SandevistanStopReason;
import dev.spake404.epm.skill.sandevistan.client.SandevistanClientState;
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
	private static final String PROTOCOL_VERSION = "2";
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
		CHANNEL.messageBuilder(AttackStopPacket.class, packetId++)
				.encoder(AttackStopPacket::encode)
				.decoder(AttackStopPacket::decode)
				.consumerMainThread(AttackStopPacket::handle)
				.add();
		CHANNEL.messageBuilder(SyncStatePacket.class, packetId++)
				.encoder(SyncStatePacket::encode)
				.decoder(SyncStatePacket::decode)
				.consumerMainThread(SyncStatePacket::handle)
				.add();
	}

	public static void sendAttackStopRequest() {
		CHANNEL.sendToServer(AttackStopPacket.INSTANCE);
	}

	public static void broadcastState(
			ServerPlayer source,
			boolean active,
			int remainingTicks,
			SandevistanStopReason reason) {
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
						EPMConfig.sandevistanTimeScale(),
						EPMConfig.sandevistanRadius(),
						EPMConfig.sandevistanAfterimageIntervalTicks()));
	}

	public static void sendSnapshot(ServerPlayer receiver, ServerPlayer source) {
		if (receiver == null || source == null) {
			return;
		}

		CHANNEL.send(
				PacketDistributor.PLAYER.with(() -> receiver),
				new SyncStatePacket(
						source.getUUID(),
						SandevistanManager.isActive(source),
						SandevistanManager.remainingTicks(source),
						null,
						EPMConfig.sandevistanTimeScale(),
						EPMConfig.sandevistanRadius(),
						EPMConfig.sandevistanAfterimageIntervalTicks()));
	}

	private static final class AttackStopPacket {
		private static final AttackStopPacket INSTANCE = new AttackStopPacket();

		private static void encode(AttackStopPacket packet, FriendlyByteBuf buffer) {
		}

		private static AttackStopPacket decode(FriendlyByteBuf buffer) {
			return INSTANCE;
		}

		private static void handle(AttackStopPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			ServerPlayer sender = context.getSender();
			if (sender != null) {
				SandevistanManager.requestStop(sender, SandevistanStopReason.ATTACK);
			}
			context.setPacketHandled(true);
		}
	}

	private static final class SyncStatePacket {
		private final UUID playerId;
		private final boolean active;
		private final int remainingTicks;
		private final SandevistanStopReason reason;
		private final double timeScale;
		private final double radius;
		private final int afterimageIntervalTicks;

		private SyncStatePacket(
				UUID playerId,
				boolean active,
				int remainingTicks,
				SandevistanStopReason reason,
				double timeScale,
				double radius,
				int afterimageIntervalTicks) {
			this.playerId = playerId;
			this.active = active;
			this.remainingTicks = remainingTicks;
			this.reason = reason;
			this.timeScale = timeScale;
			this.radius = radius;
			this.afterimageIntervalTicks = afterimageIntervalTicks;
		}

		private static void encode(SyncStatePacket packet, FriendlyByteBuf buffer) {
			buffer.writeUUID(packet.playerId);
			buffer.writeBoolean(packet.active);
			buffer.writeVarInt(packet.remainingTicks);
			buffer.writeVarInt(packet.reason == null ? -1 : packet.reason.ordinal());
			buffer.writeDouble(packet.timeScale);
			buffer.writeDouble(packet.radius);
			buffer.writeVarInt(packet.afterimageIntervalTicks);
		}

		private static SyncStatePacket decode(FriendlyByteBuf buffer) {
			UUID playerId = buffer.readUUID();
			boolean active = buffer.readBoolean();
			int remainingTicks = buffer.readVarInt();
			int reasonOrdinal = buffer.readVarInt();
			SandevistanStopReason reason = reasonOrdinal < 0
					? null
					: SandevistanStopReason.values()[Math.min(reasonOrdinal, SandevistanStopReason.values().length - 1)];
			double timeScale = buffer.readDouble();
			double radius = buffer.readDouble();
			int afterimageIntervalTicks = buffer.readVarInt();
			return new SyncStatePacket(
					playerId,
					active,
					remainingTicks,
					reason,
					timeScale,
					radius,
					afterimageIntervalTicks);
		}

		private static void handle(SyncStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
					SandevistanClientState.sync(
							packet.playerId,
							packet.active,
							packet.remainingTicks,
							packet.reason,
							packet.timeScale,
							packet.radius,
							packet.afterimageIntervalTicks)));
			context.setPacketHandled(true);
		}
	}
}
