package com.tibame.service;

import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.vo.LoginResponseVo;
import com.tibame.model.vo.UserProfileVo;

public interface AuthService {
    UserProfileVo register(RegisterRequestDto requestDto);
    LoginResponseVo login(LoginRequestDto requestDto);
    UserProfileVo getCurrentUserProfile(Long userId);
}
