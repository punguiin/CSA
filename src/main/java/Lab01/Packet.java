package Lab01;

public class Packet {
    final private byte bMagic = 0x13;
    private byte bSrc;
    private long bPktId;
    private int wLen;
    private short wCrc16_head;
    private Message bMsq;
    private short wCrc16_tail;

    public Packet(byte bMagic, byte bSrc, long bPktId, int wLen, short wCrc16_head, Message bMsq, short wCrc16_tail){
        if (bMagic != this.bMagic){
            throw new IllegalArgumentException("Bad start byte: expected 0x13, got " + bMagic);
        }
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.wLen = wLen;
        this.wCrc16_head = wCrc16_head;
        this.bMsq = bMsq;
        this.wCrc16_tail = wCrc16_tail;
    }

    public byte getBMagic(){
        return bMagic;
    }

    public byte getBSrc(){
        return bSrc;
    }

    public long getBPktId(){
        return bPktId;
    }

    public int getWLen(){
        return wLen;
    }

    public short getWCrc16Head(){
        return wCrc16_head;
    }

    public Message getBMsq(){
        return bMsq;
    }

    public short getWCrc16Tail(){
        return wCrc16_tail;
    }
}
