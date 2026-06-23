package org.example.http;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Optional;

public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final long ttlSeconds;
    private final JWTVerifier verifier;

    public JwtService(String secret, String issuer, long ttlSeconds) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String issue(String username) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(ttlSeconds))
                .sign(algorithm);
    }

    public Optional<String> verify(String token) {
        try {
            DecodedJWT decoded = verifier.verify(token);
            return Optional.ofNullable(decoded.getSubject());
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
