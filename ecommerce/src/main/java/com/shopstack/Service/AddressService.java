package com.shopstack.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopstack.DTO.AddressRequestDto;
import com.shopstack.DTO.AddressResponseDto;
import com.shopstack.Model.Address;
import com.shopstack.Model.User;
import com.shopstack.Repository.AddressRepository;
import com.shopstack.Repository.UserRepository;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AddressResponseDto addAddress(Long userId, AddressRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<Address> existing = addressRepository.findByUser_UserId(userId);
        boolean isFirst = existing.isEmpty();

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(dto.getRecipientName());
        address.setPhone(dto.getPhone());
        address.setAddressLine(dto.getAddressLine());
        address.setCity(dto.getCity());
        address.setRegion(dto.getRegion());
        address.setPostalCode(dto.getPostalCode());
        address.setCountryCode(dto.getCountryCode() != null ? dto.getCountryCode() : "IN");
        address.setDefault(isFirst || dto.isDefault());

        if (address.isDefault() && !isFirst) {
            existing.forEach(a -> {
                a.setDefault(false);
                addressRepository.save(a);
            });
        }

        Address saved = addressRepository.save(address);
        return mapToDto(saved);
    }

    public List<AddressResponseDto> getUserAddresses(Long userId) {
        return addressRepository.findByUser_UserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AddressResponseDto getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }
        return mapToDto(address);
    }

    public AddressResponseDto mapToDto(Address a) {
        return AddressResponseDto.builder()
                .addressId(a.getAddressId())
                .userId(a.getUser().getUserId())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .addressLine(a.getAddressLine())
                .city(a.getCity())
                .region(a.getRegion())
                .postalCode(a.getPostalCode())
                .countryCode(a.getCountryCode())
                .isDefault(a.isDefault())
                .build();
    }
}
