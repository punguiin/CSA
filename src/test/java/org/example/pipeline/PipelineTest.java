package org.example.pipeline;

import org.example.protocol.CommandCodec;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;
import org.example.protocol.PacketEncoder;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PipelineTest {

    private static final byte[] KEY = "0123456789abcdef".getBytes();
    private static final byte[] OK = "OK".getBytes(StandardCharsets.UTF_8);

    @Test
    void concurrentRequestsAllGetOkResponse() throws Exception {
        int threads = 8;
        int perThread = 200;
        int total = threads * perThread;

        MessageCipher cipher = new MessageCipher(KEY);
        BlockingQueue<byte[]> rawIn = new ArrayBlockingQueue<>(100);
        BlockingQueue<Packet> decoded = new ArrayBlockingQueue<>(100);
        BlockingQueue<Packet> processed = new ArrayBlockingQueue<>(100);
        BlockingQueue<byte[]> rawOut = new ArrayBlockingQueue<>(100);

        Decryptor decryptor = new Decryptor(rawIn, decoded, cipher);
        Processor processor = new Processor(decoded, processed);
        Encryptor encryptor = new Encryptor(processed, rawOut, cipher);

        decryptor.start();
        processor.start();
        encryptor.start();

        AtomicLong pktIdSeq = new AtomicLong(1);
        ExecutorService producers = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            producers.submit(() -> {
                PacketEncoder encoder = new PacketEncoder();
                try {
                    for (int i = 0; i < perThread; i++) {
                        Message msg = new Message(CommandType.INCREMENT.code(), 1,
                                CommandCodec.changeQuantity(7, 20));
                        Packet pkt = new Packet((byte) 0x13, (byte) 1, pktIdSeq.getAndIncrement(), 0, msg);
                        rawIn.put(encoder.encodePacket(pkt, cipher));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        producers.shutdown();

        Thread closer = new Thread(() -> {
            try {
                producers.awaitTermination(30, TimeUnit.SECONDS);
                rawIn.put(Poison.BYTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        closer.start();

        PacketDecoder decoder = new PacketDecoder();
        int count = 0;
        while (true) {
            byte[] out = rawOut.take();
            if (out == Poison.BYTES) break;
            Packet resp = decoder.decodePacket(out, cipher);
            assertArrayEquals(OK, resp.getBMsq().getMessage());
            count++;
        }

        assertEquals(total, count);

        closer.join();

        decryptor.join();
        processor.join();
        encryptor.join();
    }

    @Test
    void fullPipelineShutsDownCleanly() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            MessageCipher cipher = new MessageCipher(KEY);
            BlockingQueue<byte[]> rawIn = new ArrayBlockingQueue<>(100);
            BlockingQueue<Packet> decoded = new ArrayBlockingQueue<>(100);
            BlockingQueue<Packet> processed = new ArrayBlockingQueue<>(100);
            BlockingQueue<byte[]> rawOut = new ArrayBlockingQueue<>(100);

            Receiver receiver = new FakeReceiver(rawIn, cipher, (byte) 1);
            Decryptor decryptor = new Decryptor(rawIn, decoded, cipher);
            Processor processor = new Processor(decoded, processed);
            Encryptor encryptor = new Encryptor(processed, rawOut, cipher);
            Sender sender = new FakeSender(rawOut, cipher);

            sender.start();
            encryptor.start();
            processor.start();
            decryptor.start();
            receiver.start();

            Thread.sleep(300);

            receiver.stop();
            receiver.join();
            decryptor.join();
            processor.join();
            encryptor.join();
            sender.join();
        });
    }
}
