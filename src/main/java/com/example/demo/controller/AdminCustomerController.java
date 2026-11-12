package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CustomerDetailsResponse;
import com.example.demo.dto.CustomerResponse;
import com.example.demo.service.AdminCustomerService;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(
            AdminCustomerService adminCustomerService) {

        this.adminCustomerService =
                adminCustomerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>>
            getAllCustomers() {

        List<CustomerResponse> response =
                adminCustomerService.getAllCustomers();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CustomerDetailsResponse>
            getCustomerById(
                    @PathVariable Integer userId) {

        CustomerDetailsResponse response =
                adminCustomerService.getCustomerById(
                        userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<CustomerResponse>
            deactivateCustomer(
                    @PathVariable Integer userId) {

        CustomerResponse response =
                adminCustomerService
                        .deactivateCustomer(userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<CustomerResponse>
            activateCustomer(
                    @PathVariable Integer userId) {

        CustomerResponse response =
                adminCustomerService
                        .activateCustomer(userId);

        return ResponseEntity.ok(response);
    }
}