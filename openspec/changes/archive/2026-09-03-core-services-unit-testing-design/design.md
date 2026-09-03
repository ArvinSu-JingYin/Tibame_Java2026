## Context

針對 `CategoryServiceImpl` 與 `LedgerServiceImpl` 建立單元測試。測試架構基於 JUnit 5 與 Mockito (`@ExtendWith(MockitoExtension.class)`)，無需啟動 Spring Context 或連接資料庫，依據 `docs/core_services_unit_testing_design.md` 規範的情境矩陣進行實作。

## Goals / Non-Goals

**Goals:**
- 完整實作 `CategoryServiceTest` 包含 16 個業務情境（分類查詢、自訂建立、唯一性衝突、系統分類保護、多租戶隔離、外鍵關聯刪除保護）。
- 完整實作 `LedgerServiceTest` 包含 18 個業務情境（收支記錄 CRUD、型別一致性防衛、智慧字串記帳、跨租戶隔離、Specification 動態分頁查詢、月度財務報表統計與 Null 安全結餘）。
- 採用 AAA (Arrange-Act-Assert) 模式，並以繁體中文 `@DisplayName` 明確定義測試意圖與驗證點。
- 修復 `CryptoServiceTest` 中 AEAD 防竄改測試的 Base64 邊界字元，確保全套件測試皆順暢通過。

**Non-Goals:**
- 不進行包含真實 H2 / MySQL 的 `@SpringBootTest` 整合測試（保持純單元測試毫秒級執行）。
- 不更動 Controller 與 Web API 層代碼。

## Decisions

1. **純單元測試與 Mockito 隔離**：
   - 決策：使用 `@Mock` 模擬 `CategoryRepository`, `AccountRecordRepository`, `SmartParserService`，並以 `@InjectMocks` 注入待測 Service。
   - 理由：避免依賴 Spring 容器啟動負擔，單元測試執行速度達毫秒級，便於 CI/CD 與本地即時驗證。
   - 替代方案：`@DataJpaTest` 或 `@SpringBootTest`，雖然能測試 DB 連動但啟動開銷較大，不適合高頻單元迴歸測試。

2. **多租戶水平越權防護驗證**：
   - 決策：對 `updateCategory`、`deleteCategory`、`updateRecord`、`deleteRecord`、`getRecordById` 嚴格測試 `userId` 不匹配時拋出 `ForbiddenException` 或 `ResourceNotFoundException`。

3. **收支類型強制一致性與報表結餘精確度**：
   - 決策：測試 `category.type == record.type` 衝突時拋出 `ApiException`；月度統計 null 時回傳 `BigDecimal.ZERO`，避免 NPE。

## Risks / Trade-offs

- **[Risk] Specification 條件比對複雜性** → 透過 Mock `accountRecordRepository.findAll(any(Specification.class), any(Pageable.class))` 回傳 Mock 分頁，確保測試著重於 Service 層轉換與富化邏輯。
- **[Risk] 浮點數精度問題** → 嚴格使用 `BigDecimal` 進行金額比對。
