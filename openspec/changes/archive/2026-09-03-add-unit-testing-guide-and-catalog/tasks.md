## 1. 建立單元測試操作手冊與盤點清單

- [x] 1.1 建立 `docs/unit_testing_guide_and_test_catalog.md` 文件，完整納入 CLI 指令、AAA 撰寫範式、全專案 54 個單元測試案例目錄、四大安全防禦矩陣與新功能檢核清單。
- [x] 1.2 更新 `docs/daily_ledger_system/README.md` 文件導覽目錄，新增單元測試手冊之索引與說明條目。

## 2. 測試驗證與回歸確認

- [x] 2.1 執行本地全套件單元測試 `.\mvnw.cmd test`，確認全數 54 個測試案例在無 Spring Context 下皆順暢綠燈通過。
- [x] 2.2 驗證 `docs/unit_testing_guide_and_test_catalog.md` 內部文件結構、Markdown 表格渲染與相對檔案路徑連結有效性。
