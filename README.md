# 日常流水帳系統 (Daily Ledger System)

> **系統架構**：Spring Boot 3.3.13 + Java 21 + MS SQL Server / H2 In-Memory  
> **前端設計**：No-CDN 瑞士風格 (Swiss Style) 視覺設計系統 (Bootstrap 5.3 + Vue 3 MVVM + Axios)  
> **工程治理**：OpenSpec 規格驅動開發 + GitHub Actions 雙層 CI 品質門禁  

---

## 1. 專案簡介 (Overview)

「日常流水帳系統」是一套遵循瑞士風格極簡與功能主義美學的企業級記帳應用系統。系統以清楚的資訊階層、雙色調設計語言與響應式互動體驗，提供完整的收支記帳 CRUD、樹狀層級分類管理、多維度統計圖表及彈性身份認證機制。

---

## 2. 持續整合與品質守門 (CI/CD Pipeline)

本專案配置標準化 GitHub Actions 雙層分流持續整合管線：

```
+---------------------------------------------------------------------------------------------------+
|                                 GitHub Actions 雙層品質守門架構                                    |
+--------------------------+------------------------------------+-----------------------------------+
| 管線名稱                 | 觸發條件                           | 核心檢驗項目                      |
+--------------------------+------------------------------------+-----------------------------------+
| PR 快速門禁 (ci-pr.yml)  | PR opened, synchronize, reopened   | 1. Conventional Commits 繁中校驗  |
|                          | 至 main 分支                       | 2. OpenSpec validate 規格合法性   |
|                          |                                    | 3. H2 記憶體單元測試 (< 1 分鐘)   |
+--------------------------+------------------------------------+-----------------------------------+
| 主幹深度驗收 (ci-main.yml)| Push / Merge 至 main 分支          | 1. Playwright 瀏覽器快取機制      |
|                          |                                    | 2. mvn clean verify 全量 E2E 測試 |
|                          |                                    | 3. Spring Boot 可執行 JAR 打包    |
|                          |                                    | 4. 測試報告與產物歸檔 (保留 7 天) |
+--------------------------+------------------------------------+-----------------------------------+
```

- 📖 **詳細 CI 與分支保護指引**：請參閱 [docs/guides/github-actions-ci-guide.md](docs/guides/github-actions-ci-guide.md)。
- 📝 **提交規範**：`<type>(<scope>): <繁體中文簡述>`（例如：`feat(controller): 新增每日流水帳控制器`）。

---

## 3. 技術堆疊 (Technology Stack)

- **後端核心**：Java 21 (Temurin), Spring Boot 3.3.13
- **持久化層**：Spring Data JPA, Hibernate, MS SQL Server (正式/地端), H2 Database (離線測試/CI)
- **安全認證**：Spring Security Crypto (BCrypt), JJWT 0.12.6
- **前端介面**：Thymeleaf, Vue 3 (Composition API), Bootstrap 5.3, Axios, SweetAlert2 (100% 離線純本地封箱)
- **測試框架**：JUnit 5, Mockito, Microsoft Playwright for Java 1.46.0 (無頭/有頭 E2E 瀏覽器驗證)
- **規格治理**：OpenSpec CLI (`@fission-ai/openspec`)

---

## 4. 快速上手 (Quick Start)

### 4.1 環境需求
- JDK 21+
- Git
- PowerShell 5.1+ (Windows 環境)

### 4.2 本地啟動
使用專案內建智慧啟動腳本一鍵啟動：
```powershell
.\start.ps1 -Offline
```
*（預設以 H2 In-Memory 模式啟動，自動完成依賴下載與瀏覽器就緒檢查）*

### 4.3 執行測試
```powershell
# 1. 執行單元測試 (秒級快速測試)
./mvnw clean test

# 2. 執行全量測試 (包含 Playwright E2E 真機整合測試)
./mvnw clean verify
```

---

## 5. 文件門戶與治理導覽 (Documentation Portal)

專案採用「雙軌目錄架構」嚴格管理所有技術文檔與規格書：

- 📚 **文件總覽與治理門戶**：[docs/README.md](docs/README.md)
- 📋 **業務系統規格書庫**：[docs/specifications/daily_ledger_system/](docs/specifications/daily_ledger_system/)
- 🧪 **單元測試操作手冊與案例清單**：[docs/specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md](docs/specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md)
- 🎭 **Playwright E2E 測試操作手冊**：[docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md](docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md)
- 📐 **工程標準與代碼潔淨規範**：[docs/specifications/engineering_standards_and_code_cleanliness.md](docs/specifications/engineering_standards_and_code_cleanliness.md)
- 🛠️ **GitHub Actions CI 管線指引**：[docs/guides/github-actions-ci-guide.md](docs/guides/github-actions-ci-guide.md)
