package com.shopstack.Repository;

import com.shopstack.Model.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * BUG 5 FIX: Fetches all users with their roles in a SINGLE SQL query using JOIN FETCH.
     * Without this, UserService.getAllUsers() triggers N+1 queries:
     *   - 1 query to get all users
     *   - 1 additional query PER USER to fetch their roles
     * For 100 users = 101 queries. With this = 1 query.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllWithRoles();
}
