package org.example.transport;

import org.example.protocol.CommandFactory;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;
import org.example.protocol.PacketEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ClientUDP implements AutoCloseable {
    private static final int MAX_DATAGRAM = 64 * 1024;

    private final InetAddress host;
    private final int port;
    private final MessageCipher cipher;
    private final int timeoutMs;
    private final int maxAttempts;
    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();
    private final DatagramSocket socket;

    public ClientUDP(String host, int port, MessageCipher cipher) throws IOException {
        this(host, port, cipher, 500, 8);
    }

    public ClientUDP(String host, int port, MessageCipher cipher, int timeoutMs, int maxAttempts) throws IOException {
        this.host = InetAddress.getByName(host);
        this.port = port;
        this.cipher = cipher;
        this.timeoutMs = timeoutMs;
        this.maxAttempts = maxAttempts;
        this.socket = new DatagramSocket();
    }

    public Packet request(Packet req) throws GeneralSecurityException {
        byte[] frame = encoder.encodePacket(req, cipher);
        byte[] buf = new byte[MAX_DATAGRAM];

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                socket.send(new DatagramPacket(frame, frame.length, host, port));

                long deadline = System.currentTimeMillis() + timeoutMs;
                long remaining;
                while ((remaining = deadline - System.currentTimeMillis()) > 0) {
                    socket.setSoTimeout((int) remaining);
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socket.receive(dp);
                    try {
                        Packet resp = decoder.decodePacket(Arrays.copyOf(dp.getData(), dp.getLength()), cipher);
                        if (resp.getBPktId() == req.getBPktId()) {
                            return resp;
                        }

                    } catch (IllegalArgumentException | GeneralSecurityException e) {
                    }
                }
            } catch (SocketTimeoutException e) {

            } catch (IOException e) {
                System.err.println("ClientUDP: I/O error — " + e.getMessage());
                return null;
            }
            System.out.printf("ClientUDP: no ack for pktId=%d, retransmit %d/%d%n",
                    req.getBPktId(), attempt, maxAttempts);
        }
        return null;
    }

    @Override
    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        byte src = (byte) (args.length > 2 ? Integer.parseInt(args[2]) : 1);

        MessageCipher cipher = new MessageCipher("0123456789abcdef".getBytes());
        Random rng = new Random();
        AtomicLong seq = new AtomicLong(1);
        int acked = 0;
        int failed = 0;

        try (ClientUDP client = new ClientUDP(host, port, cipher)) {
            for (int i = 0; i < 20; i++) {
                Packet req = CommandFactory.randomRequest(src, seq.getAndIncrement(), rng);
                Packet resp = client.request(req);
                if (resp != null) {
                    acked++;
                    System.out.printf("ClientUDP[%d]: ack pktId=%d%n", src, resp.getBPktId());
                } else {
                    failed++;
                    System.out.printf("ClientUDP[%d]: FAILED pktId=%d%n", src, req.getBPktId());
                }
                Thread.sleep(300);
            }
        }
        System.out.printf("ClientUDP[%d]: done — %d acked, %d failed%n", src, acked, failed);
    }
}
