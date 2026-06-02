package org.example.transport;

import org.example.protocol.CommandFactory;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;
import org.example.protocol.PacketEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ClientTCP {
    private static final long MIN_BACKOFF_MS = 200;
    private static final long MAX_BACKOFF_MS = 2000;
    private static final int CONNECT_TIMEOUT_MS = 1000;

    private final String host;
    private final int port;
    private final MessageCipher cipher;
    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();
    private final Consumer<Packet> onResponse;

    private final BlockingQueue<Packet> outbox = new LinkedBlockingQueue<>();
    private final AtomicLong received = new AtomicLong();

    private volatile boolean running;
    private Thread worker;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public ClientTCP(String host, int port, MessageCipher cipher) {
        this(host, port, cipher, null);
    }

    public ClientTCP(String host, int port, MessageCipher cipher, Consumer<Packet> onResponse) {
        this.host = host;
        this.port = port;
        this.cipher = cipher;
        this.onResponse = onResponse;
    }

    public void submit(Packet req) {
        outbox.add(req);
    }

    public long receivedCount() {
        return received.get();
    }

    public int pendingCount() {
        return outbox.size();
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::run, "tcp-client");
        worker.start();
    }

    public void stop() {
        running = false;
        closeQuietly();
        if (worker != null) worker.interrupt();
    }

    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        Packet inFlight = null;
        long backoff = MIN_BACKOFF_MS;
        while (running) {
            if (!isConnected()) {
                if (connect()) {
                    backoff = MIN_BACKOFF_MS;
                } else {
                    sleep(backoff);
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    continue; // do not touch the outbox while the server is unreachable
                }
            }
            try {
                Packet req = (inFlight != null) ? inFlight : outbox.poll(200, TimeUnit.MILLISECONDS);
                if (req == null) continue; // nothing to send yet
                inFlight = req;            // remember until a response confirms delivery

                Frames.write(out, encoder.encodePacket(req, cipher));
                byte[] frame = Frames.read(in);
                if (frame == null) throw new IOException("server closed connection");

                Packet resp = decoder.decodePacket(frame, cipher);
                received.incrementAndGet();
                if (onResponse != null) onResponse.accept(resp);
                inFlight = null;
            } catch (IOException e) {
                System.err.println("ClientTCP: connection lost — " + e.getMessage() + "; pausing sends, will reconnect");
                closeQuietly();
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                System.err.println("ClientTCP: bad response frame — " + e.getMessage());
                inFlight = null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        closeQuietly();
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private boolean connect() {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket = s;
            in = s.getInputStream();
            out = s.getOutputStream();
            System.out.println("ClientTCP: connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.out.println("ClientTCP: server unavailable (" + e.getMessage() + "), retrying…");
            return false;
        }
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        socket = null;
        in = null;
        out = null;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        byte src = (byte) (args.length > 2 ? Integer.parseInt(args[2]) : 1);

        MessageCipher cipher = new MessageCipher("0123456789abcdef".getBytes());
        ClientTCP client = new ClientTCP(host, port, cipher,resp -> System.out.printf("ClientTCP[%d]: ack pktId=%d%n", src, resp.getBPktId()));
        client.start();

        Random rng = new Random();
        AtomicLong seq = new AtomicLong(1);
        for (int i = 0; i < 50; i++) {
            client.submit(CommandFactory.randomRequest(src, seq.getAndIncrement(), rng));
            Thread.sleep(500);
        }
        Thread.sleep(2000);
        client.stop();
        client.join();
        System.out.printf("ClientTCP[%d]: done — %d responses received%n", src, client.receivedCount());
    }
}
