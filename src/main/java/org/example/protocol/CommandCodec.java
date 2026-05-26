package org.example.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class CommandCodec {
    private CommandCodec() {}

    public static byte[] getQuantity(int productId) {
        return ByteBuffer.allocate(4).putInt(productId).array();
    }

    public static byte[] changeQuantity(int productId, long qty) {
        return ByteBuffer.allocate(12).putInt(productId).putLong(qty).array();
    }

    public static byte[] addGroup(int groupId, String name) {
        byte[] n = name.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(4 + n.length).putInt(groupId).put(n).array();
    }

    public static byte[] addProduct(int groupId, int productId, String name) {
        byte[] n = name.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(8 + n.length).putInt(groupId).putInt(productId).put(n).array();
    }

    public static byte[] setPrice(int productId, long priceMinor) {
        return ByteBuffer.allocate(12).putInt(productId).putLong(priceMinor).array();
    }

    public static String describe(CommandType type, byte[] payload) {
        ByteBuffer b = ByteBuffer.wrap(payload);
        return switch (type) {
            case GET_QUANTITY -> "GET_QUANTITY productId=" + b.getInt();
            case DECREMENT -> "DECREMENT productId=" + b.getInt() + " qty=" + b.getLong();
            case INCREMENT -> "INCREMENT productId=" + b.getInt() + " qty=" + b.getLong();
            case ADD_GROUP -> {
                int groupId = b.getInt();
                yield "ADD_GROUP groupId=" + groupId + " name=" + remaining(b);
            }
            case ADD_PRODUCT -> {
                int groupId = b.getInt();
                int productId = b.getInt();
                yield "ADD_PRODUCT groupId=" + groupId + " productId=" + productId + " name=" + remaining(b);
            }
            case SET_PRICE -> "SET_PRICE productId=" + b.getInt() + " price=" + b.getLong();
        };
    }

    private static String remaining(ByteBuffer b) {
        byte[] rest = new byte[b.remaining()];
        b.get(rest);
        return new String(rest, StandardCharsets.UTF_8);
    }
}
