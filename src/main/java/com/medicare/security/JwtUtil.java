package com.medicare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * WHAT IS A JWT?
 *
 * A JWT (JSON Web Token) is a compact, self-contained string that
 * proves "who this user is" without the server needing to look
 * anything up in a database or session store on every request. It
 * has three parts separated by dots: header.payload.signature
 *   - header: metadata (which algorithm was used)
 *   - payload: the actual data ("claims") - here, the username and
 *     an expiration time
 *   - signature: a cryptographic signature, created using a SECRET
 *     KEY only the server knows, that proves the token wasn't
 *     tampered with after we issued it
 *
 * Login flow: user sends username+password once -> server verifies
 * them -> server creates and returns a signed JWT -> the client
 * stores that token and sends it back in an "Authorization: Bearer
 * <token>" header on every future request -> the server just
 * verifies the signature and reads the username from the token
 * itself, with NO database lookup needed to know who's asking. This
 * is what makes JWT auth "stateless" - the server doesn't need to
 * remember anything between requests.
 *
 * @Value("${...}") injects values from application.properties (which,
 * as always in this project, ultimately come from environment
 * variables - never hardcoded).
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String username) {
        String tokenUsername = extractUsername(token);
        return tokenUsername.equals(username) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
