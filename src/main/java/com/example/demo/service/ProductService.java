package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // Add a new product
    @Transactional
    public Product addProduct(ProductRequest request) {

        validateProduct(request);

        Category category = getActiveCategory(request.getCategoryId());

        Product product = new Product();

        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setCategory(category);
        product.setSizes(request.getSizes());
        product.setColors(request.getColors());
        product.setImageUrls(request.getImageUrls());
        product.setStockQuantity(request.getStockQuantity());

        product.setActive(true);

        return productRepository.save(product);
    }

    // Get all active products
    // Public / Customer
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .filter(product ->
                        Boolean.TRUE.equals(product.getActive()))
                .toList();
    }

    // Get all products including inactive products
    // ADMIN only
    @Transactional(readOnly = true)
    public List<Product> getAllProductsForAdmin() {

        return productRepository.findAll();
    }

    // Get product by ID
    // Active products only
    @Transactional(readOnly = true)
    public Product getProductById(Integer productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + productId);
        }

        return product;
    }

    // Update an existing product
    // Admin can update active and inactive products.
    // Updating an inactive product automatically reactivates it.
    @Transactional
    public Product updateProduct(
            Integer productId,
            ProductRequest request) {

        validateProduct(request);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

        Category category =
                getActiveCategory(request.getCategoryId());

        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setCategory(category);
        product.setSizes(request.getSizes());
        product.setColors(request.getColors());
        product.setImageUrls(request.getImageUrls());
        product.setStockQuantity(request.getStockQuantity());

        // Reactivate product when admin updates it
        product.setActive(true);

        return productRepository.save(product);
    }

    // Soft delete / deactivate a product
    // ADMIN only
    @Transactional
    public void deleteProduct(Integer productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

        product.setActive(false);

        productRepository.save(product);
    }

    // Activate a previously deactivated product
    // ADMIN only
    @Transactional
    public Product activateProduct(Integer productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

        product.setActive(true);

        return productRepository.save(product);
    }

    // Find and validate an active category
    private Category getActiveCategory(Integer categoryId) {

        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found with ID: "
                                                + categoryId));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException(
                    "Cannot assign an inactive category to a product");
        }

        return category;
    }

    // Validate product information
    private void validateProduct(ProductRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Product details are required");
        }

        if (request.getProductName() == null
                || request.getProductName().isBlank()) {

            throw new IllegalArgumentException(
                    "Product name is required");
        }

        if (request.getPrice() == null
                || request.getPrice()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Price must be greater than zero");
        }

        if (request.getDiscountPrice() != null
                && (request.getDiscountPrice()
                            .compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscountPrice()
                            .compareTo(request.getPrice()) > 0)) {

            throw new IllegalArgumentException(
                    "Discount price must be greater than zero "
                    + "and cannot exceed the original price");
        }

        if (request.getCategoryId() == null
                || request.getCategoryId() <= 0) {

            throw new IllegalArgumentException(
                    "A valid category ID is required");
        }

        if (request.getStockQuantity() == null
                || request.getStockQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative");
        }
    }
}
