package com.tibame.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordCreateRequestDto {

    @NotNull(message = "分類 ID 不得為空")
    private Long categoryId;

    @NotBlank(message = "記帳類型不得為空")
    @Pattern(regexp = "^(EXPENSE|INCOME)$", message = "記帳類型必須為 EXPENSE 或 INCOME")
    private String recordType;

    @NotNull(message = "金額不得為空")
    @DecimalMin(value = "0.01", message = "金額必須大於 0")
    @Digits(integer = 10, fraction = 2, message = "金額整數位最多 10 位，小數位最多 2 位")
    private BigDecimal amount;

    @Size(max = 200, message = "備註長度不得超過 200 字元")
    private String description;

    @NotNull(message = "記帳日期不得為空")
    private LocalDate recordDate;
}
