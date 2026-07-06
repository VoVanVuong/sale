package com.example.usertool.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void saveToken(String username, String token) {
        stringRedisTemplate.opsForValue().set(buildKey(username), token, Duration.ofMinutes(30));
    }

    public boolean isTokenValid(String username, String token) {
        String storedToken = stringRedisTemplate.opsForValue().get(buildKey(username));
        return token.equals(storedToken);
    }

    public void deleteToken(String username) {
        stringRedisTemplate.delete(buildKey(username));
    }

    private String buildKey(String username) {
        return "auth:" + username;
    }
}
