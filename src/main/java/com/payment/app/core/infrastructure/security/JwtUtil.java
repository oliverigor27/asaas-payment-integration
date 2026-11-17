package com.payment.app.core.infrastructure.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component 
public class JwtUtil {
    private final SecretKey key;

    public JwtUtil(@Value("${security.jwt-key}") String jwtKey) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    public long getExpirationSeconds() {
        return EXPIRATION_TIME / 1000;
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))   // o "sub" do JWT é sempre texto
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSecretKey())
                .compact();
    }

    public boolean validateToken(String token, Long userId) {
        try {
            return getUserId(token).equals(userId);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Long.valueOf lança NumberFormatException (filha de IllegalArgumentException) se o "sub" não for um número
    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return key;
    }
}
