package com.karthi.ecommerce.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.Auth.DTO.UserResponseDto;
import com.karthi.ecommerce.Service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfile(
                userService.getUserByEmail(authentication.getName()).getUserId()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {
        String fullName = body.get("fullName");
        String phone = body.get("phone");
        String dobStr = body.get("dateOfBirth");
        LocalDate dateOfBirth = dobStr != null && !dobStr.isBlank() ? LocalDate.parse(dobStr) : null;

        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), fullName, phone, dateOfBirth));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> toggleUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean active = body.getOrDefault("active", true);
        userService.setUserStatus(userId, active);
        return ResponseEntity.ok("User status updated successfully");
    }
}
