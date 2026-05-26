package org.example.pipeline;

import org.example.protocol.CommandCodec;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketEncoder;

import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class FakeReceiver implements Receiver {
    private final BlockingQueue<byte[]> outQueue;
    private final MessageCipher cipher;
    private final PacketEncoder encoder = new PacketEncoder();
    private final AtomicLong pktIdSeq = new AtomicLong(1);
    private final Random rng = new Random();
    private final byte bSrc;

    private volatile boolean running = false;
    private Thread worker;

    public FakeReceiver(BlockingQueue<byte[]> outQueue, MessageCipher cipher, byte bSrc) {
        this.outQueue = outQueue;
        this.cipher = cipher;
        this.bSrc = bSrc;
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::run, "fake-receiver");
        worker.start();
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        try {
            while (running) {
                Packet pkt = generateRandomPacket();
                byte[] raw = encoder.encodePacket(pkt, cipher);
                outQueue.put(raw);
                Thread.sleep(50 + rng.nextInt(150));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encode random packet", e);
        } finally {
            sendPoison();
        }
    }

    private void sendPoison() {
        try {
            outQueue.put(Poison.BYTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final String[] NAMES = {"buckwheat", "rice", "sugar", "salt", "flour"};
    private static final CommandType[] COMMANDS = CommandType.values();

    private Packet generateRandomPacket() {
        CommandType type = COMMANDS[rng.nextInt(COMMANDS.length)];
        int bUserId = rng.nextInt(1000);
        byte[] payload = buildPayload(type);
        Message msg = new Message(type.code(), bUserId, payload);
        return new Packet((byte) 0x13, bSrc, pktIdSeq.getAndIncrement(), 0, msg);
    }

    private byte[] buildPayload(CommandType type) {
        return switch (type) {
            case GET_QUANTITY -> CommandCodec.getQuantity(randomProductId());
            case DECREMENT -> CommandCodec.changeQuantity(randomProductId(), 1 + rng.nextInt(50));
            case INCREMENT -> CommandCodec.changeQuantity(randomProductId(), 1 + rng.nextInt(50));
            case ADD_GROUP -> CommandCodec.addGroup(randomGroupId(), randomName());
            case ADD_PRODUCT -> CommandCodec.addProduct(randomGroupId(), randomProductId(), randomName());
            case SET_PRICE -> CommandCodec.setPrice(randomProductId(), 100L * (1 + rng.nextInt(1000)));
        };
    }

    private int randomProductId() {
        return 1 + rng.nextInt(5);
    }

    private int randomGroupId() {
        return 1 + rng.nextInt(3);
    }

    private String randomName() {
        return NAMES[rng.nextInt(NAMES.length)];
    }
}
