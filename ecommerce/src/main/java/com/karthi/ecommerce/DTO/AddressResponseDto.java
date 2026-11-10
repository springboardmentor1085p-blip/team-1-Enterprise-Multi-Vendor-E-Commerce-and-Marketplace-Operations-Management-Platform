package com.karthi.ecommerce.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {

    private Long addressId;
    private Long userId;
    private String recipientName;
    private String phone;
    private String addressLine;
    private String city;
    private String region;
    private String postalCode;
    private String countryCode;
    private boolean isDefault;
}
