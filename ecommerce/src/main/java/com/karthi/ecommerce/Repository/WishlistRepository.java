package com.karthi.ecommerce.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserEmail(String email);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items i LEFT JOIN FETCH i.product WHERE w.user.email = :email")
    Optional<Wishlist> findByUserEmailWithItems(@Param("email") String email);
}
