package com.tibame.integration.service;

import com.tibame.common.exception.ConflictException;
import com.tibame.common.exception.ForbiddenException;
import com.tibame.common.exception.ResourceNotFoundException;
import com.tibame.integration.base.ServiceIntegrationTestBase;
import com.tibame.model.dto.CategoryCreateRequestDto;
import com.tibame.model.dto.CategoryUpdateRequestDto;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.model.entity.User;
import com.tibame.model.vo.CategoryResponseVo;
import com.tibame.service.CategoryService;
import com.tibame.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分類管理業務服務持久化整合測試
 * 驗證系統種子分類唯讀約束、多租戶自訂分類可見性隔離、外鍵關聯流水帳防刪防禦與同名衝突校驗
 */
@DisplayName("分類管理業務服務持久化整合測試 (CategoryServicePersistenceIT)")
class CategoryServicePersistenceIT extends ServiceIntegrationTestBase {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    @DisplayName("TC-CAT-IT-01: 系統種子分類唯讀保護 (不可更新、不可刪除，名稱順序完好)")
    void testSystemSeedCategoryImmutabilityProtection() {
        User user = createAndPersistTestUser();
        Category seedCategory = categoryRepository.findAvailableCategories(user.getId())
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsSystem()))
                .findFirst()
                .orElseThrow();

        String originalName = seedCategory.getName();
        Integer originalOrder = seedCategory.getSortOrder();

        // 嘗試更新系統種子分類 -> 預期拋出 ForbiddenException
        CategoryUpdateRequestDto updateDto = CategoryUpdateRequestDto.builder()
                .name("惡意修改種子分類")
                .sortOrder(1)
                .build();
        assertThatThrownBy(() -> categoryService.updateCategory(seedCategory.getId(), user.getId(), updateDto))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("系統內建分類不可修改");

        // 嘗試刪除系統種子分類 -> 預期拋出 ForbiddenException
        assertThatThrownBy(() -> categoryService.deleteCategory(seedCategory.getId(), user.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("系統內建分類不可刪除");

        // 重新從資料庫取得種子分類，確認屬性未被變更
        Category reloaded = categoryRepository.findById(seedCategory.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo(originalName);
        assertThat(reloaded.getSortOrder()).isEqualTo(originalOrder);
    }

    @Test
    @DisplayName("TC-CAT-IT-02: 多租戶自訂分類資料可見性隔離 (跨租戶不可見且不可直接存取)")
    void testMultiTenantCustomCategoryVisibilityIsolation() {
        User userA = createAndPersistTestUser();
        User userB = createAndPersistTestUser();

        // User A 建立自訂支出分類「攝影器材」
        CategoryResponseVo catA = categoryService.createCategory(userA.getId(), CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("攝影器材")
                .iconCode("camera")
                .sortOrder(55)
                .build());

        // User B 查詢可用分類清單
        List<CategoryResponseVo> userBCategories = categoryService.getCategories(userB.getId(), "EXPENSE");
        assertThat(userBCategories)
                .extracting(CategoryResponseVo::getName)
                .doesNotContain("攝影器材");

        // User B 嘗試以 ID 直接存取 User A 的自訂分類 -> 預期拋出 ResourceNotFoundException
        assertThatThrownBy(() -> categoryService.getCategoryById(catA.getId(), userB.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // User A 查詢可用分類清單 -> 應可正常看見自己的「攝影器材」
        List<CategoryResponseVo> userACategories = categoryService.getCategories(userA.getId(), "EXPENSE");
        assertThat(userACategories)
                .extracting(CategoryResponseVo::getName)
                .contains("攝影器材");
    }

    @Test
    @DisplayName("TC-CAT-IT-03: 關聯流水帳之自訂分類刪除防禦 (countByCategoryId > 0 拒絕刪除)")
    void testDeleteCategoryWithAssociatedRecordsPrevention() {
        User user = createAndPersistTestUser();

        // 建立自訂分類
        CategoryResponseVo customCat = categoryService.createCategory(user.getId(), CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("自訂辦公租金")
                .iconCode("building")
                .sortOrder(51)
                .build());

        // 建立一筆流水帳關聯至此分類
        ledgerService.createRecord(user.getId(), RecordCreateRequestDto.builder()
                .categoryId(customCat.getId())
                .recordType("EXPENSE")
                .amount(new BigDecimal("18000.00"))
                .description("五月份辦公室租金")
                .recordDate(LocalDate.of(2026, 5, 1))
                .build());

        // 嘗試刪除已被引用的自訂分類 -> 預期拋出 ConflictException
        assertThatThrownBy(() -> categoryService.deleteCategory(customCat.getId(), user.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("無法直接刪除");

        // 驗證資料庫中分類依然存在
        assertThat(categoryRepository.findById(customCat.getId())).isPresent();
    }

    @Test
    @DisplayName("TC-CAT-IT-04: 無關聯之自訂分類安全刪除 (完整生命週期驗證)")
    void testSafeDeleteUnassociatedCustomCategory() {
        User user = createAndPersistTestUser();

        CategoryResponseVo customCat = categoryService.createCategory(user.getId(), CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("臨時測試分類")
                .iconCode("tag")
                .sortOrder(90)
                .build());

        assertThat(categoryRepository.findById(customCat.getId())).isPresent();

        // 刪除無任何流水帳引用的自訂分類
        categoryService.deleteCategory(customCat.getId(), user.getId());

        // 強制實體同步與快取清理
        entityManager.flush();
        entityManager.clear();

        // 驗證自資料庫完全移除
        assertThat(categoryRepository.findById(customCat.getId())).isEmpty();
        assertThatThrownBy(() -> categoryService.getCategoryById(customCat.getId(), user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("TC-CAT-IT-05: 同一使用者同類型分類名稱重複衝突防禦 (existsByUserIdAndTypeAndName)")
    void testDuplicateCategoryNameRejectionWithinUserScope() {
        User user = createAndPersistTestUser();

        // 第一次建立「健身補劑」成功
        categoryService.createCategory(user.getId(), CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("健身補劑")
                .iconCode("capsule")
                .sortOrder(60)
                .build());

        // 第二次嘗試建立同類型同名分類 -> 預期拋出 ConflictException
        CategoryCreateRequestDto duplicateDto = CategoryCreateRequestDto.builder()
                .type("EXPENSE")
                .name("健身補劑")
                .iconCode("capsule")
                .sortOrder(61)
                .build();

        assertThatThrownBy(() -> categoryService.createCategory(user.getId(), duplicateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("您已建立過同類型的分類名稱");

        // 驗證該使用者底下僅有一筆「健身補劑」
        long count = categoryService.getCategories(user.getId(), "EXPENSE").stream()
                .filter(c -> "健身補劑".equals(c.getName()))
                .count();
        assertThat(count).isEqualTo(1L);
    }
}
