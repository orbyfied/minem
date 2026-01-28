package com.orbyfied.minem;

import com.orbyfied.minem.client.ClientDisconnectHandler;
import com.orbyfied.minem.client.ClientStateSwitchHandler;
import com.orbyfied.minem.client.ClientTickHandler;
import com.orbyfied.minem.client.DisconnectReason;
import com.orbyfied.minem.event.Chain;
import com.orbyfied.minem.event.ExceptionEventHandler;
import com.orbyfied.minem.event.ExceptionEventSource;
import com.orbyfied.minem.event.MultiChain;
import com.orbyfied.minem.exception.ClientConnectException;
import com.orbyfied.minem.network.NetworkManager;
import com.orbyfied.minem.network.ProtocolConnection;
import com.orbyfied.minem.protocol.*;
import com.orbyfied.minem.protocol.login.ClientboundLoginDisconnectPacket;
import com.orbyfied.minem.protocol.login.ClientboundSetCompressionPacket;
import com.orbyfied.minem.protocol.handshake.ServerboundHandshakePacket;
import com.orbyfied.minem.protocol.play.ClientboundPlayDisconnectPacket;
import com.orbyfied.minem.protocol.play.ClientboundKeepAlivePacket;
import com.orbyfied.minem.protocol.play.ServerboundKeepAlivePacket;
import com.orbyfied.minem.scheduler.ClientScheduler;
import lombok.Getter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a simple, modular (component based) Minecraft client.
 */
public class MinecraftClient extends ProtocolContext implements ExceptionEventSource {

    /**
     * The unscheduled executor.
     */
    @Getter
    ExecutorService executor = Executors.newFixedThreadPool(2, r -> new Thread(r, "MinecraftClient(" + hexHashCode() + ")-PoolThread"));

    /**
     * The scheduler for this client.
     */
    @Getter
    ClientScheduler scheduler = new ClientScheduler(this);

    /**
     * The network manager for this client.
     */
    @Getter
    NetworkManager networkManager;

    /**
     * The protocol to use.
     */
    @Getter
    Protocol protocol;

    /**
     * All registered components mapped by class, superclasses and interfaces.
     */
    final Map<Class<?>, ClientComponent> componentMap = new HashMap<>();

    /**
     * The list of all attached components.
     */
    final List<ClientComponent> componentList = new ArrayList<>();

    boolean isResetState = true;                               // Whether the current state of the client is clear
    volatile ClientState lastState = null;                     // The previous client state
    volatile @Getter ClientState state = ClientState.INACTIVE; // The current client state

    final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * The current network connection/channel
     */
    @Getter
    ProtocolConnection connection;

    /* Connection Events */
    final Chain<PacketHandler> onPacket = new Chain<>(PacketHandler.class).integerFlagHandling();
    final Chain<PacketHandler> onPacketSink = new Chain<>(PacketHandler.class).integerFlagHandling();
    final Chain<PacketHandler> onPacketReceived = new Chain<>(PacketHandler.class).integerFlagHandling();

    /* Multiplexed Connection Events */
    final MultiChain<PacketMapping, PacketHandler> onTypedSent = new MultiChain<PacketMapping, PacketHandler>(k -> new Chain<>(PacketHandler.class).integerFlagHandling()).keyMapper(o -> protocol.match(o));
    final MultiChain<PacketMapping, PacketHandler> onTypedReceived = new MultiChain<PacketMapping, PacketHandler>(k -> new Chain<>(PacketHandler.class).integerFlagHandling()).keyMapper(o -> protocol.match(o));

    /* Updates and Ticking */
    boolean enableTicking = true; // Whether the 50ms ticking should be enabled
    int targetUps = 60;           // The target updates per second, 0 to disable updates
    Thread tickThread;            // The thread simulating 50ms ticks
    Thread updateThread;          // The thread simulating a 60 FPS render thread
    volatile long tickDt = 0;     // The latest delta time of one tick
    volatile long updateDt = 0;   // The latest delta time of one update
    AtomicLong tickCount = new AtomicLong(0);
    AtomicLong updateCount = new AtomicLong(0);
    final Chain<ClientTickHandler> onTick = new Chain<>(ClientTickHandler.class);
    final Chain<ClientTickHandler> onUpdate = new Chain<>(ClientTickHandler.class);

    /* Client Events */
    final Chain<ExceptionEventHandler> onError = new Chain<>(ExceptionEventHandler.class);
    final Chain<ClientStateSwitchHandler> onStateSwitch = new Chain<>(ClientStateSwitchHandler.class);
    final Chain<ClientDisconnectHandler> onDisconnect = new Chain<>(ClientDisconnectHandler.class);

    public static MinecraftClient create() {
        return new MinecraftClient();
    }

