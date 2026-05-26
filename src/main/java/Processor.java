import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class Processor {
    private static final byte[] OK_PAYLOAD = "OK".getBytes(StandardCharsets.UTF_8);

    private final BlockingQueue<Packet> inQueue;
    private final BlockingQueue<Packet> outQueue;

    private Thread worker;

    public Processor(BlockingQueue<Packet> inQueue, BlockingQueue<Packet> outQueue) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
    }

    public void start() {
        if (worker != null) return;
        worker = new Thread(this::run, "processor");
        worker.start();
    }

    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        try {
            while (true) {
                Packet req = inQueue.take();
                if (req == Poison.PACKET) {
                    outQueue.put(Poison.PACKET);
                    break;
                }
                Message reqMsg = req.getBMsq();
                System.out.printf("Processor: cType=%d userId=%d payloadLen=%d pktId=%d%n",
                        reqMsg.getCType(), reqMsg.getBUserId(), reqMsg.getMessage().length, req.getBPktId());

                Message respMsg = new Message(reqMsg.getCType(), reqMsg.getBUserId(), OK_PAYLOAD);
                Packet resp = new Packet((byte) 0x13, req.getBSrc(), req.getBPktId(), 0, respMsg);
                outQueue.put(resp);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
