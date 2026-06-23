package org.example.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.example.warehouse.ProductService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpApiServer {

    private static final String REALM = "warehouse";

    private final HttpServer server;

    public HttpApiServer(int port, ProductService products, JwtService jwt, UserStore users) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler(users, jwt));

        HttpContext productContext = server.createContext("/products", new ProductHandler(products));
        productContext.setAuthenticator(new BearerAuthenticator(jwt, REALM));

        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("HttpApiServer: listening on " + port());
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }
}
