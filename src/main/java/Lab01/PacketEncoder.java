package Lab01;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class PacketEncoder {
    public byte[] encodePacket(Packet pkt, MessageCipher cipher) throws GeneralSecurityException {
        byte[] plaintext = encodeMessage(pkt.getBMsq());
        byte[] ciphertext = cipher.encrypt(plaintext);
        int wLen = ciphertext.length;

        ByteBuffer buffer = ByteBuffer.allocate(16 + wLen + 2);
        buffer.put(pkt.getBMagic());
        buffer.put(pkt.getBSrc());
        buffer.putLong(pkt.getBPktId());
        buffer.putInt(wLen);
        buffer.putShort(pkt.getWCrc16Head());
        buffer.put(ciphertext);
        buffer.putShort(pkt.getWCrc16Tail());

        return buffer.array();
    }

    private byte[] encodeMessage(Message msg) {
        byte[] payload = msg.getMessage();
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length);
        buffer.putInt(msg.getCType());
        buffer.putInt(msg.getBUserId());
        buffer.put(payload);

        return buffer.array();
    }
}
