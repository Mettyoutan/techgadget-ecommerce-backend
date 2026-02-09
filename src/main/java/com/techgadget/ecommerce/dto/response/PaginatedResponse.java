package com.techgadget.ecommerce.dto.response;

import lombok.*;

import java.util.List;

/**
 * Generic DTO response for pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNextPage;
    private boolean hasPreviousPage;
    private List<T> content;

}
