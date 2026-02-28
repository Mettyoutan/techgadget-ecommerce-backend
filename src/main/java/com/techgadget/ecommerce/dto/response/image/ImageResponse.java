package com.techgadget.ecommerce.dto.response.image;


public record ImageResponse(
        Long id,
        String url,
        boolean isPrimary
) {}
