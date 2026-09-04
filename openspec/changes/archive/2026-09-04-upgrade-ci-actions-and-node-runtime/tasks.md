## 1. 工作流程配置升級 (Workflow Updates)

- [x] 1.1 升級 `.github/workflows/ci-main.yml` 中之 `actions/setup-java` 至 `@v5`，並驗證參數相容性與 YAML 語法無誤
- [x] 1.2 升級 `.github/workflows/ci-pr.yml` 中 `pr-compliance` 任務之 `node-version` 為 `22` (Active LTS)，並驗證 YAML 語法無誤
- [x] 1.3 升級 `.github/workflows/ci-pr.yml` 中 `pr-unit-test` 任務之 `actions/setup-java` 至 `@v5`，並驗證參數相容性與 YAML 語法無誤

## 2. 治理文件與指引同步 (Governance & Documentation)

- [x] 2.1 更新 `docs/guides/github-actions-ci-guide.md`，同步 PR 門禁 Node.js 22 LTS 與 setup-java@v5 說明
- [x] 2.2 檢核 OpenSpec CI 管線規格與變更描述之一致性

## 3. 整合與規格驗收門禁 (Verification Gate)

- [x] 3.1 執行 `openspec validate --all` 驗證所有規格與變更產物語法合規，退出碼為 0
- [x] 3.2 執行 `./mvnw clean test --no-transfer-progress` 驗證 Java 單元測試與配置檢查全數通過
