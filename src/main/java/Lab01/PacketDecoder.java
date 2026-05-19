package Lab01;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class PacketDecoder {
    public Packet decodePacket(byte[] arr, MessageCipher cipher) throws GeneralSecurityException {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        byte bMagic = buffer.get(0);
        byte bSrc = buffer.get(1);
        long bPktId = buffer.getLong(2);
        int wLen = buffer.getInt(10);
        short wCrc16_head = buffer.getShort(14);

        short computedHeadCrc = Crc16.calculateCrc(Arrays.copyOfRange(arr, 0, 14));
        if (computedHeadCrc != wCrc16_head) {
            throw new IllegalArgumentException(String.format("Head CRC mismatch: wire=0x%04X, computed=0x%04X", wCrc16_head & 0xFFFF, computedHeadCrc & 0xFFFF));
        }

        byte[] ciphertext = new byte[wLen];
        buffer.position(16);
        buffer.get(ciphertext, 0, wLen);

        short wCrc16_tail = buffer.getShort(16 + wLen);

        short computedTailCrc = Crc16.calculateCrc(ciphertext);
        if (computedTailCrc != wCrc16_tail) {
            throw new IllegalArgumentException(String.format("Tail CRC mismatch: wire=0x%04X, computed=0x%04X", wCrc16_tail & 0xFFFF, computedTailCrc & 0xFFFF));
        }

        byte[] plaintext = cipher.decrypt(ciphertext);
        Message msg = decodeMessage(plaintext);

        return new Packet(bMagic, bSrc, bPktId, wLen, msg);
    }

    private Message decodeMessage(byte[] plaintext) {
        ByteBuffer buffer = ByteBuffer.wrap(plaintext);
        int cType = buffer.getInt(0);
        int bUserId = buffer.getInt(4);
        byte[] payload = new byte[plaintext.length - 8];
        buffer.position(8);
        buffer.get(payload, 0, payload.length);

        return new Message(cType, bUserId, payload);
    }
}
