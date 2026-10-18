package com.karthi.ecommerce.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.Auth.DTO.UserResponseDto;
import com.karthi.ecommerce.Model.CustomerProfile;
import com.karthi.ecommerce.Model.Role;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Repository.CustomerProfileRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public UserService(
            UserRepository userRepository,
            VendorRepository vendorRepository,
            CustomerProfileRepository customerProfileRepository
    ) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = getUserById(userId);
        return mapToDto(user);
    }

    @Transactional
    public UserResponseDto updateProfile(String email, String fullName, String phone, LocalDate dateOfBirth) {
        User user = getUserByEmail(email);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        User updated = userRepository.save(user);

        if (dateOfBirth != null) {
            CustomerProfile profile = customerProfileRepository.findByUser(user)
                    .orElseGet(() -> new CustomerProfile(user, dateOfBirth));
            profile.setDateOfBirth(dateOfBirth);
            customerProfileRepository.save(profile);
        }

        return mapToDto(updated);
    }

    @Transactional
    public void setUserStatus(Long userId, boolean active) {
        User user = getUserById(userId);
        user.setActive(active);
        userRepository.save(user);
    }

    private UserResponseDto mapToDto(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        Long vendorId = null;
        String businessName = null;
        String vendorStatus = null;
        LocalDate dob = null;

        var vendorOpt = vendorRepository.findByUser(user);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            vendorId = vendor.getVendorId();
            businessName = vendor.getBusinessName();
            vendorStatus = vendor.getStatus().name();
        }

        var customerOpt = customerProfileRepository.findByUser(user);
        if (customerOpt.isPresent()) {
            dob = customerOpt.get().getDateOfBirth();
        }

        return new UserResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.isActive(),
                roles,
                vendorId,
                businessName,
                vendorStatus,
                dob
        );
    }
}
