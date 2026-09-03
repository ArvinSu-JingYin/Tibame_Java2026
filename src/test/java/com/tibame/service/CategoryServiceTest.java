package com.tibame.service;

import com.tibame.common.exception.ConflictException;
import com.tibame.common.exception.ForbiddenException;
import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.model.vo.CategoryResponseVo;
import com.tibame.repository.AccountRecordRepository;
import com.tibame.repository.CategoryRepository;
import com.tibame.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRecordRepository accountRecordRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ==========================================
    // 1. getCategories 情境測試
    // ==========================================

    @Test
    @DisplayName("測試不帶 type 查詢全部可用分類 (系統內建 + 個人自訂)")
    void testGetCategoriesWithoutType_ReturnsAllAvailableCategories() {
        // Arrange
        Long userId = 1L;
        Category sysCat = Category.builder()
                .id(1L)
                .userId(null)
                .type("EXPENSE")
                .name("餐飲食品")
                .iconCode("food")
                .isSystem(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .build();
        Category userCat = Category.builder()
                .id(2L)
                .userId(userId)
                .type("INCOME")
                .name("兼職外快")
                .iconCode("cash")
                .isSystem(false)
                .sortOrder(10)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findAvailableCategories(userId)).thenReturn(List.of(sysCat, userCat));

        // Act
        List<CategoryResponseVo> result = categoryService.getCategories(userId, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("餐飲食品", result.get(0).getName());
        assertEquals("兼職外快", result.get(1).getName());
        verify(categoryRepository, times(1)).findAvailableCategories(userId);
        verify(categoryRepository, never()).findAvailableCategoriesByType(anyLong(), anyString());
    }

    @Test
    @DisplayName("測試帶 type 參數篩選指定收支類型分類")
    void testGetCategoriesWithType_ReturnsFilteredCategories() {
        // Arrange
        Long userId = 1L;
        String type = "EXPENSE";
        Category expenseCat = Category.builder()
                .id(1L)
                .type("EXPENSE")
                .name("餐飲食品")
                .iconCode("food")
                .isSystem(true)
                .sortOrder(1)
                .build();

        when(categoryRepository.findAvailableCategoriesByType(userId, "EXPENSE"))
                .thenReturn(List.of(expenseCat));

        // Act
        List<CategoryResponseVo> result = categoryService.getCategories(userId, type);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EXPENSE", result.get(0).getType());
        verify(categoryRepository, times(1)).findAvailableCategoriesByType(userId, "EXPENSE");
        verify(categoryRepository, never()).findAvailableCategories(anyLong());
    }

    // ==========================================
    // 2. getCategoryById 情境測試
    // ==========================================

    @Test
    @DisplayName("測試查詢存在且屬於當前使用者/系統之分類 ID")
    void testGetCategoryById_Success() {
        // Arrange
        Long categoryId = 5L;
        Long userId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type("EXPENSE")
                .name("娛樂日用")
                .iconCode("game")
                .isSystem(false)
                .sortOrder(5)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.of(category));

        // Act
        CategoryResponseVo vo = categoryService.getCategoryById(categoryId, userId);

        // Assert
        assertNotNull(vo);
        assertEquals(categoryId, vo.getId());
        assertEquals("娛樂日用", vo.getName());
        verify(categoryRepository, times(1)).findAvailableById(categoryId, userId);
    }

    @Test
    @DisplayName("測試查詢不存在或無權存取之分類 ID 應拋出 ResourceNotFoundException")
    void testGetCategoryById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long categoryId = 999L;
        Long userId = 1L;
        when(categoryRepository.findAvailableById(categoryId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(categoryId, userId));
        verify(categoryRepository, times(1)).findAvailableById(categoryId, userId);
    }

    // ==========================================
    // 3. createCategory 情境測試
    // ==========================================

    @Test
    @DisplayName("測試建立自訂分類 (使用預設 icon 與排序值)")
    void testCreateCategory_WithDefaults_Success() {
        // Arrange
        Long userId = 1L;
        CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("毛孩寵物")
                .iconCode(null)
                .sortOrder(null)
                .build();

        when(categoryRepository.existsByUserIdAndTypeAndName(userId, "EXPENSE", "毛孩寵物")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        // Act
        CategoryResponseVo result = categoryService.createCategory(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("毛孩寵物", result.getName());
        assertEquals("tag", result.getIconCode()); // 預設值 "tag"
        assertEquals(50, result.getSortOrder());   // 預設值 50
        assertFalse(result.getIsSystem());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("tag", captor.getValue().getIconCode());
        assertEquals(50, captor.getValue().getSortOrder());
    }

    @Test
    @DisplayName("測試建立自訂分類 (指定自訂 icon 與排序值)")
    void testCreateCategory_WithCustomValues_Success() {
        // Arrange
        Long userId = 1L;
        CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                .type("INCOME")
                .name("股票股利")
                .iconCode("trending-up")
                .sortOrder(1)
                .build();

        when(categoryRepository.existsByUserIdAndTypeAndName(userId, "INCOME", "股票股利")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });

        // Act
        CategoryResponseVo result = categoryService.createCategory(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals("trending-up", result.getIconCode());
        assertEquals(1, result.getSortOrder());
        assertEquals("INCOME", result.getType());
    }

    @Test
    @DisplayName("測試建立重複名稱同類型分類應拋出 ConflictException")
    void testCreateCategory_Duplicate_ThrowsConflictException() {
        // Arrange
        Long userId = 1L;
        CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("餐飲食品")
                .build();

        when(categoryRepository.existsByUserIdAndTypeAndName(userId, "EXPENSE", "餐飲食品")).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> categoryService.createCategory(userId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ==========================================
    // 4. updateCategory 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常更新自己的自訂分類")
    void testUpdateCategory_Success() {
        // Arrange
        Long categoryId = 15L;
        Long userId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type("EXPENSE")
                .name("舊名稱")
                .iconCode("old-icon")
                .isSystem(false)
                .sortOrder(10)
                .build();

        CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder()
                .name("新名稱")
                .iconCode("new-icon")
                .sortOrder(5)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryResponseVo result = categoryService.updateCategory(categoryId, userId, request);

        // Assert
        assertNotNull(result);
        assertEquals("新名稱", result.getName());
        assertEquals("new-icon", result.getIconCode());
        assertEquals(5, result.getSortOrder());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("測試嘗試修改系統內建分類 (isSystem=true) 應拋出 ForbiddenException")
    void testUpdateCategory_SystemCategory_ThrowsForbiddenException() {
        // Arrange
        Long categoryId = 1L;
        Long userId = 1L;
        Category sysCat = Category.builder()
                .id(categoryId)
                .userId(null)
                .type("EXPENSE")
                .name("系統分類")
                .isSystem(true)
                .build();

        CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder()
                .name("改動系統名稱")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sysCat));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> categoryService.updateCategory(categoryId, userId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("測試嘗試修改他人建立之自訂分類 (跨租戶越權) 應拋出 ForbiddenException")
    void testUpdateCategory_OtherUserCategory_ThrowsForbiddenException() {
        // Arrange
        Long categoryId = 20L;
        Long currentUserId = 1L;
        Long otherUserId = 2L;
        Category otherUserCat = Category.builder()
                .id(categoryId)
                .userId(otherUserId)
                .type("EXPENSE")
                .name("他人分類")
                .isSystem(false)
                .build();

        CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder()
                .name("惡意修改")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(otherUserCat));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> categoryService.updateCategory(categoryId, currentUserId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("測試更新不存在之分類 ID 應拋出 ResourceNotFoundException")
    void testUpdateCategory_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long categoryId = 999L;
        Long userId = 1L;
        CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder().name("新名稱").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(categoryId, userId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ==========================================
    // 5. deleteCategory 情境測試
    // ==========================================

    @Test
    @DisplayName("測試正常刪除無記帳記錄引用之自訂分類")
    void testDeleteCategory_Success() {
        // Arrange
        Long categoryId = 15L;
        Long userId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type("EXPENSE")
                .name("待刪分類")
                .isSystem(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(accountRecordRepository.countByCategoryId(categoryId)).thenReturn(0L);

        // Act
        categoryService.deleteCategory(categoryId, userId);

        // Assert
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("測試嘗試刪除已有記帳記錄引用之分類應拋出 ConflictException")
    void testDeleteCategory_WithLinkedRecords_ThrowsConflictException() {
        // Arrange
        Long categoryId = 15L;
        Long userId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type("EXPENSE")
                .name("熱門分類")
                .isSystem(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(accountRecordRepository.countByCategoryId(categoryId)).thenReturn(3L);

        // Act & Assert
        assertThrows(ConflictException.class, () -> categoryService.deleteCategory(categoryId, userId));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("測試嘗試刪除系統內建分類 (isSystem=true) 應拋出 ForbiddenException")
    void testDeleteCategory_SystemCategory_ThrowsForbiddenException() {
        // Arrange
        Long categoryId = 1L;
        Long userId = 1L;
        Category sysCat = Category.builder()
                .id(categoryId)
                .userId(null)
                .isSystem(true)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sysCat));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> categoryService.deleteCategory(categoryId, userId));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("測試嘗試刪除他人建立之自訂分類 (跨租戶越權) 應拋出 ForbiddenException")
    void testDeleteCategory_OtherUserCategory_ThrowsForbiddenException() {
        // Arrange
        Long categoryId = 25L;
        Long currentUserId = 1L;
        Long otherUserId = 2L;
        Category otherUserCat = Category.builder()
                .id(categoryId)
                .userId(otherUserId)
                .isSystem(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(otherUserCat));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> categoryService.deleteCategory(categoryId, currentUserId));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("測試刪除不存在之分類 ID 應拋出 ResourceNotFoundException")
    void testDeleteCategory_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long categoryId = 999L;
        Long userId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(categoryId, userId));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
