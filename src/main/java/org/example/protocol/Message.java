package org.example.protocol;

public class Message {
    final private int cType;
    final private int bUserId;
    final private byte[] message;

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
