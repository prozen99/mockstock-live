package com.minsu.mockstocklive.auth.controller;

import com.minsu.mockstocklive.auth.dto.LoginRequest;
import com.minsu.mockstocklive.auth.dto.LoginResponse;
import com.minsu.mockstocklive.auth.dto.SignupRequest;
import com.minsu.mockstocklive.auth.dto.SignupResponse;
import com.minsu.mockstocklive.auth.service.AuthService;
import com.minsu.mockstocklive.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
