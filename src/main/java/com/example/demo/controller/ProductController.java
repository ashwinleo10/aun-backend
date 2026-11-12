package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // =========================================================
    // ADMIN - ADD PRODUCT
    // =========================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody ProductRequest request) {

        Product product =
                productService.addProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ProductResponse.fromEntity(product));
    }

    // =========================================================
    // CUSTOMER - GET ALL ACTIVE PRODUCTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> products =
                productService.getAllProducts()
                        .stream()
                        .map(ProductResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(products);
    }

    // =========================================================
    // ADMIN - GET ALL PRODUCTS
    // Includes ACTIVE + INACTIVE products
    // =========================================================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>>
            getAllProductsForAdmin() {

        List<ProductResponse> products =
                productService.getAllProductsForAdmin()
                        .stream()
                        .map(ProductResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(products);
    }

    // =========================================================
    // CUSTOMER - GET ACTIVE PRODUCT BY ID
    // =========================================================

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Integer productId) {

        Product product =
                productService.getProductById(productId);

        return ResponseEntity.ok(
                ProductResponse.fromEntity(product));
    }

    // =========================================================
    // ADMIN - UPDATE PRODUCT
    // =========================================================

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Integer productId,
            @Valid @RequestBody ProductRequest request) {

        Product product =
                productService.updateProduct(
                        productId,
                        request);

        return ResponseEntity.ok(
                ProductResponse.fromEntity(product));
    }

    // =========================================================
    // ADMIN - DEACTIVATE PRODUCT
    // Soft Delete
    // =========================================================

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Integer productId) {

        productService.deleteProduct(productId);

        return ResponseEntity.ok(
                "Product deactivated successfully");
    }

    // =========================================================
    // ADMIN - ACTIVATE PRODUCT
    // =========================================================

    @PatchMapping("/{productId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> activateProduct(
            @PathVariable Integer productId) {

        Product product =
                productService.activateProduct(productId);

        return ResponseEntity.ok(
                ProductResponse.fromEntity(product));
    }
}
