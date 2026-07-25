package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.ChangePasswordRequest;
import com.liuhecai.dto.LoginRequest;
import com.liuhecai.service.AuthService;
import com.liuhecai.vo.AuthProfileVO;
import com.liuhecai.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentAuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.loginAgent(request));
    }

    @GetMapping("/me")
    public Result<AuthProfileVO> me() {
        return Result.ok(authService.currentProfile());
    }

    @PutMapping("/auth/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok(null);
    }
}
