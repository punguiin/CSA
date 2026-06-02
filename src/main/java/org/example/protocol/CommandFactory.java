package org.example.protocol;

import java.util.Random;

/**
 * Builds random warehouse command packets. Shared by the in-process
 * {@code FakeReceiver} and the TCP/UDP network clients so they all speak the
 * same wire protocol without duplicating payload-building logic.
 */
public final class CommandFactory {
    private static final String[] NAMES = {"buckwheat", "rice", "sugar", "salt", "flour"};
    private static final CommandType[] COMMANDS = CommandType.values();

    private CommandFactory() {}

    public static Packet randomRequest(byte src, long pktId, Random rng) {
        CommandType type = COMMANDS[rng.nextInt(COMMANDS.length)];
        int userId = rng.nextInt(1000);
        byte[] payload = buildPayload(type, rng);
        Message msg = new Message(type.code(), userId, payload);
        return new Packet((byte) 0x13, src, pktId, 0, msg);
    }

    private static byte[] buildPayload(CommandType type, Random rng) {
        return switch (type) {
            case GET_QUANTITY -> CommandCodec.getQuantity(productId(rng));
            case DECREMENT, INCREMENT -> CommandCodec.changeQuantity(productId(rng), 1 + rng.nextInt(50));
            case ADD_GROUP -> CommandCodec.addGroup(groupId(rng), name(rng));
            case ADD_PRODUCT -> CommandCodec.addProduct(groupId(rng), productId(rng), name(rng));
            case SET_PRICE -> CommandCodec.setPrice(productId(rng), 100L * (1 + rng.nextInt(1000)));
        };
    }

    private static int productId(Random rng) {
        return 1 + rng.nextInt(5);
    }

    private static int groupId(Random rng) {
        return 1 + rng.nextInt(3);
    }

    private static String name(Random rng) {
        return NAMES[rng.nextInt(NAMES.length)];
    }
}
