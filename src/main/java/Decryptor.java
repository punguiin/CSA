import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

public class Decryptor {
    private final BlockingQueue<byte[]> inQueue;
    private final BlockingQueue<Packet> outQueue;
    private final MessageCipher cipher;
    private final PacketDecoder decoder = new PacketDecoder();

    private Thread worker;

    public Decryptor(BlockingQueue<byte[]> inQueue, BlockingQueue<Packet> outQueue, MessageCipher cipher) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.cipher = cipher;
    }

    public void start() {
        if (worker != null) return;
        worker = new Thread(this::run, "decryptor");
        worker.start();
    }

    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        try {
            while (true) {
                byte[] raw = inQueue.take();
                if (raw == Poison.BYTES) {
                    outQueue.put(Poison.PACKET);
                    break;
                }
                try {
                    Packet pkt = decoder.decodePacket(raw, cipher);
                    outQueue.put(pkt);
                } catch (IllegalArgumentException | GeneralSecurityException e) {
                    System.err.println("Decryptor: dropping bad packet — " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
