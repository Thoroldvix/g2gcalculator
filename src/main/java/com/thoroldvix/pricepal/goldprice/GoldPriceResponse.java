package com.thoroldvix.pricepal.goldprice;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
record GoldPriceResponse(
        BigDecimal price,
        String server,
        LocalDateTime updatedAt
) {
}