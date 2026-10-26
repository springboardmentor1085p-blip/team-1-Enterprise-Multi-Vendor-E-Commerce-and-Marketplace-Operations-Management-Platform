package com.shopstack.DTO;

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

    // Optional: auto-generated from name if not provided
    private String slug;

    // Optional: set to make this a subcategory
    private Long parentCategoryId;
}
