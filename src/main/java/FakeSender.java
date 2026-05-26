import java.util.HexFormat;
import java.util.concurrent.BlockingQueue;

public class FakeSender implements Sender {
    private final BlockingQueue<byte[]> inQueue;

    private Thread worker;

    public FakeSender(BlockingQueue<byte[]> inQueue) {
        this.inQueue = inQueue;
    }

    @Override
    public void start() {
        if (worker != null) return;
        worker = new Thread(this::run, "fake-sender");
        worker.start();
    }

    @Override
    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        HexFormat hex = HexFormat.of();
        try {
            while (true) {
                byte[] raw = inQueue.take();
                if (raw == Poison.BYTES) break;
                System.out.println("FakeSender: out " + raw.length + "B -> " + hex.formatHex(raw));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
