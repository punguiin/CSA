import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketTest {

    @Test
    void constructorAcceptsCorrectMagicByte() {
        Message msg = new Message(1, 42, new byte[]{0});
        Packet pkt = new Packet((byte) 0x13, (byte) 0, 0L, 0, msg);

        assertEquals((byte) 0x13, pkt.getBMagic());
    }

    @Test
    void constructorRejectsWrongMagicByte() {
        Message msg = new Message(1, 42, new byte[]{0});

        assertThrows(IllegalArgumentException.class,
            () -> new Packet((byte) 0x14, (byte) 0, 0L, 0, msg));
    }

    @Test
    void gettersReturnConstructorValues() {
        Message msg = new Message(7, 99, new byte[]{1, 2, 3});
        Packet pkt = new Packet((byte) 0x13, (byte) 5, 12345L, 11, msg);

        assertEquals((byte) 5, pkt.getBSrc());
        assertEquals(12345L, pkt.getBPktId());
        assertEquals(11, pkt.getWLen());
        assertEquals(msg, pkt.getBMsq());
    }
}
