package org.example.protocol;

public enum CommandType {
    GET_QUANTITY(1),
    DECREMENT(2),
    INCREMENT(3),
    ADD_GROUP(4),
    ADD_PRODUCT(5),
    SET_PRICE(6);

    private final int code;

    CommandType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CommandType fromCode(int code) {
        for (CommandType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown command code: " + code);
    }
}
