package org.example;

import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.pipeline.Decryptor;
import org.example.pipeline.Encryptor;
import org.example.pipeline.FakeReceiver;
import org.example.pipeline.FakeSender;
import org.example.pipeline.Processor;
import org.example.pipeline.Receiver;
import org.example.pipeline.Sender;
import org.example.warehouse.Product;
import org.example.warehouse.ProductService;
import org.example.warehouse.SqliteProductRepository;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class App {
    private static final byte[] KEY = "0123456789abcdef".getBytes();
    private static final int QUEUE_CAPACITY = 100;

    public static void main(String[] args) throws InterruptedException {
        MessageCipher cipher = new MessageCipher(KEY);

        ProductService warehouse = new ProductService(SqliteProductRepository.inMemory());
        seed(warehouse);

        BlockingQueue<byte[]> rawIn = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<Packet> decoded = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<Packet> processed = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<byte[]> rawOut = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        Receiver receiver = new FakeReceiver(rawIn, cipher, (byte) 1);
        Decryptor decryptor = new Decryptor(rawIn, decoded, cipher);
        Processor processor = new Processor(decoded, processed, warehouse);
        Encryptor encryptor = new Encryptor(processed, rawOut, cipher);
        Sender sender = new FakeSender(rawOut, cipher);

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
        System.out.println("App: warehouse holds " + warehouse.count() + " products");
    }

    private static void seed(ProductService warehouse) {
        String[] names = {"buckwheat", "rice", "sugar", "salt", "flour"};
        for (int id = 1; id <= names.length; id++) {
            warehouse.create(new Product(id, names[id - 1], "grocery", 100, 100L * id));
        }
    }
}
