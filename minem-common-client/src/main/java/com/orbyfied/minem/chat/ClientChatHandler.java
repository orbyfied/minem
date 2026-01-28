package com.orbyfied.minem.chat;

import com.orbyfied.minem.ClientComponent;
import com.orbyfied.minem.MinecraftClient;
import com.orbyfied.minem.model.ProtocolTextComponents;
import com.orbyfied.minem.event.Chain;
import com.orbyfied.minem.protocol.play.ClientboundChatMessagePacket;
import com.orbyfied.minem.protocol.play.ServerboundChatPacket;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles incoming chat messages and sending chat messages/commands.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientChatHandler extends ClientComponent {

    public static ClientChatHandler create() {
        return new ClientChatHandler();
    }

    /* Event Chains */
    final Chain<ChatMessageHandler> onChatReceived = new Chain<>(ChatMessageHandler.class);

    @Override
    protected boolean attach(MinecraftClient client) {
        client.onTypedReceived().by(ClientboundChatMessagePacket.class).addLast(packet -> {
            ClientboundChatMessagePacket messagePacket = packet.data(ClientboundChatMessagePacket.class);

            // fix legacy colors
            Component message = messagePacket.getMessage();
            message = ProtocolTextComponents.fixLegacyFormattingInTree(message);
            String rawMessage = PlainTextComponentSerializer.plainText().serialize(message);
            onChatReceived.invoker().chatReceived(this, message, rawMessage, Type.values()[messagePacket.getPosition()]);
            return 0;
        });

        return super.attach(client);
    }

    public Chain<ChatMessageHandler> onChatReceived() {
        return onChatReceived;
    }

    /**
     * Send the given chat message/command.
     */
    public void sendChatSync(String message) {
        client.getConnection().sendSync(client.createPacket(new ServerboundChatPacket(message)));
    }

    /**
     * The type/position of a chat message.
     */
    public enum Type {
        CHAT,
        SYSTEM,
        BAR
    }

    // Event Handler: called when a chat message is received
    public interface ChatMessageHandler {
        void chatReceived(ClientChatHandler handler,
                          Component message,
                          String rawMessage,
                          Type type);
    }

}
