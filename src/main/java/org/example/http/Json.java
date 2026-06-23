package org.example.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
    }

    static JsonNode read(InputStream body) throws IOException {
        return MAPPER.readTree(body);
    }

    static byte[] write(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Can't serialise " + value, e);
        }
    }
}
