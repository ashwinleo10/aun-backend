package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.entity.Category;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // ==========================================================
    // CREATE CATEGORY
    // ==========================================================

    public Category createCategory(CategoryRequest request) {

        validateCategory(request);

        String categoryName = request.getCategoryName().trim();

        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new IllegalArgumentException(
                    "Category already exists: " + categoryName);
        }

        Category category = new Category();

        category.setCategoryName(categoryName);
        category.setDescription(request.getDescription());
        category.setActive(true);

        return categoryRepository.save(category);
    }

    // ==========================================================
    // GET ALL CATEGORIES
    // ADMIN
    // Returns both active and inactive categories
    // ==========================================================

    public List<Category> getAllCategoriesForAdmin() {

        return categoryRepository.findAll();
    }

    // ==========================================================
    // GET ACTIVE CATEGORIES
    // CUSTOMER / PUBLIC
    // ==========================================================

    public List<Category> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .filter(category ->
                        Boolean.TRUE.equals(category.getActive()))
                .toList();
    }

    // ==========================================================
    // GET CATEGORY BY ID
    // ==========================================================

    public Category getCategoryById(Integer categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ResourceNotFoundException(
                    "Category not found with ID: " + categoryId);
        }

        return category;
    }

    // ==========================================================
    // UPDATE CATEGORY
    // ADMIN
    // ==========================================================

    public Category updateCategory(
            Integer categoryId,
            CategoryRequest request) {

        validateCategory(request);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId));

        String categoryName = request.getCategoryName().trim();

        boolean nameExists =
                categoryRepository.existsByCategoryNameIgnoreCase(
                        categoryName);

        if (nameExists &&
                !category.getCategoryName()
                        .equalsIgnoreCase(categoryName)) {

            throw new IllegalArgumentException(
                    "Category already exists: " + categoryName);
        }

        category.setCategoryName(categoryName);
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    // ==========================================================
    // DEACTIVATE CATEGORY
    // ADMIN
    // ==========================================================

    public void deleteCategory(Integer categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId));

        category.setActive(false);

        categoryRepository.save(category);
    }

    // ==========================================================
    // ACTIVATE CATEGORY
    // ADMIN
    // ==========================================================

    public Category activateCategory(Integer categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId));

        category.setActive(true);

        return categoryRepository.save(category);
    }

    // ==========================================================
    // VALIDATION
    // ==========================================================

    private void validateCategory(CategoryRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Category details are required");
        }

        if (request.getCategoryName() == null ||
                request.getCategoryName().isBlank()) {

            throw new IllegalArgumentException(
                    "Category name is required");
        }

        if (request.getDescription() != null &&
                request.getDescription().length() > 500) {

            throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters");
        }
    }
}