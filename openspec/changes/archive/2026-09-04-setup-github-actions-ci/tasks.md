## 1. CI PR 快速門禁管線實作 (PR Gatekeeper Pipeline)

- [x] 1.1 建立 `.github/workflows/ci-pr.yml` 工作流程骨架，定義 PR 觸發條件（opened, synchronize, reopened, edited 至 main），並驗證 YAML 語法合法
- [x] 1.2 在 `ci-pr.yml` 實作 `pr-compliance` 任務之 PR 標題與提交歷史 Conventional Commits、Scope 白名單及繁體中文正規檢驗腳本，並驗證阻擋與放行邏輯
- [x] 1.3 在 `ci-pr.yml` 的 `pr-compliance` 任務中整合 Node.js 20 與 `openspec validate --all` 自動化規格一致性檢核步驟
- [x] 1.4 在 `ci-pr.yml` 實作 `pr-unit-test` 任務，配置 Java 21、Maven 依賴快取及 `./mvnw clean test --no-transfer-progress` 秒級單元測試守門

## 2. 主分支深度整合與回歸管線實作 (Main Deep Verification Pipeline)

- [x] 2.1 建立 `.github/workflows/ci-main.yml` 工作流程骨架，定義 main 分支 push 觸發條件，並驗證 YAML 語法合法
- [x] 2.2 在 `ci-main.yml` 配置 Java 21、Maven 依賴快取與 Playwright 瀏覽器二進位檔快取（`~/.cache/ms-playwright`）機制
- [x] 2.3 在 `ci-main.yml` 實作 Playwright 缺失回退安裝步驟（`install --with-deps chromium`）與全量驗證執行（`./mvnw clean verify --no-transfer-progress`），確保單元、整合與 Playwright E2E 測試完整執行
- [x] 2.4 在 `ci-main.yml` 實作 Spring Boot 可執行 JAR 打包步驟（`./mvnw package -DskipTests --no-transfer-progress`）
- [x] 2.5 在 `ci-main.yml` 配置測試報告與建置產物自動收集上傳（`actions/upload-artifact@v4`，保存 Surefire、Failsafe 報告 7 天）

## 3. CI 治理與分支保護指引文檔 (CI Governance Documentation)

- [x] 3.1 建立專案 CI 與分支保護指引文檔 `docs/guides/github-actions-ci-guide.md`，詳述雙層架構、本地除錯步驟與 GitHub Branch Protection Rules 規範配置
- [x] 3.2 於專案首頁 `README.md` 與文件總覽門戶 `docs/README.md` 更新 CI 管線章節與指引連結

## 4. 全量驗證與 OpenSpec 規格合規確認 (Verification & Compliance)

- [x] 4.1 執行 `openspec validate --all` 驗證全專案 specifications、change proposal 與 tasks 之一致性與合法性
- [x] 4.2 執行本地 `./mvnw clean test` 確保單元測試與配置驗證 100% 通過（符合 Zero-Warning 品質門禁）
