package com.karthi.ecommerce.Auth;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.karthi.ecommerce.Model.Role;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.RoleRepository;
import com.karthi.ecommerce.Repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Ensure essential roles exist
        Role roleAdmin = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        roleRepository.findByRoleName("ROLE_VENDOR")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_VENDOR")));

        roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CUSTOMER")));

        // 2. Seed a default Administrator account if one doesn't exist
        String adminEmail = "admin@shopstack.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFullName("Platform Administrator");
            admin.setPhone("+1234567890");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setActive(true);

            Set<Role> roles = new HashSet<>();
            roles.add(roleAdmin);
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println(">>> [DataInitializer] Seeded default admin account: " + adminEmail + " (password: Admin@123)");
        }
    }
}
