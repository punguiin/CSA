package Lab01;

import java.nio.ByteBuffer;

public class PacketEncoder {
    public byte[] encodePacket(Packet pkt){
        ByteBuffer buffer = ByteBuffer.allocate(16 + pkt.getWLen() + 2);
        buffer.put(pkt.getBMagic());
        buffer.put(pkt.getBSrc());
        buffer.putLong(pkt.getBPktId());
        buffer.putInt(pkt.getWLen());
        buffer.putShort(pkt.getWCrc16Head());
        buffer.put(encodeMessage(pkt.getBMsq(), pkt.getWLen() - 8));
        buffer.putShort(pkt.getWCrc16Tail());

        return buffer.array();
    }

    private byte[] encodeMessage(Message msg, int len){
        ByteBuffer buffer = ByteBuffer.allocate(8 + len);
        buffer.putInt(msg.getCType());
        buffer.putInt(msg.getBUserId());
        buffer.put(msg.getMessage());

        return buffer.array();
    }
}
