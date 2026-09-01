package com.tibame.service.impl;

import com.tibame.common.exception.ConflictException;
import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.common.exception.UnauthorizedException;
import com.tibame.common.security.TokenService;
import com.tibame.model.dto.LoginRequestDto;
import com.tibame.model.dto.RegisterRequestDto;
import com.tibame.model.entity.User;
import com.tibame.model.vo.LoginResponseVo;
import com.tibame.model.vo.UserProfileVo;
import com.tibame.repository.UserRepository;
import com.tibame.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo register(RegisterRequestDto requestDto) {
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new ConflictException("該帳號名稱已存在: " + requestDto.getUsername());
        }
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new ConflictException("該電子郵件已被註冊: " + requestDto.getEmail());
        }

        User user = User.builder()
                .username(requestDto.getUsername())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .email(requestDto.getEmail())
                .displayName(requestDto.getDisplayName() != null && !requestDto.getDisplayName().isBlank()
                        ? requestDto.getDisplayName()
                        : requestDto.getUsername())
                .build();

        User saved = userRepository.save(user);
        log.info("新用戶註冊成功: id={}, username={}", saved.getId(), saved.getUsername());
        return convertToProfileVo(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseVo login(LoginRequestDto requestDto) {
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new UnauthorizedException("帳號或密碼不正確"));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("帳號或密碼不正確");
        }

        String token = tokenService.generateToken(user.getId(), user.getUsername());
        log.info("用戶登入成功: id={}, username={}", user.getId(), user.getUsername());

        return LoginResponseVo.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .user(convertToProfileVo(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVo getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者資料 (ID: " + userId + ")"));
        return convertToProfileVo(user);
    }

    private UserProfileVo convertToProfileVo(User user) {
        return UserProfileVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
