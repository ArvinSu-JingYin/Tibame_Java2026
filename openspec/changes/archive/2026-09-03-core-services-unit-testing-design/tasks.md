## 1. 密碼模組測試微調與基準驗證

- [x] 1.1 修正 `CryptoServiceTest.java` 之防竄改測試邊界字元，並執行 `./mvnw test -Dtest=CryptoServiceTest` 驗證通過

## 2. CategoryService 單元測試套件實作

- [x] 2.1 實作 `CategoryServiceTest.java` 查詢與建立分類共 7 個測試情境，並驗證通過
- [x] 2.2 實作 `CategoryServiceTest.java` 修改與刪除分類共 9 個測試情境（涵蓋系統分類唯讀與外鍵防護），並驗證通過

## 3. LedgerService 單元測試套件實作

- [x] 3.1 實作 `LedgerServiceTest.java` 建立、快速記帳與更新記帳等 9 個測試情境（涵蓋型別一致性與跨租戶防護），並驗證通過
- [x] 3.2 實作 `LedgerServiceTest.java` 刪除、單筆查詢、Specification 分頁查詢與月度財務報表統計等 9 個測試情境，並驗證通過

## 4. 全量單元測試執行與品質驗證

- [x] 4.1 執行全量 `./mvnw test`，確認專案全部 54 個測試案例通過且無錯誤
