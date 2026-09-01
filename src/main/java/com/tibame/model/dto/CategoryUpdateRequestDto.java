package com.tibame.model.dto;

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
public class CategoryUpdateRequestDto {

    @NotBlank(message = "分類名稱不得為空")
    @Size(min = 1, max = 50, message = "分類名稱長度需介於 1 到 50 字元")
    private String name;

    @Size(max = 30, message = "圖標代碼長度不得超過 30 字元")
    private String iconCode;

    private Integer sortOrder;
}
