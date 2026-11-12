
package com.example.demo.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AddressRequest;
import com.example.demo.dto.AddressResponse;
import com.example.demo.entity.CustomerAddress;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CustomerAddressRepository;
import com.example.demo.repository.UserRepository;

@Service
public class AddressService {

    private final CustomerAddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(
            CustomerAddressRepository addressRepository,
            UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedCustomer() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof String)) {
            throw new IllegalArgumentException("Customer authentication required");
        }

        String username = (String) authentication.getPrincipal();

        User customer = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found"));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can manage addresses");
        }

        return customer;
    }

    private CustomerAddress getCustomerAddress(
            Integer addressId, Integer userId) {

        if (addressId == null || addressId <= 0) {
            throw new IllegalArgumentException("Invalid address ID");
        }

        return addressRepository
                .findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Address not found"));
    }

    private void applyAddressDetails(
            CustomerAddress address, AddressRequest request) {

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());
        address.setAddressType(request.getAddressType());
    }

    private void makeDefaultAddress(CustomerAddress address, Integer userId) {

        addressRepository
                .findByUser_UserIdAndDefaultAddressTrue(userId)
                .ifPresent(existingAddress -> {
                    if (!existingAddress.getAddressId()
                            .equals(address.getAddressId())) {
                        existingAddress.setDefaultAddress(false);
                    }
                });

        address.setDefaultAddress(true);
    }

    @Transactional
    public AddressResponse addAddress(AddressRequest request) {

        User customer = getAuthenticatedCustomer();

        CustomerAddress address = new CustomerAddress();

        address.setUser(customer);
        applyAddressDetails(address, request);

        boolean isFirstAddress =
                !addressRepository.existsByUser_UserId(customer.getUserId());

        boolean makeDefault = Boolean.TRUE.equals(request.getDefaultAddress())
                || isFirstAddress;

        if (makeDefault) {
            makeDefaultAddress(address, customer.getUserId());
        } else {
            address.setDefaultAddress(false);
        }

        CustomerAddress savedAddress = addressRepository.save(address);

        return AddressResponse.fromEntity(savedAddress);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses() {

        User customer = getAuthenticatedCustomer();

        return addressRepository
                .findByUser_UserIdOrderByCreatedAtDesc(customer.getUserId())
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Integer addressId) {

        User customer = getAuthenticatedCustomer();

        CustomerAddress address = getCustomerAddress(
                addressId, customer.getUserId());

        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public AddressResponse updateAddress(
            Integer addressId, AddressRequest request) {

        User customer = getAuthenticatedCustomer();

        CustomerAddress address = getCustomerAddress(
                addressId, customer.getUserId());

        applyAddressDetails(address, request);

        if (Boolean.TRUE.equals(request.getDefaultAddress())) {
            makeDefaultAddress(address, customer.getUserId());
        }

        CustomerAddress updatedAddress = addressRepository.save(address);

        return AddressResponse.fromEntity(updatedAddress);
    }

    @Transactional
    public void deleteAddress(Integer addressId) {

        User customer = getAuthenticatedCustomer();

        CustomerAddress address = getCustomerAddress(
                addressId, customer.getUserId());

        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());

        addressRepository.delete(address);

        if (wasDefault) {
            List<CustomerAddress> remainingAddresses =
                    addressRepository
                            .findByUser_UserIdOrderByCreatedAtDesc(
                                    customer.getUserId());

            if (!remainingAddresses.isEmpty()) {
                CustomerAddress newDefault = remainingAddresses.get(0);
                newDefault.setDefaultAddress(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public AddressResponse setDefaultAddress(Integer addressId) {

        User customer = getAuthenticatedCustomer();

        CustomerAddress address = getCustomerAddress(
                addressId, customer.getUserId());

        makeDefaultAddress(address, customer.getUserId());

        CustomerAddress updatedAddress = addressRepository.save(address);

        return AddressResponse.fromEntity(updatedAddress);
    }
}
