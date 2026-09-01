package com.tibame.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVo {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserProfileVo user;
}
