package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.request.user.CreateAddressRequest;
import com.techgadget.ecommerce.dto.response.user.AddressResponse;
import com.techgadget.ecommerce.dto.response.user.UserProfileResponse;
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
import java.util.stream.Collectors;

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

        // Create Set of Address Response
        Set<AddressResponse> addressResponses = user
                .getAddresses()
                .stream()
                .map(address -> new AddressResponse(
                        address.getId(),
                        address.getRecipientName(),
                        address.getPhoneNumber(),
                        address.getStreet(),
                        address.getCity(),
                        address.getProvince(),
                        address.getPostalCode(),
                        address.getNotes()
                )).collect(Collectors.toSet());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                addressResponses
        );
    }
}
