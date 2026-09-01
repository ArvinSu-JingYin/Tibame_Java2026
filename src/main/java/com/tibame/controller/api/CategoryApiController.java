package com.tibame.controller.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.common.security.UserContext;
import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.vo.CategoryResponseVo;
import com.tibame.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryApiController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponseVo>> getCategories(@RequestParam(required = false) String type) {
        Long userId = UserContext.requireUserId();
        List<CategoryResponseVo> list = categoryService.getCategories(userId, type);
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponseVo> getCategoryById(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        CategoryResponseVo vo = categoryService.getCategoryById(id, userId);
        return ApiResponse.ok(vo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponseVo> createCategory(@Valid @RequestBody CategoryCreateRequestDto requestDto) {
        Long userId = UserContext.requireUserId();
        CategoryResponseVo vo = categoryService.createCategory(userId, requestDto);
        return ApiResponse.ok("分類建立成功", vo);
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponseVo> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequestDto requestDto) {
        Long userId = UserContext.requireUserId();
        CategoryResponseVo vo = categoryService.updateCategory(id, userId, requestDto);
        return ApiResponse.ok("分類更新成功", vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        categoryService.deleteCategory(id, userId);
        return ApiResponse.ok("分類刪除成功", null);
    }
}
