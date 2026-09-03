## 1. IDE 工具配置與雜訊過濾

- [x] 1.1 於 `.vscode/settings.json` 配置 Java 存檔自動組織引用 (`editor.codeActionsOnSave` -> `source.organizeImports: always`) 與關閉 Spring Tools 版本驗證 (`boot-java.validation.java.version-validation: OFF`)，並驗證 JSON 語法格式正確
- [x] 1.2 檢視 `pom.xml` 確認 Spring Boot Starter Parent 版本為穩定修補版 `3.3.13`，並執行 `.\mvnw.cmd dependency:resolve` 驗證依賴解析正常

## 2. 代碼潔淨度重構與死碼清除

- [x] 2.1 移除 `UserRepository`、`CategoryRepository` 與 `AccountRecordRepository` 介面上多餘的 `@Repository` 標註及其 import，並執行 `.\mvnw.cmd test-compile` 驗證編譯通過
- [x] 2.2 重構 `RegexSmartParserServiceImpl` 中的正則表達式為類別靜態常數 `AMOUNT_PATTERN`，移除方法內部重複編譯與無用死碼欄位，並執行 `SmartParserServiceTest` / `LedgerServiceTest` 驗證解析功能 100% 綠燈
- [x] 2.3 清理 `AuthServiceTest`、`LedgerServiceTest` 與 `TokenServiceTest` 中多餘與未使用的 import 引用，並執行 `.\mvnw.cmd test` 驗證全套件 54 個單元測試全數綠燈通過

## 3. 開發規範與交付標準 (DoD) 固化

- [x] 3.1 於 `.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md` 增補 JpaRepository 介面原則、正則常數快取原則、測試類別可見性與「零警告檢核」清單，並檢視手冊結構一致性
- [x] 3.2 於 `openspec/config.yaml` 的 `operations.apply.guidance` 中增補代碼潔淨度與編譯警告零容忍規範，並透過 `openspec status` 驗證配置生效

## 4. 全專案編譯與品質驗證

- [x] 4.1 執行 `.\mvnw.cmd clean test` 驗證專案構建 BUILD SUCCESS 且全測試案例通過
- [x] 4.2 檢查 IDE Problems 面板確認無任何未排除之編譯錯誤（Error）與風格警告（Warning），達成零警告標準
