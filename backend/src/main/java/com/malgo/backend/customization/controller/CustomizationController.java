package com.malgo.backend.customization.controller;

import com.malgo.backend.customization.dto.CustomizationRequest;
import com.malgo.backend.customization.dto.CustomizationResponse;
import com.malgo.backend.customization.service.CustomizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customization")
public class CustomizationController {

    private final CustomizationService customizationService;

    @GetMapping("/me")
    public ResponseEntity<CustomizationResponse> getMyCustomization(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return ResponseEntity.ok(customizationService.getMyCustomization(memberId));
    }

    @PutMapping("/me")
    public ResponseEntity<CustomizationResponse> updateMyCustomization(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody CustomizationRequest request
    ) {
        return ResponseEntity.ok(customizationService.updateMyCustomization(memberId, request));
    }
}
