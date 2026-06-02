package org.example.transport;

import org.example.protocol.CommandCodec;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.Packet;

import java.nio.charset.StandardCharsets;

public final class RequestHandler {
    private static final byte[] OK = "OK".getBytes(StandardCharsets.UTF_8);

    public Packet handle(Packet req) {
        Message m = req.getBMsq();
        CommandType type = CommandType.fromCode(m.getCType());
        System.out.printf("Server: src=%d pktId=%d %s%n", req.getBSrc(), req.getBPktId(), CommandCodec.describe(type, m.getMessage()));

        Message resp = new Message(m.getCType(), m.getBUserId(), OK);
        return new Packet((byte) 0x13, req.getBSrc(), req.getBPktId(), 0, resp);
    }
}
