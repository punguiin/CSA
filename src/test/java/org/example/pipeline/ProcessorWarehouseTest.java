package org.example.pipeline;

import org.example.protocol.CommandCodec;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.Packet;
import org.example.warehouse.Product;
import org.example.warehouse.ProductService;
import org.example.warehouse.SqliteProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessorWarehouseTest {

    private ProductService service;
    private BlockingQueue<Packet> in;
    private BlockingQueue<Packet> out;
    private Processor processor;
    private final AtomicLong pktId = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new ProductService(SqliteProductRepository.inMemory());
        in = new ArrayBlockingQueue<>(50);
        out = new ArrayBlockingQueue<>(50);
        processor = new Processor(in, out, service);
        processor.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        in.put(Poison.PACKET);
        processor.join();
    }

    private String send(CommandType type, byte[] payload) throws InterruptedException {
        Message msg = new Message(type.code(), 7, payload);
        in.put(new Packet((byte) 0x13, (byte) 1, pktId.getAndIncrement(), 0, msg));
        Packet resp = out.take();
        return new String(resp.getBMsq().getMessage(), StandardCharsets.UTF_8);
    }

    @Test
    void getQuantityReadsThroughTheService() throws Exception {
        int id = service.create(new Product("rice", "grocery", 42, 1200));
        assertEquals("QTY 42", send(CommandType.GET_QUANTITY, CommandCodec.getQuantity(id)));
    }

    @Test
    void getQuantityOnMissingProductReportsNotFound() throws Exception {
        assertEquals("ERR not_found", send(CommandType.GET_QUANTITY, CommandCodec.getQuantity(404)));
    }

    @Test
    void addGroupThenAddProductCreatesRowInDb() throws Exception {
        assertEquals("OK", send(CommandType.ADD_GROUP, CommandCodec.addGroup(2, "grain")));
        assertEquals("OK 10", send(CommandType.ADD_PRODUCT, CommandCodec.addProduct(2, 10, "buckwheat")));

        Product created = service.getById(10).orElseThrow();
        assertEquals("buckwheat", created.getName());
        assertEquals("grain", created.getCategory(), "category resolved from the ADD_GROUP registry");
    }

    @Test
    void incrementAndDecrementMutateStock() throws Exception {
        int id = service.create(new Product("sugar", "grocery", 100, 2000));

        assertEquals("OK 130", send(CommandType.INCREMENT, CommandCodec.changeQuantity(id, 30)));
        assertEquals("OK 110", send(CommandType.DECREMENT, CommandCodec.changeQuantity(id, 20)));
        assertEquals(110, service.getQuantity(id).getAsInt());
    }

    @Test
    void decrementBeyondStockIsRejected() throws Exception {
        int id = service.create(new Product("salt", "grocery", 5, 300));

        assertEquals("ERR insufficient_stock", send(CommandType.DECREMENT, CommandCodec.changeQuantity(id, 10)));
        assertEquals(5, service.getQuantity(id).getAsInt(), "stock left untouched");
    }

    @Test
    void setPriceUpdatesThroughTheService() throws Exception {
        int id = service.create(new Product("flour", "grocery", 10, 800));

        assertEquals("OK", send(CommandType.SET_PRICE, CommandCodec.setPrice(id, 950)));
        assertEquals(950, service.getById(id).orElseThrow().getPriceMinor());
    }
}
