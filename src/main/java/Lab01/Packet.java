package Lab01;

public class Packet {
    final private byte bMagic = 0x13;
    final private byte bSrc;
    final private long bPktId;
    final private int wLen;
    final private Message bMsq;

    public Packet(byte bMagic, byte bSrc, long bPktId, int wLen, Message bMsq){
        if (bMagic != this.bMagic){
            throw new IllegalArgumentException("Bad start byte: expected 0x13, got " + bMagic);
        }
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.wLen = wLen;
        this.bMsq = bMsq;
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

    public Message getBMsq(){
        return bMsq;
    }
}
