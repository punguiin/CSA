import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

public class Decryptor {
    private final BlockingQueue<byte[]> inQueue;
    private final BlockingQueue<Packet> outQueue;
    private final MessageCipher cipher;
    private final PacketDecoder decoder = new PacketDecoder();

    private volatile boolean running = false;
    private Thread worker;

    public Decryptor(BlockingQueue<byte[]> inQueue, BlockingQueue<Packet> outQueue, MessageCipher cipher) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.cipher = cipher;
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::run, "decryptor");
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private void run() {
        while (running) {
            try {
                byte[] raw = inQueue.take();
                Packet pkt = decoder.decodePacket(raw, cipher);
                outQueue.put(pkt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IllegalArgumentException | GeneralSecurityException e) {
                System.err.println("Decryptor: dropping bad packet — " + e.getMessage());
            }
        }
    }
}
