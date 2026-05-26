import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

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

        short headCrc = Crc16.calculateCrc(Arrays.copyOfRange(buffer.array(), 0, 14));
        buffer.putShort(headCrc);

        buffer.put(ciphertext);

        short tailCrc = Crc16.calculateCrc(ciphertext);
        buffer.putShort(tailCrc);

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
