import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

public class Encryptor {
    private final BlockingQueue<Packet> inQueue;
    private final BlockingQueue<byte[]> outQueue;
    private final MessageCipher cipher;
    private final PacketEncoder encoder = new PacketEncoder();

    private volatile boolean running = false;
    private Thread worker;

    public Encryptor(BlockingQueue<Packet> inQueue, BlockingQueue<byte[]> outQueue, MessageCipher cipher) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.cipher = cipher;
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::run, "encryptor");
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private void run() {
        while (running) {
            try {
                Packet pkt = inQueue.take();
                byte[] raw = encoder.encodePacket(pkt, cipher);
                outQueue.put(raw);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (GeneralSecurityException e) {
                System.err.println("Encryptor: failed to encrypt response — " + e.getMessage());
            }
        }
    }
}
