package org.example.pipeline;

import org.example.protocol.MessageCipher;
import org.example.protocol.Packet;
import org.example.protocol.PacketEncoder;

import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;

public class Encryptor {
    private final BlockingQueue<Packet> inQueue;
    private final BlockingQueue<byte[]> outQueue;
    private final MessageCipher cipher;
    private final PacketEncoder encoder = new PacketEncoder();

    private Thread worker;

    public Encryptor(BlockingQueue<Packet> inQueue, BlockingQueue<byte[]> outQueue, MessageCipher cipher) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.cipher = cipher;
    }

    public void start() {
        if (worker != null) return;
        worker = new Thread(this::run, "encryptor");
        worker.start();
    }

    public void join() throws InterruptedException {
        if (worker != null) worker.join();
    }

    private void run() {
        try {
            while (true) {
                Packet pkt = inQueue.take();
                if (pkt == Poison.PACKET) {
                    outQueue.put(Poison.BYTES);
                    break;
                }
                try {
                    byte[] raw = encoder.encodePacket(pkt, cipher);
                    outQueue.put(raw);
                } catch (GeneralSecurityException e) {
                    System.err.println("Encryptor: failed to encrypt — " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
