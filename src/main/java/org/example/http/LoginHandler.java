package org.example.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class LoginHandler extends JsonHandler {

    private final UserStore users;
    private final JwtService jwt;

    public LoginHandler(UserStore users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @Override
    protected void route(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Use POST /login");
            return;
        }

        JsonNode body = readBody(exchange);
        String username = text(body, "username", text(body, "login", null));
        String password = text(body, "password", null);

        if (!users.authenticate(username, password)) {
            sendError(exchange, 401, "Invalid username or password");
            return;
        }
        sendJson(exchange, 200, Map.of("token", jwt.issue(username)));
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : fallback;
    }
}
