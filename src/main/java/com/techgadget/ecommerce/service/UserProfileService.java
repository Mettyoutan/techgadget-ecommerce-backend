package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.CreateAddressRequest;
import com.techgadget.ecommerce.dto.response.AddressResponse;
import com.techgadget.ecommerce.dto.response.UserProfileResponse;
import com.techgadget.ecommerce.entity.Address;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.exception.NotFoundException;
import com.techgadget.ecommerce.repository.AddressRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {

        log.debug("Processing get user profile - User: {}", userId);

        User user = userRepository.findByIdWithAddress(userId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", userId);
                    return new NotFoundException("User not found.");
                });

        log.info("Successfully retrieved user profile - User: {}", userId);

        return mapToUserProfile(user);
    }

    @Transactional
    public UserProfileResponse addAddress(Long userId, CreateAddressRequest request) {

        log.debug("Processing add address request - User: {}", userId);

        User user = userRepository.findByIdWithAddress(userId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", userId);
                    return new NotFoundException("User not found.");
                });

        // Create new address
        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setProvince(request.getProvince());
        address.setPostalCode(request.getPostalCode());
        address.setNotes(request.getNotes());

        // Add to user addresses
        user.addAddress(address);
        userRepository.save(user);

        log.info("User {} successfully added address {}", userId, address.getId());

        return mapToUserProfile(user);
    }

    private UserProfileResponse mapToUserProfile(User user) {

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setFullName(user.getFullName());

        // Create Address Response DTO
        Set<AddressResponse> addresses = new HashSet<>();

        for (Address address : user.getAddresses()) {
            AddressResponse ar = new AddressResponse();
            ar.setId(address.getId());
            ar.setRecipientName(address.getRecipientName());
            ar.setPhoneNumber(address.getPhoneNumber());
            ar.setStreet(address.getStreet());
            ar.setCity(address.getCity());
            ar.setProvince(address.getProvince());
            ar.setPostalCode(address.getPostalCode());
            ar.setNotes(address.getNotes());

            addresses.add(ar);
        }

        response.setAddresses(addresses);

        return response;
    }
}
