package org.example.http;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.util.Optional;

public class BearerAuthenticator extends Authenticator {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwt;
    private final String realm;

    public BearerAuthenticator(JwtService jwt, String realm) {
        this.jwt = jwt;
        this.realm = realm;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            return new Failure(401);
        }
        String token = header.substring(PREFIX.length()).trim();
        Optional<String> username = jwt.verify(token);
        return username
                .<Result>map(name -> new Success(new HttpPrincipal(name, realm)))
                .orElseGet(() -> new Failure(401));
    }
}
