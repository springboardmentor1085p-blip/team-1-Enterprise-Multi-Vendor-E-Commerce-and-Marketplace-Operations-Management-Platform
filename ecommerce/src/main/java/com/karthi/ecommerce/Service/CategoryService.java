package com.karthi.ecommerce.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.DTO.CategoryRequestDto;
import com.karthi.ecommerce.DTO.CategoryResponseDto;
import com.karthi.ecommerce.Model.Category;
import com.karthi.ecommerce.Repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CategoryResponseDto> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::mapToDtoWithSubcategories)
                .collect(Collectors.toList());
    }

    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        return mapToDto(category);
    }

    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(dto.getName());
        } else {
            slug = generateSlug(slug);
        }

        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("A category with slug '" + slug + "' already exists");
        }

        Category parentCategory = null;
        if (dto.getParentCategoryId() != null) {
            parentCategory = categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found with ID: " + dto.getParentCategoryId()));
        }

        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setSlug(slug);
        category.setParentCategory(parentCategory);

        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    private String generateSlug(String input) {
        return input.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }

    public CategoryResponseDto mapToDto(Category c) {
        return CategoryResponseDto.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .slug(c.getSlug())
                .parentCategoryId(c.getParentCategory() != null ? c.getParentCategory().getCategoryId() : null)
                .parentCategoryName(c.getParentCategory() != null ? c.getParentCategory().getName() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }

    public CategoryResponseDto mapToDtoWithSubcategories(Category c) {
        CategoryResponseDto dto = mapToDto(c);
        if (c.getSubCategories() != null && !c.getSubCategories().isEmpty()) {
            dto.setSubCategories(c.getSubCategories().stream()
                    .map(this::mapToDtoWithSubcategories)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
