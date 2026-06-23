package org.example.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.warehouse.DuplicateProductNameException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

abstract class JsonHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (DuplicateProductNameException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (BadRequestException | JsonProcessingException | IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            sendError(exchange, 500, "Internal error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    protected abstract void route(HttpExchange exchange) throws IOException;

    protected JsonNode readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            JsonNode node = Json.read(in);
            if (node == null || node.isMissingNode() || node.isNull()) {
                throw new BadRequestException("Request body must be a JSON object");
            }
            return node;
        }
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = Json.write(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message == null ? "" : message));
    }

    protected void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    static final class BadRequestException extends RuntimeException {
        BadRequestException(String message) {
            super(message);
        }
    }
}
