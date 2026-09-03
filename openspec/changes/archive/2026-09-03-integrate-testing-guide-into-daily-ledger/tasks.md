## 1. 搬移手冊並整合進 daily_ledger_system

- [x] 1.1 建立 `docs/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md`，將標題與前言區塊對齊 `01~08` 系列格式。
- [x] 1.2 移除根目錄之舊檔案 `docs/unit_testing_guide_and_test_catalog.md`，消除冗餘檔案。
- [x] 1.3 更新 `docs/daily_ledger_system/README.md` 文件導覽目錄，將單元測試手冊編號修正為 `09_unit_testing_guide_and_test_catalog.md`。

## 2. 驗證與回歸檢核

- [x] 2.1 檢查 `docs/daily_ledger_system/` 目錄中 01 至 09 文件之完整性與 Markdown 渲染連結。
- [x] 2.2 執行本地全套件測試 `.\mvnw.cmd test`，確認全數 54 個單元測試持續維持綠燈通過。
