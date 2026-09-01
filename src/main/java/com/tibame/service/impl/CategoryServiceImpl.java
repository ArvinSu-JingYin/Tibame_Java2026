package com.tibame.service.impl;

import com.tibame.common.exception.ConflictException;
import com.tibame.common.exception.ForbiddenException;
import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.model.vo.CategoryResponseVo;
import com.tibame.repository.AccountRecordRepository;
import com.tibame.repository.CategoryRepository;
import com.tibame.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AccountRecordRepository accountRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseVo> getCategories(Long userId, String type) {
        List<Category> categories;
        if (StringUtils.hasText(type)) {
            categories = categoryRepository.findAvailableCategoriesByType(userId, type.toUpperCase());
        } else {
            categories = categoryRepository.findAvailableCategories(userId);
        }
        return categories.stream().map(this::convertToVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseVo getCategoryById(Long id, Long userId) {
        Category category = categoryRepository.findAvailableById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到分類資料 (ID: " + id + ")"));
        return convertToVo(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponseVo createCategory(Long userId, CategoryCreateRequestDto requestDto) {
        String type = requestDto.getType().toUpperCase();
        if (categoryRepository.existsByUserIdAndTypeAndName(userId, type, requestDto.getName())) {
            throw new ConflictException("您已建立過同類型的分類名稱: " + requestDto.getName());
        }

        Category category = Category.builder()
                .userId(userId)
                .type(type)
                .name(requestDto.getName())
                .iconCode(requestDto.getIconCode() != null ? requestDto.getIconCode() : "tag")
                .isSystem(false)
                .sortOrder(requestDto.getSortOrder() != null ? requestDto.getSortOrder() : 50)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("成功建立自訂分類: id={}, userId={}, name={}", saved.getId(), userId, saved.getName());
        return convertToVo(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponseVo updateCategory(Long id, Long userId, CategoryUpdateRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到分類資料 (ID: " + id + ")"));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw new ForbiddenException("系統內建分類不可修改");
        }

        if (!userId.equals(category.getUserId())) {
            throw new ForbiddenException("無權修改他人建立之自訂分類");
        }

        category.setName(requestDto.getName());
        if (requestDto.getIconCode() != null) {
            category.setIconCode(requestDto.getIconCode());
        }
        if (requestDto.getSortOrder() != null) {
            category.setSortOrder(requestDto.getSortOrder());
        }

        Category updated = categoryRepository.save(category);
        log.info("成功更新自訂分類: id={}, name={}", updated.getId(), updated.getName());
        return convertToVo(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到分類資料 (ID: " + id + ")"));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw new ForbiddenException("系統內建分類不可刪除");
        }

        if (!userId.equals(category.getUserId())) {
            throw new ForbiddenException("無權刪除他人建立之自訂分類");
        }

        long linkedRecordCount = accountRecordRepository.countByCategoryId(id);
        if (linkedRecordCount > 0) {
            throw new ConflictException("此分類已被 " + linkedRecordCount + " 筆記帳記錄引用，無法直接刪除");
        }

        categoryRepository.delete(category);
        log.info("成功刪除自訂分類: id={}, userId={}", id, userId);
    }

    private CategoryResponseVo convertToVo(Category category) {
        return CategoryResponseVo.builder()
                .id(category.getId())
                .userId(category.getUserId())
                .type(category.getType())
                .name(category.getName())
                .iconCode(category.getIconCode())
                .isSystem(category.getIsSystem())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
