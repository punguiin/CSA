package org.example.transport;

import org.example.protocol.CommandFactory;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransportTest {
    private static final MessageCipher CIPHER = new MessageCipher("0123456789abcdef".getBytes());

    @Test
    void tcpDeliversEveryResponse() throws Exception {
        ServerTCP server = new ServerTCP(0, CIPHER);
        server.start();
        ClientTCP client = new ClientTCP("localhost", server.port(), CIPHER);
        client.start();
        try {
            Random rng = new Random(1);
            AtomicLong seq = new AtomicLong(1);
            int n = 20;
            for (int i = 0; i < n; i++) {
                client.submit(CommandFactory.randomRequest((byte) 1, seq.getAndIncrement(), rng));
            }
            waitUntil(() -> client.receivedCount() == n, 5000);
            assertEquals(n, client.receivedCount());
        } finally {
            client.stop();
            client.join();
            server.stop();
            server.join();
        }
    }

    @Test
    void tcpClientReconnectsAfterServerRestart() throws Exception {
        ServerTCP server = new ServerTCP(0, CIPHER);
        server.start();
        int port = server.port();

        ClientTCP client = new ClientTCP("localhost", port, CIPHER);
        client.start();
        Random rng = new Random(2);
        AtomicLong seq = new AtomicLong(1);

        for (int i = 0; i < 5; i++) {
            client.submit(CommandFactory.randomRequest((byte) 1, seq.getAndIncrement(), rng));
        }
        waitUntil(() -> client.receivedCount() == 5, 5000);
        assertEquals(5, client.receivedCount());

        server.stop();
        server.join();

        for (int i = 0; i < 5; i++) {
            client.submit(CommandFactory.randomRequest((byte) 1, seq.getAndIncrement(), rng));
        }
        Thread.sleep(1000);
        assertEquals(5, client.receivedCount(), "nothing should be delivered while the server is down");

        ServerTCP server2 = new ServerTCP(port, CIPHER);
        server2.start();
        try {
            waitUntil(() -> client.receivedCount() == 10, 8000);
            assertEquals(10, client.receivedCount(), "queued requests delivered after reconnect");
        } finally {
            client.stop();
            client.join();
            server2.stop();
            server2.join();
        }
    }

    @Test
    void udpRetransmitsThroughHeavyLoss() throws Exception {
        ServerUDP server = new ServerUDP(0, CIPHER, 0.5); // drop ~50% each way
        server.start();
        try (ClientUDP client = new ClientUDP("localhost", server.port(), CIPHER, 300, 40)) {
            Packet req = CommandFactory.randomRequest((byte) 1, 1L, new Random(3));
            Packet resp = client.request(req);
            assertNotNull(resp, "retransmission should eventually get through 50% loss");
            assertEquals(req.getBPktId(), resp.getBPktId());
        } finally {
            server.stop();
            server.join();
        }
    }

    private static void waitUntil(BooleanSupplier cond, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline && !cond.getAsBoolean()) {
            Thread.sleep(50);
        }
    }
}
