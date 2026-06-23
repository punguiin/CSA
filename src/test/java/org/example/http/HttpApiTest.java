package org.example.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.warehouse.Product;
import org.example.warehouse.ProductService;
import org.example.warehouse.SqliteProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProductService warehouse;
    private HttpApiServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        warehouse = new ProductService(SqliteProductRepository.inMemory());
        warehouse.create(new Product(1, "rice", "grocery", 50, 1200));
        warehouse.create(new Product(2, "salt", "grocery", 10, 300));

        JwtService jwt = new JwtService("test-secret", "warehouse-test", 3600);
        UserStore users = UserStore.withDemoUser();

        server = new HttpApiServer(0, warehouse, jwt, users);
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + server.port();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void loginReturnsToken() throws Exception {
        HttpResponse<String> res = login("admin", "admin");

        assertEquals(200, res.statusCode());
        String token = json(res).get("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        HttpResponse<String> res = login("admin", "nope");
        assertEquals(401, res.statusCode());
    }

    @Test
    void loginRejectsNonPostMethod() throws Exception {
        HttpResponse<String> res = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login")).GET().build(),
                BodyHandlers.ofString());
        assertEquals(405, res.statusCode());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        HttpResponse<String> res = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1")).GET().build(),
                BodyHandlers.ofString());
        assertEquals(401, res.statusCode());
    }

    @Test
    void protectedEndpointRejectsBogusToken() throws Exception {
        HttpResponse<String> res = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1"))
                        .header("Authorization", "Bearer not.a.jwt")
                        .GET().build(),
                BodyHandlers.ofString());
        assertEquals(401, res.statusCode());
    }

    @Test
    void getExistingProduct() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1")).GET());

        assertEquals(200, res.statusCode());
        JsonNode body = json(res);
        assertEquals(1, body.get("id").asInt());
        assertEquals("rice", body.get("name").asText());
        assertEquals(1200, body.get("priceMinor").asLong());
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/999")).GET());
        assertEquals(404, res.statusCode());
    }

    @Test
    void getCollectionIsNotAllowed() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products")).GET());
        assertEquals(405, res.statusCode());
    }

    @Test
    void createProduct() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products"))
                        .PUT(BodyPublishers.ofString(
                                "{\"name\":\"sugar\",\"category\":\"grocery\",\"quantity\":7,\"priceMinor\":900}")));

        assertEquals(201, res.statusCode());
        JsonNode body = json(res);
        int id = body.get("id").asInt();
        assertEquals("sugar", body.get("name").asText());
        assertTrue(warehouse.getById(id).isPresent());
    }

    @Test
    void createWithDuplicateNameReturns409() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products"))
                        .PUT(BodyPublishers.ofString(
                                "{\"name\":\"rice\",\"category\":\"grocery\",\"quantity\":1,\"priceMinor\":1}")));
        assertEquals(409, res.statusCode());
    }

    @Test
    void createWithoutNameReturns400() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products"))
                        .PUT(BodyPublishers.ofString("{\"category\":\"grocery\"}")));
        assertEquals(400, res.statusCode());
    }

    @Test
    void putWithIdIsNotAllowed() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1"))
                        .PUT(BodyPublishers.ofString("{\"name\":\"x\",\"category\":\"y\"}")));
        assertEquals(405, res.statusCode());
    }

    @Test
    void updateProduct() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1"))
                        .POST(BodyPublishers.ofString(
                                "{\"name\":\"brown rice\",\"category\":\"grain\",\"quantity\":80,\"priceMinor\":1500}")));

        assertEquals(200, res.statusCode());
        Product updated = warehouse.getById(1).orElseThrow();
        assertEquals("brown rice", updated.getName());
        assertEquals("grain", updated.getCategory());
        assertEquals(80, updated.getQuantity());
        assertEquals(1500, updated.getPriceMinor());
    }

    @Test
    void partialUpdateKeepsUntouchedFields() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/1"))
                        .POST(BodyPublishers.ofString("{\"quantity\":5}")));

        assertEquals(200, res.statusCode());
        Product updated = warehouse.getById(1).orElseThrow();
        assertEquals("rice", updated.getName(), "name should be unchanged");
        assertEquals(5, updated.getQuantity());
        assertEquals(1200, updated.getPriceMinor(), "price should be unchanged");
    }

    @Test
    void updateMissingProductReturns404() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/999"))
                        .POST(BodyPublishers.ofString("{\"name\":\"ghost\",\"category\":\"x\"}")));
        assertEquals(404, res.statusCode());
    }

    @Test
    void deleteProduct() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/2")).DELETE());

        assertEquals(204, res.statusCode());
        assertTrue(warehouse.getById(2).isEmpty());
    }

    @Test
    void deleteMissingProductReturns404() throws Exception {
        HttpResponse<String> res = authed(
                HttpRequest.newBuilder(URI.create(baseUrl + "/products/999")).DELETE());
        assertEquals(404, res.statusCode());
    }

    private HttpResponse<String> login(String user, String password) throws IOException, InterruptedException {
        String body = "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}";
        return client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(body)).build(),
                BodyHandlers.ofString());
    }

    private String token() throws IOException, InterruptedException {
        return json(login("admin", "admin")).get("token").asText();
    }

    private HttpResponse<String> authed(HttpRequest.Builder builder) throws IOException, InterruptedException {
        return client.send(
                builder.header("Authorization", "Bearer " + token()).build(),
                BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> res) throws IOException {
        return MAPPER.readTree(res.body());
    }
}
