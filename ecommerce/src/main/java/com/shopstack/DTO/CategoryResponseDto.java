package com.shopstack.DTO;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    private Long categoryId;
    private String name;
    private String slug;
    private Long parentCategoryId;
    private String parentCategoryName;
    private List<CategoryResponseDto> subCategories;
    private OffsetDateTime createdAt;
}
