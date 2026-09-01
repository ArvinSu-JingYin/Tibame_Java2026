package com.tibame.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "使用者名稱不得為空")
    @Size(min = 3, max = 50, message = "使用者名稱長度需介於 3 到 50 字元")
    private String username;

    @NotBlank(message = "密碼不得為空")
    @Size(min = 4, max = 100, message = "密碼長度需至少 4 字元")
    private String password;

    @NotBlank(message = "電子郵件不得為空")
    @Email(message = "電子郵件格式不正確")
    private String email;

    @Size(max = 50, message = "顯示名稱長度不得超過 50 字元")
    private String displayName;
}
