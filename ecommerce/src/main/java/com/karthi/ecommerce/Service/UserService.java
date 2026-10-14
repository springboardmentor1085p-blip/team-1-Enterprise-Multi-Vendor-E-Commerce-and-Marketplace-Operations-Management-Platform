package com.karthi.ecommerce.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.karthi.ecommerce.Model.Role;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.RoleRepository;
import com.karthi.ecommerce.Repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

     private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                    new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<Role> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new RuntimeException("User not found"));

        return List.copyOf(user.getRoles());
    }
}
