package com.tibame.common.crypto.password.impl;

import com.tibame.common.crypto.password.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基於 BCrypt 演算法的密碼管理服務實作
 * 支援自定義強度 (Strength/Rounds) 與無感雜湊升級檢測
 */
@Slf4j
@Service
public class BCryptPasswordServiceImpl implements PasswordService {

    private static final String ALGORITHM_NAME = "BCrypt";
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");

    private final int strength;
    private final BCryptPasswordEncoder passwordEncoder;

    public BCryptPasswordServiceImpl(@Value("${crypto.password.bcrypt.strength:10}") int strength) {
        this.strength = Math.max(4, Math.min(31, strength));
        this.passwordEncoder = new BCryptPasswordEncoder(this.strength);
        log.info("初始化 BCrypt 密碼服務，預設 Cost Factor (Strength): {}", this.strength);
    }

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密碼不得為空");
        }
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    @Override
    public boolean needsUpgrade(String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return true;
        }
        Matcher matcher = BCRYPT_PATTERN.matcher(storedHash);
        if (!matcher.matches()) {
            // 非標準 BCrypt 格式，需升級
            return true;
        }
        try {
            int currentStrength = Integer.parseInt(matcher.group(1));
            return currentStrength < this.strength;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }
}
