package com.orbyfied.minem;

/**
 * Represents an attachable component of a {@link MinecraftClient}.
 */
public abstract class ClientComponent {

    /**
     * The client this component is attached to, if any.
     */
    protected MinecraftClient client;

    // Internal attach procedure
    final boolean attach0(MinecraftClient client) {
        boolean v = attach(client);
        if (v) {
            this.client = client;
        }

        return v;
    }

    /**
     * Called when this component will be attached to the client.
     *
     * If false is returned the attachment will not proceed.
     *
     * @param client The client.
     * @return Whether to allow attachment.
     */
    protected boolean attach(MinecraftClient client) {
        return true;
    }

    /**
     * Called when this component should void all state, for example because
     * the client disconnected and is preparing for a new connection.
     */
    protected void resetState() {

    }

}
