package dev.spake404.epm.network;

import dev.spake404.epm.EPM;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunAnimationOverrides;
import dev.spake404.epm.phantom.PhantomAscentAirAttackState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EPMNetwork {
	private static final String PROTOCOL_VERSION = "2";
	private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(ResourceLocation.fromNamespaceAndPath(EPM.MODID, "main"))
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.clientAcceptedVersions(PROTOCOL_VERSION::equals)
			.serverAcceptedVersions(PROTOCOL_VERSION::equals)
			.simpleChannel();
	private static int packetId;

	private EPMNetwork() {
	}

	public static void register() {
		CHANNEL.messageBuilder(PhantomAscentAirAttackPacket.class, packetId++)
				.encoder(PhantomAscentAirAttackPacket::encode)
				.decoder(PhantomAscentAirAttackPacket::decode)
				.consumerMainThread(PhantomAscentAirAttackPacket::handle)
				.add();
		CHANNEL.messageBuilder(SyncNaturalSprinterFastRunAnimationOverridesPacket.class, packetId++)
				.encoder(SyncNaturalSprinterFastRunAnimationOverridesPacket::encode)
				.decoder(SyncNaturalSprinterFastRunAnimationOverridesPacket::decode)
				.consumerMainThread(SyncNaturalSprinterFastRunAnimationOverridesPacket::handle)
				.add();
	}

	public static void sendPhantomAscentAirAttackWindow() {
		CHANNEL.sendToServer(PhantomAscentAirAttackPacket.INSTANCE);
	}

	public static void sendNaturalSprinterFastRunAnimationOverrides(ServerPlayer player) {
		if (player != null) {
			CHANNEL.send(
					PacketDistributor.PLAYER.with(() -> player),
					new SyncNaturalSprinterFastRunAnimationOverridesPacket(NaturalSprinterFastRunAnimationOverrides.serverRules()));
		}
	}

	private static final class PhantomAscentAirAttackPacket {
		private static final PhantomAscentAirAttackPacket INSTANCE = new PhantomAscentAirAttackPacket();

		private static void encode(PhantomAscentAirAttackPacket packet, FriendlyByteBuf buffer) {
		}

		private static PhantomAscentAirAttackPacket decode(FriendlyByteBuf buffer) {
			return INSTANCE;
		}

		private static void handle(PhantomAscentAirAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			ServerPlayer sender = context.getSender();
			if (sender != null) {
				PhantomAscentAirAttackState.mark(sender);
			}
			context.setPacketHandled(true);
		}
	}

	private static final class SyncNaturalSprinterFastRunAnimationOverridesPacket {
		private final List<NaturalSprinterFastRunAnimationOverrides.RuleData> rules;

		private SyncNaturalSprinterFastRunAnimationOverridesPacket(List<NaturalSprinterFastRunAnimationOverrides.RuleData> rules) {
			this.rules = List.copyOf(rules);
		}

		private static void encode(SyncNaturalSprinterFastRunAnimationOverridesPacket packet, FriendlyByteBuf buffer) {
			buffer.writeVarInt(packet.rules.size());
			for (NaturalSprinterFastRunAnimationOverrides.RuleData rule : packet.rules) {
				rule.encode(buffer);
			}
		}

		private static SyncNaturalSprinterFastRunAnimationOverridesPacket decode(FriendlyByteBuf buffer) {
			int count = buffer.readVarInt();
			List<NaturalSprinterFastRunAnimationOverrides.RuleData> rules = new ArrayList<>(count);
			for (int index = 0; index < count; index++) {
				rules.add(NaturalSprinterFastRunAnimationOverrides.RuleData.decode(buffer));
			}
			return new SyncNaturalSprinterFastRunAnimationOverridesPacket(rules);
		}

		private static void handle(SyncNaturalSprinterFastRunAnimationOverridesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> NaturalSprinterFastRunAnimationOverrides.applyClientRules(packet.rules));
			context.setPacketHandled(true);
		}
	}
}
