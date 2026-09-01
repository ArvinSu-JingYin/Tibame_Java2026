package com.tibame.service;

import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.vo.CategoryResponseVo;

import java.util.List;

public interface CategoryService {
    List<CategoryResponseVo> getCategories(Long userId, String type);
    CategoryResponseVo getCategoryById(Long id, Long userId);
    CategoryResponseVo createCategory(Long userId, CategoryCreateRequestDto requestDto);
    CategoryResponseVo updateCategory(Long id, Long userId, CategoryUpdateRequestDto requestDto);
    void deleteCategory(Long id, Long userId);
}
