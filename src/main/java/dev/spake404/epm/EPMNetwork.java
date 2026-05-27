package dev.spake404.epm;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EPMNetwork {
	private static final String PROTOCOL_VERSION = "1";
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
	}

	public static void sendPhantomAscentAirAttackWindow() {
		CHANNEL.sendToServer(PhantomAscentAirAttackPacket.INSTANCE);
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
}
