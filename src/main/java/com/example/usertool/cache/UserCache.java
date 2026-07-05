package com.example.usertool.cache;

/**
 * Stores per-user auth state (token version, enabled flag) in a fast cache,
 * intended as the source of truth for future JWT validation.
 */
public interface UserCache {

    /**
     * Seed the initial auth state for a freshly registered user
     * (tokenVersion = 0, enabled = true).
     */
    void initialize(Long userId);
}
