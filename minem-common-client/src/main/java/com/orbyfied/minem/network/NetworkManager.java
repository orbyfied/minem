package com.orbyfied.minem.network;

import com.orbyfied.minem.MinecraftClient;
import com.orbyfied.minem.exception.ClientConnectException;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;
import java.net.Socket;

@RequiredArgsConstructor
public class NetworkManager {

    public ProtocolConnection connect(MinecraftClient client, InetSocketAddress address) {
        try {
            // create socket
            Socket socket = new Socket();
            socket.connect(address);

            // create connection instance
            ProtocolConnection connection = new ProtocolConnection(this, client);
            connection.socket = socket;
            return connection;
        } catch (Exception ex) {
            throw new ClientConnectException("An exception occurred when trying to create a connection to `" + address + "`", ex);
        }
    }

}
