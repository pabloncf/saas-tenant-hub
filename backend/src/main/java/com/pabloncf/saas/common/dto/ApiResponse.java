package com.pabloncf.saas.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ApiResponse<T>(T data, PageMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<List<T>> ofPage(Page<T> page) {
        return new ApiResponse<>(page.getContent(), PageMeta.from(page));
    }
}
