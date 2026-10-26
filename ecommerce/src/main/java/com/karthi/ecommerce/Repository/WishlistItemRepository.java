package com.karthi.ecommerce.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.Product;
import com.karthi.ecommerce.Model.Wishlist;
import com.karthi.ecommerce.Model.WishlistItem;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByWishlistAndProduct(Wishlist wishlist, Product product);
    void deleteByWishlist(Wishlist wishlist);
}
