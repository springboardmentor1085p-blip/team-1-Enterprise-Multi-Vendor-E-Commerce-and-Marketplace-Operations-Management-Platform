package com.karthi.ecommerce.Auth.DTO;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName,

    String phone,

    // Optional role requested during registration: "CUSTOMER" (default) or "VENDOR"
    String role,

    // Vendor specific field from ER diagram: vendors.business_name
    String businessName,

    // Customer specific field from ER diagram: customer_profiles.date_of_birth
    LocalDate dateOfBirth
) {}
