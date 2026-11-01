package com.shopsphere.controller;

import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.user.AddressResponse;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Customer Addresses", description = "Endpoints for managing authenticated customer shipping/billing addresses")
@SecurityRequirement(name = "BearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get all addresses for authenticated customer")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<AddressResponse> addresses = addressService.getUserAddresses(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @PostMapping
    @Operation(summary = "Create a new address for authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse created = addressService.createAddress(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID for authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        AddressResponse address = addressService.getAddressById(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address for authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse updated = addressService.updateAddress(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address for authenticated customer")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        addressService.deleteAddress(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set address as default for authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        AddressResponse updated = addressService.setDefaultAddress(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully", updated));
    }
}
