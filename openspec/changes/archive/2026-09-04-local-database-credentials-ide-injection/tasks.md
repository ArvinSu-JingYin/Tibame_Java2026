## 1. 版本控制防護與範本建立 (Git Configuration & Template Creation)

- [x] 1.1 檢核並更新 `.gitignore` 設定，維護 `.vscode/` 排除規則並允許 `!.vscode/*.example`，並使用 `git check-ignore` 驗證 `launch.json` 被忽略而 `launch.json.example` 可正常納入版控
- [x] 1.2 建立 `.vscode/launch.json.example` 標準執行組態範本檔，定義「本機 MSSQL (含自訂密碼)」與「H2 記憶體快速測試模式 (預設)」雙軌組態，並驗證其 JSON 格式合法性

## 2. 工程規格整合與文件指引 (Specification Integration & Documentation)

- [x] 2.1 建立或更新本機憑證注入規格指引（可參照探索報告整理至 `docs/specifications/` 或相關指南），明確載明 F5 一鍵偵錯與無感模式切換步驟
- [x] 2.2 更新專案核心文件總覽門戶 `docs/README.md`，納入本機開發憑證注入與 IDE 隔離機制的導覽索引與連結

## 3. 整合驗證與品質門禁 (Verification & Quality Gate)

- [x] 3.1 執行 `mvn test` 與靜態組態檢查，確保專案無編譯錯誤且既有測試套件 100% 綠燈通過
- [x] 3.2 模擬本機複製產生 `.vscode/launch.json`，執行 `git status` 驗證本地機密確實未受追蹤且 Git 工作區維持 100% 乾淨
