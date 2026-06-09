package org.example.pipeline;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.Packet;
import org.example.warehouse.ProductService;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;

public class Processor {
    private static final byte[] OK_PAYLOAD = "OK".getBytes(StandardCharsets.UTF_8);

    private final BlockingQueue<Packet> inQueue;
    private final BlockingQueue<Packet> outQueue;
    private final ProductService service;

    private final Map<Integer, String> groups = new HashMap<>();

    private Thread worker;

    public Processor(BlockingQueue<Packet> inQueue, BlockingQueue<Packet> outQueue) {
        this(inQueue, outQueue, null);
    }

    public Processor(BlockingQueue<Packet> inQueue, BlockingQueue<Packet> outQueue, ProductService service) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.service = service;
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
                byte[] payload = service == null ? OK_PAYLOAD : dispatch(reqMsg);

                Message respMsg = new Message(reqMsg.getCType(), reqMsg.getBUserId(), payload);
                Packet resp = new Packet((byte) 0x13, req.getBSrc(), req.getBPktId(), 0, respMsg);
                outQueue.put(resp);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private byte[] dispatch(Message msg) {
        CommandType type = CommandType.fromCode(msg.getCType());
        ByteBuffer b = ByteBuffer.wrap(msg.getMessage());
        String reply;
        try {
            reply = switch (type) {
                case GET_QUANTITY -> {
                    OptionalInt qty = service.getQuantity(b.getInt());
                    yield qty.isPresent() ? "QTY " + qty.getAsInt() : "ERR not_found";
                }
                case INCREMENT -> {
                    OptionalInt now = service.credit(b.getInt(), (int) b.getLong());
                    yield now.isPresent() ? "OK " + now.getAsInt() : "ERR not_found";
                }
                case DECREMENT -> {
                    OptionalInt now = service.writeOff(b.getInt(), (int) b.getLong());
                    yield now.isPresent() ? "OK " + now.getAsInt() : "ERR insufficient_stock";
                }
                case ADD_GROUP -> {
                    int groupId = b.getInt();
                    groups.put(groupId, remaining(b));
                    yield "OK";
                }
                case ADD_PRODUCT -> addProduct(b);
                case SET_PRICE -> service.setPrice(b.getInt(), b.getLong()) ? "OK" : "ERR not_found";
            };
        } catch (RuntimeException e) {
            reply = "ERR " + e.getMessage();
        }
        System.out.printf("Processor: userId=%d %s -> %s%n", msg.getBUserId(), type, reply);
        return reply.getBytes(StandardCharsets.UTF_8);
    }

    private String addProduct(ByteBuffer b) {
        int groupId = b.getInt();
        int productId = b.getInt();
        String name = remaining(b);
        String category = groups.getOrDefault(groupId, "group-" + groupId);
        try {
            service.create(new org.example.warehouse.Product(productId, name, category, 0, 0L));
            return "OK " + productId;
        } catch (RuntimeException e) {
            return "ERR exists";
        }
    }

    private static String remaining(ByteBuffer b) {
        byte[] rest = new byte[b.remaining()];
        b.get(rest);
        return new String(rest, StandardCharsets.UTF_8);
    }
}
