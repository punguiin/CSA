package org.example.transport;

import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;
import org.example.protocol.PacketEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class ServerUDP {
    private static final int MAX_DATAGRAM = 64 * 1024;
    private static final int CACHE_SIZE = 1024;

    private final int port;
    private final MessageCipher cipher;
    private final double lossProbability;
    private final PacketDecoder decoder = new PacketDecoder();
    private final PacketEncoder encoder = new PacketEncoder();
    private final RequestHandler handler = new RequestHandler();
    private final Random rng = new Random();

    private final Map<String, byte[]> recentResponses = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private volatile boolean running;
    private DatagramSocket socket;
    private Thread worker;

    public ServerUDP(int port, MessageCipher cipher) {
        this(port, cipher, 0.0);
    }

    public ServerUDP(int port, MessageCipher cipher, double lossProbability) {
        this.port = port;
        this.cipher = cipher;
        this.lossProbability = lossProbability;
    }

    public int port() {
        return socket != null ? socket.getLocalPort() : port;
    }

    public void start() throws IOException {
        socket = new DatagramSocket(port);
        running = true;
        worker = new Thread(this::run, "udp-server");
        worker.start();
        System.out.println("ServerUDP: listening on " + socket.getLocalPort()
                + (lossProbability > 0 ? " (simulated loss=" + lossProbability + ")" : ""));
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        byte[] buf = new byte[MAX_DATAGRAM];
        while (running) {
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);

                if (drop()) {
                    System.out.println("ServerUDP: (simulated loss) dropped inbound datagram");
                    continue;
                }

                String client = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                byte[] frame = Arrays.copyOf(dp.getData(), dp.getLength());
                byte[] response = buildResponse(client, frame);
                if (response == null) continue;

                if (drop()) {
                    System.out.println("ServerUDP: (simulated loss) dropped response to " + client);
                    continue;
                }
                socket.send(new DatagramPacket(response, response.length, dp.getAddress(), dp.getPort()));
            } catch (IOException e) {
                if (running) System.err.println("ServerUDP: " + e.getMessage());
            }
        }
    }

    private byte[] buildResponse(String client, byte[] frame) {
        try {
            Packet req = decoder.decodePacket(frame, cipher);
            String dedupKey = client + "#" + req.getBPktId();

            byte[] cached = recentResponses.get(dedupKey);
            if (cached != null) {
                System.out.printf("ServerUDP: duplicate pktId=%d from %s, re-sending cached ack%n",
                        req.getBPktId(), client);
                return cached;
            }

            Packet resp = handler.handle(req);
            byte[] encoded = encoder.encodePacket(resp, cipher);
            recentResponses.put(dedupKey, encoded);
            return encoded;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            System.err.println("ServerUDP: bad frame from " + client + " — " + e.getMessage());
            return null;
        }
    }

    private boolean drop() {
        return lossProbability > 0 && rng.nextDouble() < lossProbability;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9091;
        double loss = args.length > 1 ? Double.parseDouble(args[1]) : 0.0;
        MessageCipher cipher = new MessageCipher("0123456789abcdef".getBytes());
        ServerUDP server = new ServerUDP(port, cipher, loss);
        server.start();
        server.join();
    }
}
