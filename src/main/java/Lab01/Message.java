package Lab01;

public class Message {
    private int cType;
    private int bUserId;
    private byte[] message;

    public Message(int cType, int bUserId, byte[] message){
        this.cType = cType;
        this.bUserId = bUserId;
        this.message = message;
    }

    public int getCType(){
        return cType;
    }

    public int getBUserId(){
        return bUserId;
    }

    public byte[] getMessage(){
        return message;
    }
}
