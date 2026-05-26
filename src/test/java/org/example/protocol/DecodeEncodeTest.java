package org.example.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecodeEncodeTest {

    private static final byte[] KEY = "test_key_encrypt".getBytes();

    private Packet samplePacket() {
        byte[] payload = "{\"action\":\"ping\"}".getBytes();
        Message msg = new Message(0x01020304, 777, payload);
        return new Packet((byte) 0x13, (byte) 7, 1234567890L, 0, msg);
    }

    @Test
    void encodeThenDecodeRecoversFields() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        PacketEncoder encoder = new PacketEncoder();
        PacketDecoder decoder = new PacketDecoder();

        Packet original = samplePacket();
        byte[] wire = encoder.encodePacket(original, cipher);
        Packet decoded = decoder.decodePacket(wire, cipher);

        assertEquals(original.getBMagic(), decoded.getBMagic());
        assertEquals(original.getBSrc(), decoded.getBSrc());
        assertEquals(original.getBPktId(), decoded.getBPktId());
        assertEquals(original.getBMsq().getCType(), decoded.getBMsq().getCType());
        assertEquals(original.getBMsq().getBUserId(), decoded.getBMsq().getBUserId());
        assertArrayEquals(original.getBMsq().getMessage(), decoded.getBMsq().getMessage());
    }

    @Test
    void decodedWLenMatchesCiphertextLength() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        PacketEncoder encoder = new PacketEncoder();
        PacketDecoder decoder = new PacketDecoder();

        byte[] wire = encoder.encodePacket(samplePacket(), cipher);
        Packet decoded = decoder.decodePacket(wire, cipher);

        assertEquals(wire.length - 18, decoded.getWLen());
    }

    @Test
    void corruptedHeaderTripsCrcCheck() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        PacketEncoder encoder = new PacketEncoder();
        PacketDecoder decoder = new PacketDecoder();

        byte[] wire = encoder.encodePacket(samplePacket(), cipher);
        wire[5] ^= 0x01;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> decoder.decodePacket(wire, cipher));
        assertEquals(true, ex.getMessage().startsWith("Head CRC mismatch"));
    }

    @Test
    void corruptedBodyTripsTailCrcCheck() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        PacketEncoder encoder = new PacketEncoder();
        PacketDecoder decoder = new PacketDecoder();

        byte[] wire = encoder.encodePacket(samplePacket(), cipher);
        wire[20] ^= 0x01;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> decoder.decodePacket(wire, cipher));
        assertEquals(true, ex.getMessage().startsWith("Tail CRC mismatch"));
    }

    @Test
    void corruptedMagicByteTripsHeadCrcFirst() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        PacketEncoder encoder = new PacketEncoder();
        PacketDecoder decoder = new PacketDecoder();

        byte[] wire = encoder.encodePacket(samplePacket(), cipher);
        wire[0] = 0x14;

        assertThrows(IllegalArgumentException.class, () -> decoder.decodePacket(wire, cipher));
    }
}
