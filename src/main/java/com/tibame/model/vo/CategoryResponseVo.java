package com.tibame.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseVo {
    private Long id;
    private Long userId;
    private String type;
    private String name;
    private String iconCode;
    private Boolean isSystem;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
