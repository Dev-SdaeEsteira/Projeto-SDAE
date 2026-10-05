package com.cba.sdae.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MINUTES = 15;
    
    private final ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockCache = new ConcurrentHashMap<>();

    public void loginFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        attemptsCache.put(key, attempts);
        
        if (attempts >= MAX_ATTEMPTS) {
            blockCache.put(key, System.currentTimeMillis());
            log.warn("Login bloqueado para chave '{}' após {} tentativas falhas.", key, attempts);
        }
    }

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
        blockCache.remove(key);
    }

    public boolean isBlocked(String key) {
        Long blockTime = blockCache.get(key);
        if (blockTime == null) {
            return false;
        }
        
        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - blockTime);
        if (elapsedMinutes >= BLOCK_DURATION_MINUTES) {
            blockCache.remove(key);
            attemptsCache.remove(key);
            return false;
        }
        
        return true;
    }
}
