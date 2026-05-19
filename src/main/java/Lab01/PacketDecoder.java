package Lab01;

import java.nio.ByteBuffer;

public class PacketDecoder {
    public Packet encodePacket(byte[] arr){
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        byte bMagic = buffer.get(0);
        byte bSrc = buffer.get(1);
        long bPktId = buffer.getLong(2);
        int wLen = buffer.getInt(10);
        short wCrc16_head = buffer.getShort(14);
        byte[] bMsg = new byte[wLen];
        buffer.get(bMsg, wLen,16);
        short wCrc16_tail = buffer.getShort(16 + wLen);

        buffer = ByteBuffer.wrap(bMsg);
        int cType = buffer.getInt(0);
        int bUserId = buffer.getInt(4);
        byte[] message = new byte[wLen - 8];
        buffer.get(message, wLen - 8, 8);

        Message msg = new Message(cType, bUserId, message);
        Packet pkt = new Packet(bMagic, bSrc, bPktId, wLen, wCrc16_head, msg, wCrc16_tail);
        return pkt;
    }
}
