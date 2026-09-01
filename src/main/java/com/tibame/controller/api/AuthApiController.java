package com.tibame.controller.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.common.security.UserContext;
import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.vo.LoginResponseVo;
import com.tibame.model.vo.UserProfileVo;
import com.tibame.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileVo> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        UserProfileVo vo = authService.register(requestDto);
        return ApiResponse.ok("註冊成功", vo);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseVo> login(@Valid @RequestBody LoginRequestDto requestDto) {
        LoginResponseVo vo = authService.login(requestDto);
        return ApiResponse.ok("登入成功", vo);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileVo> getCurrentUser() {
        Long currentUserId = UserContext.requireUserId();
        UserProfileVo vo = authService.getCurrentUserProfile(currentUserId);
        return ApiResponse.ok(vo);
    }
}
