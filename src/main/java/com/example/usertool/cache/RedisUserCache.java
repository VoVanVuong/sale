package com.example.usertool.cache;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUserCache implements UserCache {

    static final String KEY_PREFIX = "user:auth:";
    static final String TOKEN_VERSION_FIELD = "tokenVersion";
    static final String ENABLED_FIELD = "enabled";

    private final StringRedisTemplate redisTemplate;

    public RedisUserCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void initialize(Long userId) {
        String key = key(userId);
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        // HSETNX per field: seed only if absent, so re-seeding never resets an
        // existing tokenVersion (which would un-revoke previously issued tokens).
        hash.putIfAbsent(key, TOKEN_VERSION_FIELD, "0");
        hash.putIfAbsent(key, ENABLED_FIELD, "true");
    }

    static String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