    public MinecraftClient executor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public MinecraftClient protocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public MinecraftClient networkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
        return this;
    }

    // Register the given component for the given class and superclasses recursively.
    private void registerComponentClass(Class<?> kl, ClientComponent component) {
        componentMap.put(kl, component);
        if (kl.getSuperclass() != null && kl.getSuperclass() != Object.class) {
            registerComponentClass(kl.getSuperclass(), component);
        }

        for (Class<?> itf : kl.getInterfaces()) {
            registerComponentClass(itf, component);
        }
    }

    /**
     * Attach the given component to this client.
     *
     * @param component The component.
     * @return This.
     */
    public MinecraftClient with(ClientComponent component) {
        Objects.requireNonNull(component, "Component can not be null");
        if (component.attach0(this)) {
            componentList.add(component);
            registerComponentClass(component.getClass(), component);
        }

        return this;
    }

    /**
     * Find a component by the given type if present.
     *
     * @param cClass The runtime type.
     * @param <C> The type.
     * @return The component or null if absent.
     */
    @SuppressWarnings("unchecked")
    public <C extends ClientComponent> C find(Class<C> cClass) {
        return (C) componentMap.get(cClass);
    }

    @SuppressWarnings("unchecked")
    public <C extends ClientComponent> C require(Class<C> cClass) {
        C comp = (C) componentMap.get(cClass);
        if (comp == null) {
            throw new IllegalStateException("Expected an attached component by " + cClass);
        }

        return comp;
    }

    // Switch the current client state
    public void switchState(ClientState state) {
        this.lastState = this.state;
        this.state = state;
        onStateSwitch.invoker().onStateSwitch(this.lastState, state);
    }

    // the hexadecimal identity hashcode for this client instance
    private String hexHashCode() {
        return Integer.toHexString(this.hashCode());
    }

    /**
     * Connect the client to the given address.
     *
     * @param address The address.
     * @return The future.
     */
    public synchronized CompletableFuture<MinecraftClient> connect(InetSocketAddress address) {
        resetState();
        return CompletableFuture.supplyAsync(() -> {
            try {
                active.set(true);
                switchState(ClientState.CONNECTING);

                // create connection instance
                connection = networkManager.connect(this, address);

                // initiate handshake
                switchState(ClientState.HANDSHAKE);
                connection.sendSync(createPacket("ServerboundHandshake",
                        new ServerboundHandshakePacket(protocol.getProtocolNumber(), address.getHostString(),
                                (short) address.getPort(), ServerboundHandshakePacket.NextState.LOGIN)));
                switchState(ClientState.LOGIN);

                connection.start();

                // start tick and update schedule
                if (enableTicking && tickThread == null) {
                    tickThread = new Thread(this::runTickLoop, "MinecraftClient(" + hexHashCode() + ")-TickThread");
                    tickThread.setDaemon(true);
                }

                if (targetUps != 0 && updateThread == null) {
                    updateThread = new Thread(this::runUpdateLoop, "MinecraftClient(" + hexHashCode() + ")-UpdateThread");
                    updateThread.setDaemon(true);
                }

                if (enableTicking) tickThread.start();
                if (targetUps != 0) updateThread.start();

                return this;
            } catch (Exception ex) {
                ClientConnectException clientConnectException = new ClientConnectException("Failed to connect to " + address, ex);
                disconnect(DisconnectReason.ERROR, clientConnectException);
                throw clientConnectException;
            }
        }, executor);
    }

    public boolean isActive() {
        return active.get();
    }

    /**
     * Close/deactivate the client until the next connection.
     */
    public boolean close() {
        if (!isActive()) {
            return false;
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Throwable thr) {
            thr.printStackTrace();
        } finally {
            resetState();
            active.set(false);
        }

        return true;
    }

    /**
     * Disconnects the client with the given reason, if connected.
     *
     * @param reason The reason.
     * @param details The detailed reason.
     * @return Whether it disconnected the client.
     */
    public synchronized boolean disconnect(DisconnectReason reason,
                                           Object details) {
        if (!close()) {
            return false;
        }

        try {
            onDisconnect.invoker().onDisconnect(this, reason, details);
        } catch (Exception ex) {
            System.err.println("Error while invoking disconnect handlers");
            ex.printStackTrace();
        }

        return true;
    }

    // Get the last tick delta time
    public float tickDeltaTime() {
        return tickDt / 1000f;
    }

    // Get the last user update delta time
    public float updateDeltaTime() {
        return updateDt / 1000f;
    }

    private void sleepSafe(long ms) {
        try { Thread.sleep(ms); }
        catch (Exception ignored) { }
    }

    // run() for the tick thread
    private void runTickLoop() {
        this.tickCount.set(0);
        final long period = 50;
        long lastTick = 0;
        while (active.get() && enableTicking) {
            // execute tick
            scheduler.tick();
            onTick.invoker().onTick(this);

            tickCount.incrementAndGet();

            long now = System.currentTimeMillis();
            tickDt = now - lastTick;
            lastTick = now;

            if (tickDt < period) {
                sleepSafe(period - tickDt);
            }
        }
    }

    // run() for the update thread
    private void runUpdateLoop() {
        this.updateCount.set(0);
        long lastTick = 0;
        while (active.get() && targetUps != 0) {
            long period = 1000 / targetUps;

            // execute update
            scheduler.update();
            onUpdate.invoker().onTick(this);

            long now = System.currentTimeMillis();
            updateDt = now - lastTick;
            lastTick = now;

            if (updateDt < period) {
                sleepSafe(period - updateDt);
            }

            updateCount.incrementAndGet();
        }
    }

    public long tickCount() {
        return tickCount.get();
    }

    public long updateCount() {
        return updateCount.get();
    }

    /**
     * Try to find a packet mapping by the given name and create a new
     * packet with the given data.
     */
    public PacketContainer createPacket(String name, Object data) {
        return protocol
                .forPhase(state.phase)
                .requirePacketMapping(name)
                .createPacketContainer(this)
                .withData(data);
    }

    /**
     * Try to find a packet mapping by the given data type and create a new packet
     * with that data.
     */
    public PacketContainer createPacket(Object data) {
        return protocol
                .forPhase(state.phase)
                .requirePacketMapping(data.getClass())
                .createPacketContainer(this)
                .withData(data);
    }

    // Reset the client state to default
    private synchronized void resetState() {
        if (isResetState) {
            return;
        }

        // set inactive state
        active.set(false);
        this.state = ClientState.INACTIVE;

        // we don't have to reset the ProtocolConnection instance
        // as it is recreated for each reconnect

        // reset components
        for (ClientComponent component : this.componentList) {
            component.resetState();
        }

        // reset counters
        this.tickCount.set(0);
        this.updateCount.set(0);

        // mark flag
        this.isResetState = true;
    }

    /* ------------ Events ------------ */

    public Chain<PacketHandler> onPacketSink() {
        return onPacketSink;
    }

    public Chain<PacketHandler> onPacketReceived() {
        return onPacketReceived;
    }

    public Chain<ClientStateSwitchHandler> onStateSwitch() {
        return onStateSwitch;
    }

    public Chain<PacketHandler> onPacket() {
        return onPacket;
    }

    public MultiChain<PacketMapping, PacketHandler> onTypedReceived() {
        return onTypedReceived;
    }

    public MultiChain<PacketMapping, PacketHandler> onTypedSent() {
        return onTypedSent;
    }

    public Chain<ClientDisconnectHandler> onDisconnect() {
        return onDisconnect;
    }

    public Chain<ClientTickHandler> onTick() {
        return onTick;
    }

    public Chain<ClientTickHandler> onUpdate() {
        return onUpdate;
    }

    {
        /*
            Base Packet Handlers
         */

        onPacketReceived.addFirst(packetContainer -> {
            // Disconnect Packets
            if (packetContainer.data() instanceof ClientboundLoginDisconnectPacket) {
                disconnect(DisconnectReason.REMOTE, LegacyComponentSerializer.legacySection().deserialize(packetContainer
                        .data(ClientboundLoginDisconnectPacket.class).getReason()));
                return 0;
            }

            if (packetContainer.data() instanceof ClientboundPlayDisconnectPacket) {
                disconnect(DisconnectReason.REMOTE, packetContainer.data(ClientboundPlayDisconnectPacket.class).getReason());
                return 0;
            }

            // Set Compression
            if (packetContainer.data() instanceof ClientboundSetCompressionPacket packet) {
                connection.setCompressionThreshold(packet.getThreshold());
            }

            // Keep-Alive
            if (packetContainer.data() instanceof ClientboundKeepAlivePacket) {
                connection.sendSync(createPacket("ServerboundKeepAlive",
                        new ServerboundKeepAlivePacket(packetContainer.data(ClientboundKeepAlivePacket.class).getId())));
            }

            return 0;
        });
    }

    /* ------------ Configuration ------------ */

    public MinecraftClient enableTicking(boolean enableTicking) {
        this.enableTicking = enableTicking;
        if (!enableTicking) {
            tickThread = null;
        }

        return this;
    }

    public MinecraftClient targetUpdateRate(int targetUps) {
        this.targetUps = targetUps;
        if (targetUps == 0) {
            updateThread = null;
        }

        return this;
    }

    @Override
    public Chain<ExceptionEventHandler> onException() {
        return onError;
    }

}
