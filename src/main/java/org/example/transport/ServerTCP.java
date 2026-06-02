package org.example.transport;

import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;
import org.example.protocol.PacketEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTCP {
    private final int port;
    private final MessageCipher cipher;
    private final PacketDecoder decoder = new PacketDecoder();
    private final PacketEncoder encoder = new PacketEncoder();
    private final RequestHandler handler = new RequestHandler();
    private final Set<Socket> connections = ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ServerTCP(int port, MessageCipher cipher) {
        this.port = port;
        this.cipher = cipher;
    }

    public int port() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "tcp-accept");
        acceptThread.start();
        System.out.println("ServerTCP: listening on " + serverSocket.getLocalPort());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        for (Socket s : connections) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void join() throws InterruptedException {
        if (acceptThread != null) acceptThread.join();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handleClient(socket), "tcp-client-" + socket.getPort());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) System.err.println("ServerTCP: accept failed — " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        String peer = String.valueOf(socket.getRemoteSocketAddress());
        connections.add(socket);
        System.out.println("ServerTCP: client connected " + peer);
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            byte[] frame;
            while ((frame = Frames.read(in)) != null) {
                try {
                    Packet req = decoder.decodePacket(frame, cipher);
                    Packet resp = handler.handle(req);
                    Frames.write(out, encoder.encodePacket(resp, cipher));
                } catch (IllegalArgumentException | GeneralSecurityException e) {
                    System.err.println("ServerTCP: bad frame from " + peer + " — " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("ServerTCP: connection lost to " + peer + " — " + e.getMessage());
        } finally {
            connections.remove(socket);
            System.out.println("ServerTCP: client disconnected " + peer);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        MessageCipher cipher = new MessageCipher("0123456789abcdef".getBytes());
        ServerTCP server = new ServerTCP(port, cipher);
        server.start();
        server.join();
    }
}
