package com.karthi.ecommerce.Auth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.Enum.VendorStatus;
import com.karthi.ecommerce.DTO.AuthResponse;
import com.karthi.ecommerce.DTO.LoginRequest;
import com.karthi.ecommerce.DTO.RegisterRequest;
import com.karthi.ecommerce.DTO.UserResponseDto;
import com.karthi.ecommerce.Model.CustomerProfile;
import com.karthi.ecommerce.Model.Role;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Model.Vendor;
import com.karthi.ecommerce.Repository.CustomerProfileRepository;
import com.karthi.ecommerce.Repository.RoleRepository;
import com.karthi.ecommerce.Repository.UserRepository;
import com.karthi.ecommerce.Repository.VendorRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VendorRepository vendorRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            VendorRepository vendorRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.vendorRepository = vendorRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        String requestedRole = request.role() != null ? request.role().toUpperCase().trim() : "CUSTOMER";

        if (!requestedRole.startsWith("ROLE_")) {
            requestedRole = "ROLE_" + requestedRole;
        }

        // Safety check: Prevent direct ADMIN registration via public API
        if ("ROLE_ADMIN".equals(requestedRole)) {
            throw new IllegalArgumentException("Cannot register as ADMIN directly");
        }

        String finalRoleName = requestedRole;
        Role role = roleRepository.findByRoleName(finalRoleName)
                .orElseGet(() -> roleRepository.save(new Role(finalRoleName)));
        roles.add(role);

        // Always also add customer role if registering as vendor
        if ("ROLE_VENDOR".equals(requestedRole)) {
            Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_CUSTOMER")));
            roles.add(customerRole);
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // If vendor, create Vendor entity (matching vendors table in ER diagram)
        if ("ROLE_VENDOR".equals(requestedRole)) {
            Vendor vendor = new Vendor();
            vendor.setUser(savedUser);
            vendor.setBusinessName(request.businessName() != null && !request.businessName().isBlank()
                    ? request.businessName()
                    : savedUser.getFullName() + "'s Business");
            vendor.setStatus(VendorStatus.PENDING);
            vendor.setCommissionRate(new BigDecimal("10.00"));
            vendorRepository.save(vendor);
        }

        // If customer profile data provided, save customer_profiles entity
        if (request.dateOfBirth() != null) {
            CustomerProfile profile = new CustomerProfile(savedUser, request.dateOfBirth());
            customerProfileRepository.save(profile);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String jwtToken = jwtService.generateToken(userDetails, savedUser.getUserId());

        List<String> roleNames = savedUser.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        return new AuthResponse(
                jwtToken,
                "Bearer",
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                roleNames,
                jwtService.getExpirationTime()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails, user.getUserId());

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        return new AuthResponse(
                jwtToken,
                "Bearer",
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                roleNames,
                jwtService.getExpirationTime()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

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
