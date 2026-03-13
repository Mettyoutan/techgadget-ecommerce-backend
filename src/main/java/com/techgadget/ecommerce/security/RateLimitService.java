package com.techgadget.ecommerce.security;

import com.techgadget.ecommerce.enums.RateLimitTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Rate limit using slide window algorithm
 * -
 * Using redis ZSet data structure ()
 * > Value: current millis in string
 * > Score: current millis in long
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> stringRedisTemplate;

    /**
     * 1) Delete timestamp outside window (by score)
     * 2) Count total requests within window
     * 3) If < max requests -> add & increment
     */
    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT = RedisScript.of("""
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window_start = tonumber(ARGV[2])
        local max_requests = tonumber(ARGV[3])
        local ttl = tonumber(ARGV[4])
        local member = ARGV[5]
        
        redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
        
        local count = redis.call('ZCOUNT', key, window_start, now)
    
        if count < max_requests then
            redis.call('ZADD', key, now, member)
            redis.call('EXPIRE', key, ttl)
        end
        
        return count
        """, Long.class);

    /**
     * Add current request timestamp (If allowed)
     * -
     * Key can be:
     * -> IP Address or user id
     */
    public boolean isAllowed(String key, RateLimitTier tier) {
        long now = System.currentTimeMillis();
        long windowStart = now - (tier.getWindowSizeSeconds() * 1000L); // now - 10 sec
        String redisKey = "rate:" + key;
        String uniqueMember = now + ":" + UUID.randomUUID();

        // Run redis script
        Long count = stringRedisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(redisKey),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(tier.getMaxRequests()),
                String.valueOf(tier.getWindowSizeSeconds() + 1),
                uniqueMember
        );

        return count != null && count <= tier.getMaxRequests();
    }

    /**
     * Used when rate limit is hit
     * -
     * To know when user can access endpoint after sec duration
     */
    public long getRetryAfterSeconds(String key, RateLimitTier tier) {
        long now = System.currentTimeMillis();
        long windowStart = now - (tier.getWindowSizeSeconds() * 1000L);
        String redisKey = "rate:" + key;

        // Find the oldest entry by score
        Double oldest = stringRedisTemplate.opsForZSet()
                .rangeByScoreWithScores(redisKey, windowStart, now)
                .stream().findFirst()
                .map(ZSetOperations.TypedTuple::getScore)
                .orElse(null);

        if (oldest == null) return 0;

        // (oldest timestamp + window size - now) + 1 sec
        return (long) ((oldest + (tier.getWindowSizeSeconds() * 1000L) - now) / 1000L) + 1;
    }
}
