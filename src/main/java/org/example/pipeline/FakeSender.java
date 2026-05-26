package org.example.pipeline;

import org.example.protocol.Message;
import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketDecoder;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

public class FakeSender implements Sender {
    private final BlockingQueue<byte[]> inQueue;
    private final MessageCipher cipher;
    private final PacketDecoder decoder = new PacketDecoder();

    private Thread worker;

    public FakeSender(BlockingQueue<byte[]> inQueue, MessageCipher cipher) {
        this.inQueue = inQueue;
        this.cipher = cipher;
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
        try {
            while (true) {
                byte[] raw = inQueue.take();
                if (raw == Poison.BYTES) break;
                try {
                    Packet resp = decoder.decodePacket(raw, cipher);
                    Message msg = resp.getBMsq();
                    String payload = new String(msg.getMessage(), StandardCharsets.UTF_8);
                    System.out.printf("FakeSender: out %dB pktId=%d response=%s%n",
                            raw.length, resp.getBPktId(), payload);
                } catch (IllegalArgumentException | GeneralSecurityException e) {
                    System.err.println("FakeSender: bad outgoing frame — " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
