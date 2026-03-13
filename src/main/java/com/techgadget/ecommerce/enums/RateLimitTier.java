package com.techgadget.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RateLimitTier {
    AUTH(10, 60), // 10 req / 60 sec
    WRITE(30, 60), // 30 req / 60 sec
    READ(100, 60); // 100 req / 60 sec

    private final int maxRequests;
    private final int windowSizeSeconds;
}
