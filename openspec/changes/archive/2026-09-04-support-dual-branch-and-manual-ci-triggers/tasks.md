## 1. GitHub Actions CI 工作流程重構

- [x] 1.1 更新 `.github/workflows/ci-main.yml` 觸發條件，將 `push.branches` 擴展為 `[main, dev]` 並新增 `workflow_dispatch:` 手動派發支援，透過 YAML 語法檢驗確認結構無誤
- [x] 1.2 更新 `.github/workflows/ci-pr.yml` 觸發條件，將 `pull_request.branches` 擴展為 `[main, dev]` 並新增 `workflow_dispatch:` 手動派發支援，透過 YAML 語法檢驗確認結構無誤
- [x] 1.3 重構 `.github/workflows/ci-pr.yml` 中 `pr-compliance` 任務的 Node.js 檢驗邏輯，注入 `EVENT_NAME: ${{ github.event_name }}` 並新增事件判斷分支（非 PR 事件略過 PR 標題檢查），透過本地 Node.js 腳本模擬測試驗證 PR 與手動派發兩種情境皆能正確執行

## 2. CI 治理文件與規格同步

- [x] 2.1 更新 `docs/guides/github-actions-ci-guide.md`，同步雙分支矩陣架構圖、觸發事件表格與手動 `workflow_dispatch` 派發指南，檢查文件格式與錨點鏈結完整
- [x] 2.2 更新 `docs/README.md` 全域文件索引，同步 GitHub Actions CI 相關描述與最新探索報告索引連結

## 3. 全域規格與管線驗證

- [x] 3.1 執行 `openspec validate --all`，確認所有規格定義檔與本次變更提案之語法均符合規範且通過校驗
- [x] 3.2 執行 `./mvnw clean test --no-transfer-progress`，驗證本機端快速單元測試守門機制運作正常且全數通過
