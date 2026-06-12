package com.pabloncf.saas.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

public record PageMeta(
        int page,
        int size,
        long total,
        @JsonProperty("total_pages") int totalPages
) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
