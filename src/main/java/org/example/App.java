package org.example;

import org.example.http.HttpApiServer;
import org.example.http.JwtService;
import org.example.http.UserStore;
import org.example.protocol.MessageCipher;
import org.example.transport.ServerTCP;
import org.example.transport.ServerUDP;
import org.example.warehouse.Product;
import org.example.warehouse.ProductService;
import org.example.warehouse.SqliteProductRepository;

import java.io.IOException;

public class App {

    private static final byte[] KEY = "0123456789abcdef".getBytes();

    private static final String JWT_SECRET = "change-me-in-production";
    private static final String JWT_ISSUER = "warehouse";
    private static final long JWT_TTL_SECONDS = 3600;

    public static void main(String[] args) throws IOException {
        int tcpPort = port("TCP_PORT", 9090);
        int udpPort = port("UDP_PORT", 9091);
        int httpPort = port("HTTP_PORT", 8080);

        ProductService warehouse = new ProductService(SqliteProductRepository.inMemory());
        seed(warehouse);

        MessageCipher cipher = new MessageCipher(KEY);
        ServerTCP tcp = new ServerTCP(tcpPort, cipher);
        ServerUDP udp = new ServerUDP(udpPort, cipher);

        JwtService jwt = new JwtService(JWT_SECRET, JWT_ISSUER, JWT_TTL_SECONDS);
        UserStore users = UserStore.withDemoUser();
        HttpApiServer http = new HttpApiServer(httpPort, warehouse, jwt, users);

        tcp.start();
        udp.start();
        http.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("App: shutting down");
            http.stop();
            udp.stop();
            tcp.stop();
        }));

        System.out.println("App: warehouse up on TCP/" + tcp.port()
                + ", UDP/" + udp.port() + ", HTTP/" + http.port()
                + " (" + warehouse.count() + " products, demo login admin/admin)");
    }

    private static int port(String envVar, int fallback) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private static void seed(ProductService warehouse) {
        String[] names = {"buckwheat", "rice", "sugar", "salt", "flour"};
        for (int id = 1; id <= names.length; id++) {
            warehouse.create(new Product(id, names[id - 1], "grocery", 100, 100L * id));
        }
    }
}
