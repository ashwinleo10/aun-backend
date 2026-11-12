
package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddressRequest;
import com.example.demo.dto.AddressResponse;
import com.example.demo.service.AddressService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.addAddress(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {

        List<AddressResponse> response =
                addressService.getAllAddresses();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable Integer addressId) {

        AddressResponse response =
                addressService.getAddressById(addressId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Integer addressId,
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response =
                addressService.updateAddress(addressId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Integer addressId) {

        addressService.deleteAddress(addressId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable Integer addressId) {

        AddressResponse response =
                addressService.setDefaultAddress(addressId);

        return ResponseEntity.ok(response);
    }
}
