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

    private Packet generateRandomPacket() {
        int cType = 1 + rng.nextInt(6);
        int bUserId = rng.nextInt(1000);
        byte[] payload = new byte[1 + rng.nextInt(16)];
        rng.nextBytes(payload);
        Message msg = new Message(cType, bUserId, payload);
        return new Packet((byte) 0x13, bSrc, pktIdSeq.getAndIncrement(), 0, msg);
    }
}
