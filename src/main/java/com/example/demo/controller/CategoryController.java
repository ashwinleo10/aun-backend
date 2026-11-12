package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.entity.Category;
import com.example.demo.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ==========================================================
    // CREATE CATEGORY
    // ADMIN ONLY
    // ==========================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        Category category =
                categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(category);
    }

    // ==========================================================
    // GET ACTIVE CATEGORIES
    // PUBLIC
    // Used by customer website
    // ==========================================================

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {

        List<Category> categories =
                categoryService.getAllCategories();

        return ResponseEntity.ok(categories);
    }

    // ==========================================================
    // GET ALL CATEGORIES
    // ADMIN ONLY
    // Includes active and inactive categories
    // ==========================================================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Category>> getAllCategoriesForAdmin() {

        List<Category> categories =
                categoryService.getAllCategoriesForAdmin();

        return ResponseEntity.ok(categories);
    }

    // ==========================================================
    // GET CATEGORY BY ID
    // PUBLIC
    // ==========================================================

    @GetMapping("/{categoryId}")
    public ResponseEntity<Category> getCategoryById(
            @PathVariable Integer categoryId) {

        Category category =
                categoryService.getCategoryById(categoryId);

        return ResponseEntity.ok(category);
    }

    // ==========================================================
    // UPDATE CATEGORY
    // ADMIN ONLY
    // ==========================================================

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody CategoryRequest request) {

        Category category =
                categoryService.updateCategory(
                        categoryId,
                        request);

        return ResponseEntity.ok(category);
    }

    // ==========================================================
    // DEACTIVATE CATEGORY
    // ADMIN ONLY
    // ==========================================================

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCategory(
            @PathVariable Integer categoryId) {

        categoryService.deleteCategory(categoryId);

        return ResponseEntity.ok(
                "Category deactivated successfully");
    }

    // ==========================================================
    // ACTIVATE CATEGORY
    // ADMIN ONLY
    // ==========================================================

    @PatchMapping("/{categoryId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> activateCategory(
            @PathVariable Integer categoryId) {

        Category category =
                categoryService.activateCategory(categoryId);

        return ResponseEntity.ok(category);
    }
}