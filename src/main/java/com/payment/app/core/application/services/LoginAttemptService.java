package com.payment.app.core.application.services;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.payment.app.core.application.exceptions.TooManyAttemptsException;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;

@Service 
public class LoginAttemptService {
    private final Cache<String, Bucket> porIp = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build();
    
    private final Cache<String, Bucket> porEmail = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build();
    
    public void verificarIp(String ip) {
        Bucket bucket = porIp.get(ip, k -> Bucket.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
            .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed())
            throw new TooManyAttemptsException(probe.getNanosToWaitForRefill());
    }

    public void verificarEmail(String email) {
        Bucket bucket = porEmail.getIfPresent(email);
        if (bucket == null) return;
        EstimationProbe probe = bucket.estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            throw new TooManyAttemptsException(probe.getNanosToWaitForRefill());
        }
    }

    public void registrarFalha(String email) {
        porEmail.get(email, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(5).refillIntervally(1, Duration.ofMinutes(3)))
                .build())
            .tryConsume(1);
    }

    public void registrarSucesso(String email) {
        porEmail.invalidate(email);
    }
}
