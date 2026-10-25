package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    private String name;

    // Optional: slug for URL (e.g., "electronics"). Auto-generated if omitted.
    private String slug;

    // Optional: ID of parent category if this is a subcategory (e.g. Laptops under Electronics)
    private Long parentCategoryId;
}
