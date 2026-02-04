package com.github.dimitryivaniuta.booking.api.dto;

import java.util.List;

/**
 * Generic paged response.
 *
 * @param items items in the current page
 * @param page zero-based page index
 * @param size page size
 * @param totalElements total items
 * @param totalPages total pages
 */
public record PagedResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
