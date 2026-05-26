import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class App {
    private static final byte[] KEY = "0123456789abcdef".getBytes();
    private static final int QUEUE_CAPACITY = 100;

    public static void main(String[] args) throws InterruptedException {
        MessageCipher cipher = new MessageCipher(KEY);

        BlockingQueue<byte[]> rawIn = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<Packet> decoded = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<Packet> processed = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<byte[]> rawOut = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        Receiver receiver = new FakeReceiver(rawIn, cipher, (byte) 1);
        Decryptor decryptor = new Decryptor(rawIn, decoded, cipher);
        Processor processor = new Processor(decoded, processed);
        Encryptor encryptor = new Encryptor(processed, rawOut, cipher);
        Sender sender = new FakeSender(rawOut);

        sender.start();
        encryptor.start();
        processor.start();
        decryptor.start();
        receiver.start();

        Thread.sleep(3000);

        receiver.stop();
        receiver.join();
        decryptor.join();
        processor.join();
        encryptor.join();
        sender.join();

        System.out.println("App: pipeline stopped cleanly");
    }
}
