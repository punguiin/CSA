package org.example.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import org.example.warehouse.Product;
import org.example.warehouse.ProductService;

import java.io.IOException;
import java.util.Optional;

public class ProductHandler extends JsonHandler {

    private static final String BASE = "/products";

    private final ProductService products;

    public ProductHandler(ProductService products) {
        this.products = products;
    }

    @Override
    protected void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        Optional<Integer> id = pathId(exchange);

        switch (method) {
            case "GET" -> {
                if (id.isEmpty()) {
                    sendError(exchange, 405, "Use GET /products/{id}");
                } else {
                    getById(exchange, id.get());
                }
            }
            case "PUT" -> {
                if (id.isPresent()) {
                    sendError(exchange, 405, "Use PUT /products (no id)");
                } else {
                    create(exchange);
                }
            }
            case "POST" -> {
                if (id.isEmpty()) {
                    sendError(exchange, 405, "Use POST /products/{id}");
                } else {
                    update(exchange, id.get());
                }
            }
            case "DELETE" -> {
                if (id.isEmpty()) {
                    sendError(exchange, 405, "Use DELETE /products/{id}");
                } else {
                    delete(exchange, id.get());
                }
            }
            default -> sendError(exchange, 405, "Unsupported method " + method);
        }
    }

    private void getById(HttpExchange exchange, int id) throws IOException {
        Optional<Product> found = products.getById(id);
        if (found.isPresent()) {
            sendJson(exchange, 200, found.get());
        } else {
            sendError(exchange, 404, "No product with id " + id);
        }
    }

    private void create(HttpExchange exchange) throws IOException {
        JsonNode body = readBody(exchange);
        Product product = new Product(
                requiredText(body, "name"),
                requiredText(body, "category"),
                optInt(body, "quantity", 0),
                optLong(body, "priceMinor", 0L));
        products.createUnique(product);
        sendJson(exchange, 201, product);
    }

    private void update(HttpExchange exchange, int id) throws IOException {
        Optional<Product> existing = products.getById(id);
        if (existing.isEmpty()) {
            sendError(exchange, 404, "No product with id " + id);
            return;
        }
        Product current = existing.get();
        JsonNode body = readBody(exchange);
        Product updated = new Product(
                id,
                optText(body, "name", current.getName()),
                optText(body, "category", current.getCategory()),
                optInt(body, "quantity", current.getQuantity()),
                optLong(body, "priceMinor", current.getPriceMinor()));
        products.update(updated);
        sendJson(exchange, 200, updated);
    }

    private void delete(HttpExchange exchange, int id) throws IOException {
        if (products.delete(id)) {
            sendEmpty(exchange, 204);
        } else {
            sendError(exchange, 404, "No product with id " + id);
        }
    }

    private Optional<Integer> pathId(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String rest = path.length() > BASE.length() ? path.substring(BASE.length()) : "";
        if (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        if (rest.contains("/")) {
            throw new BadRequestException("Unsupported path " + path);
        }
        try {
            return Optional.of(Integer.parseInt(rest));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Product id must be an integer: " + rest);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optText(node, field, null);
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Missing required field '" + field + "'");
        }
        return value;
    }

    private static String optText(JsonNode node, String field, String fallback) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : fallback;
    }

    private static int optInt(JsonNode node, String field, int fallback) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return fallback;
        }
        if (!v.isNumber()) {
            throw new BadRequestException("Field '" + field + "' must be a number");
        }
        return v.asInt();
    }

    private static long optLong(JsonNode node, String field, long fallback) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return fallback;
        }
        if (!v.isNumber()) {
            throw new BadRequestException("Field '" + field + "' must be a number");
        }
        return v.asLong();
    }
}
