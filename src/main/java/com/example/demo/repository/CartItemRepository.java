
package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    List<CartItem> findByCart_CartId(Integer cartId);

    Optional<CartItem> findByCart_CartIdAndProduct_ProductIdAndSelectedSizeAndSelectedColor(
            Integer cartId,
            Integer productId,
            String selectedSize,
            String selectedColor
    );

    Optional<CartItem> findByCart_CartIdAndCartItemId(
            Integer cartId,
            Integer cartItemId
    );

    void deleteByCart_CartId(Integer cartId);

    void deleteByCart_CartIdAndCartItemId(
            Integer cartId,
            Integer cartItemId
    );
}
