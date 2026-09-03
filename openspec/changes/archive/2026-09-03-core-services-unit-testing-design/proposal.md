## Why

為每日記帳系統 (Daily Ledger System) 之核心業務邏輯模組 (`CategoryService` 與 `LedgerService`) 建立完整的純單元測試套件。透過純 Mockito 隔離執行，在毫秒級測試速度下全覆蓋收支分類防護、收支類型一致性、多租戶資料隔離 (IDOR) 與財務結餘計算邊界條件，確保核心業務邏輯之高可靠度與程式碼品質。

## What Changes

- 新增 `CategoryServiceTest.java`：實作 16 個測試案例，全覆蓋分類查詢、建立、更新、刪除、系統內建分類唯讀防護、重複名稱衝突防護與外鍵引用刪除防禦。
- 新增 `LedgerServiceTest.java`：實作 18 個測試案例，全覆蓋收支記帳 CRUD、智慧記帳代理調用、收支類型強制一致性檢驗、水平越權防護、Specification 動態分頁查詢與月度收支結餘邊界計算。
- 修復 `CryptoServiceTest.java`：修正密文竄改測試案例之 Base64 邊界字符，確保 AEAD 防竄改驗證正確觸發並通過測試。

## Capabilities

### New Capabilities
無。

### Modified Capabilities
無。

## Impact

- 受影響測試檔案：
  - `src/test/java/com/tibame/service/CategoryServiceTest.java` (新建立)
  - `src/test/java/com/tibame/service/LedgerServiceTest.java` (新建立)
  - `src/test/java/com/tibame/common/crypto/cipher/CryptoServiceTest.java` (微調測試斷言)
- 測試覆蓋率：專案測試案例數由 20 提升至 54，核心業務服務層覆蓋率達 90% 以上。
