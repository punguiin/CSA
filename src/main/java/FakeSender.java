import java.util.HexFormat;
import java.util.concurrent.BlockingQueue;

public class FakeSender implements Sender {
    private final BlockingQueue<byte[]> inQueue;

    private volatile boolean running = false;
    private Thread worker;

    public FakeSender(BlockingQueue<byte[]> inQueue) {
        this.inQueue = inQueue;
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::run, "fake-sender");
        worker.start();
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private void run() {
        HexFormat hex = HexFormat.of();
        while (running) {
            try {
                byte[] raw = inQueue.take();
                System.out.println("FakeSender: out " + raw.length + "B -> " + hex.formatHex(raw));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
