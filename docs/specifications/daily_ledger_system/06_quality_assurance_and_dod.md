# 6. 品質保證與驗收標準 (Quality Assurance & Definition of Done)

> **專案代號**：`daily-ledger-system`  
> **所屬模組**：品質保證、驗收標準與交付矩陣  
> **狀態**：規劃產物完整審查通過 (`Planning Complete`, `Strict Validated`)

---

## 1. 交付定義驗收矩陣 (DoD Checklist)

```mermaid
checklist
    title 交付定義驗收矩陣 (DoD Matrix)
    - [x] 後端四層嚴格解耦 (Controller ➔ Service ➔ Repository ➔ DB)
    - [x] 所有密碼採用 BCrypt 雜湊加密
    - [x] 所有 API 使用統一 ApiResponse 包裝
    - [x] 跨使用者數據 100% 嚴格隔離 (No IDOR)
    - [x] 靜態資產 Strict No-CDN (100% 純離線載入)
    - [x] 視覺設計 100% 遵循瑞士國際主義風格
    - [x] 金額運算一律採用 BigDecimal 防止精度遺失
    - [x] 代碼潔淨零警告 (Zero-Warning) 檢驗通過
    - [x] OpenSpec 嚴格模式檢驗通過 (Strict Validated)
```

---

## 2. 核心檢驗規約

1. **後端四層架構解耦**：
   - 控制器層不得包含業務邏輯與 JPA 查詢。
   - 服務層需明確宣告 `@Transactional`，並處理商業例外。
   - 數據訪問層僅負責資料庫存取，所有查詢強制綁定當前 `user_id`。

2. **安全與數據隔離**：
   - 密碼不得明文儲存，一律經 BCrypt 加密。
   - 請求進入時經 `JwtAuthenticationFilter` 解析並寫入 `CurrentUserContext` (ThreadLocal)。
   - 確保在 `finally` 區塊強制調用 `CurrentUserContext.clear()`，避免執行緒池複用污染。

3. **前端純離線與瑞士風格**：
   - 禁用任何 `https://cdn...` 外鏈資源，所有 JS、CSS、字體一律從 `/lib/` 本機靜態路徑載入。
   - 介面採用瑞士國際主義風格：直角銳利框線 (`0px`)、瑞士紅 (`#DC2626`)、黑白高對比及結構化編號索引。

4. **數值與金額精度**：
   - 所有金額在後端 Java 中必須使用 `BigDecimal` 處理，資料庫使用 `DECIMAL(12, 2)`，避免浮點數精度偏差。

5. **代碼潔淨與零警告規約 (Zero-Warning Standard)**：
   - 繼承 `JpaRepository` 之介面一律不得標註多餘的 `@Repository`。
   - 所有正規表達式 `Pattern` 必須宣告為 `private static final` 類別常數快取，嚴禁於方法內部重複編譯或殘留孤兒死碼欄位。
   - 全模組必須 100% 遵循 [專案通用工程標準、代碼潔淨與 IDE 排除指南](../engineering_standards_and_code_cleanliness.md)，確保 IDE Problems 面板維持 0 錯誤、0 警告。

---

## 3. 實施推進指南

本系統所有變更已在 `openspec/changes/daily-ledger-system/` 完成嚴格檢驗。在進入編碼階段時，可直接使用以下指令推進工作：

- **啟動開發實作**：使用 `/opsx-apply` 依據任務清單逐項實作程式碼。
- **規格同步**：若實作過程有規格演進，使用 `/opsx-sync` 同步至主規格。
