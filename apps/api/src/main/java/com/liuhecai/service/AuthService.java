package com.liuhecai.service;

import com.liuhecai.dto.LoginRequest;
import com.liuhecai.dto.UserRegisterRequest;
import com.liuhecai.vo.AuthProfileVO;
import com.liuhecai.vo.LoginVO;

public interface AuthService {
    LoginVO loginSuper(LoginRequest request);

    LoginVO loginAgent(LoginRequest request);

    LoginVO loginUser(LoginRequest request);

    LoginVO registerUser(UserRegisterRequest request);

    AuthProfileVO currentProfile();
}
