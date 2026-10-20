package com.karthi.ecommerce.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequestDto {

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Address line is required")
    private String addressLine;

    @NotBlank(message = "City is required")
    private String city;

    private String region;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    private String countryCode = "IN";

    private boolean isDefault = false;
}
