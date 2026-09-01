package com.tibame.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordResponseVo {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private String categoryIconCode;
    private String recordType;
    private BigDecimal amount;
    private String description;
    private LocalDate recordDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
