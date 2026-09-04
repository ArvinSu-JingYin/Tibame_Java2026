## Why

在 GitHub Actions 託管 Runner (Hosted Runners) 淘汰 Node.js 20 執行期並強轉為 Node.js 24 的政策下，CI 工作流程日誌中持續出現 Deprecation 棄用警告。同時，`actions/setup-java@v4` 已被 GitHub 官方正式宣告生命週期結束 (End-of-Life) 並推薦遷移至原生支援 Node 24 的 `actions/setup-java@v5`。為杜絕技術債、確保持續整合管線長久穩健運行，並使 PR 門禁規格與最新 Active LTS 生態對齊，需進行此項升級變更。

## What Changes

- **升級主幹深度驗證工作流程之 Java 設置 Action**：將 `.github/workflows/ci-main.yml` 中之 `actions/setup-java@v4` 升級為 `actions/setup-java@v5`。
- **升級 PR 快速門禁工作流程之 Java 設置 Action**：將 `.github/workflows/ci-pr.yml` 中 `pr-unit-test` 任務之 `actions/setup-java@v4` 升級為 `actions/setup-java@v5`。
- **升級 PR 門禁 Node.js 執行環境**：將 `.github/workflows/ci-pr.yml` 中 `pr-compliance` 任務之 `node-version: 20` 升級為 `node-version: 22` (Active LTS)。
- **同步 OpenSpec CI 管線規格**：修改 `openspec/specs/ci-pipeline/spec.md`，將 OpenSpec 工具鏈校驗之 Node.js 執行期要求由 20 修訂為 22。
- **同步 CI 分支保護與維運手冊**：更新 `docs/guides/github-actions-ci-guide.md` 中的環境配置說明。

## Capabilities

### New Capabilities
<!-- 無新增 Capability -->

### Modified Capabilities
- `ci-pipeline`: 更新 Pull Request 門禁中 OpenSpec 規格校驗環境之 Node.js 執行期規範（由 Node.js 20 升級為 Node.js 22）。

## Impact

- **CI 工作流程**：`.github/workflows/ci-main.yml` 與 `.github/workflows/ci-pr.yml` 消除所有 Node 20 / setup-java v4 淘汰警示，確保與 GitHub 最新 Runner 架構無縫相容。
- **規範與文件**：`openspec/specs/ci-pipeline/spec.md` 與 `docs/guides/github-actions-ci-guide.md` 保持單一真實來源 (SSOT)。
- **相容性影響**：本次變更為無損替換 (Drop-in Replacement)，對 Java 後端代碼、Maven 建置生命週期、資料庫、前端頁面與使用者行為零破壞性影響。
