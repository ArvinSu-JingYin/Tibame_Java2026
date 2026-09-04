## Context

本架構設計基於 Spring Boot 3.3.13 原生屬性解析機制（`Environment`、Relaxed Binding 與屬性佔位符）。現有系統於 `application-mssql.yml` 與 `application.yml` 中直接記錄資料庫管理員帳密與 JWT 簽署金鑰。詳細背景與資安分析請參照 [proposal.md](proposal.md) 及探索文件 [database_credentials_and_jwt_secret_security_exploration.md](../../docs/explorations/database_credentials_and_jwt_secret_security_exploration.md)。

## Goals / Non-Goals

**Goals:**
- **去除原始碼硬編碼**：將資料庫連線參數與 JWT 簽署金鑰全數轉化為環境變數佔位符（`${VAR:default}`）。
- **零摩擦本機開發 (Zero-Friction Dev)**：保留合法且安全的開發預設值，確保本機 `mvn test`、IDE 啟動與 CI 管線無須額外設定即可立即開箱執行。
- **啟動期防呆阻斷 (Fail-Fast Defense)**：於 `JwtProperties` 建立啟動期驗證，在 `prod` profile 下嚴禁使用預設開發金鑰，並強制驗證金鑰長度不小於 256 位元（32 位元組）。
- **零外部依賴與零回歸**：不引進額外第三方套件（如 Jasypt、Vault），確保現有靜態檢查（`YamlConfigurationLintTest`）與全案測試 100% 綠燈通過。

**Non-Goals:**
- 不於本階段引入重型金鑰管理系統（如 HashiCorp Vault 或 AWS Secrets Manager）。
- 不實施執行期動態金鑰輪替（Dynamic Secret Rotation）機制。
- 不變更現有 JWT 簽名邏輯（`JwtTokenServiceImpl`）與資料庫驅動架構。

## Decisions

### 決策 1：採用 Spring Boot 原生屬性佔位符覆蓋機制（12-Factor 方案 A）
- **決定**：於 YAML 中採用 `${DB_URL:...}`、`${DB_USERNAME:sa}`、`${DB_PASSWORD:1111}` 與 `${JWT_SECRET:...}` 語法。
- **理由**：Spring Boot 原生支援進程環境變數（Environment Variables）、系統屬性（System Properties）與命令列參數對 YAML 的階層覆蓋，與 Linux Systemd（`EnvironmentFile`）、Docker Compose 及 Kubernetes Secrets 完美契合，且不增加 Maven 依賴與打包體積。
- **替代方案評估**：
  - *Jasypt 加密 (方案 B)*：需維護對稱解密主金鑰，且需額外引入第三方依賴，架構複雜度較高。
  - *私有配置檔隔離 (方案 C)*：需團隊成員手動複製範本檔，容易遺漏且不利容器化自動化部署。
  - *Vault 秘密系統 (方案 D)*：過度設計（Over-engineering），現階段單體系統維運成本過高。

### 決策 2：於 `JwtProperties` 透過 `@PostConstruct` 實施啟動期驗證
- **決定**：在 `@ConfigurationProperties` 類別 `JwtProperties` 注入 `Environment`，並新增 `@PostConstruct public void validateSecurityPosture()`。
- **理由**：
  1. 能在 Spring 容器啟動階段直接中斷程序（Fail-Fast），避免帶有弱密鑰的伺服器對外提供服務。
  2. 相比於在 `JwtTokenServiceImpl` 呼叫時才檢查，能在第一時間（應用啟動時）給予維運人員明確的中文錯誤診斷資訊。
  3. 可靈活判定當前 active profile 是否包含 `prod`，實現生產嚴格阻斷與本地開發寬容之雙態相容。
- **替代方案評估**：
  - *標準 JSR-380 Bean Validation*：無法依據 `Environment.getActiveProfiles()` 動態切換驗證邏輯（如僅在 `prod` 禁止預設值）。

### 決策 3：保留開發用預設回退值 (Sensible Fallbacks)
- **決定**：在 YAML 佔位符與 `JwtProperties.java` 中保留本地開發預設值（例如弱密碼 `'1111'`、預設 72 位元組 JWT 金鑰）。
- **理由**：維護「Clone 即跑」的極佳開發者體驗，避免新進團隊成員或本機測試因缺少環境變數而中斷。安全性則由決策 2 的生產環境啟動檢查進行雙重把關。

## Risks / Trade-offs

- **[風險] 維運人員於正式環境漏設環境變數**
  - **緩解措施**：`JwtProperties` 的 `@PostConstruct` 檢查在 `prod` profile 下若比對到預設值立即拋出 `IllegalStateException` 中斷程序，防止上線裸奔。
- **[風險] YAML 佔位符語法影響 `YamlConfigurationLintTest`**
  - **緩解措施**：佔位符格式為純字串純量（Scalar），不影響 Map key 的括號跳脫規範；實施後將以靜態 Lint 測試進行嚴格回歸驗證。
- **[風險] 開發金鑰長度不足風險**
  - **緩解措施**：預設金鑰 `SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026` 長度為 72 位元組（576 位元），遠高於 HMAC-SHA256 要求的 32 位元組（256 位元），安全長度校驗在本機與正式環境均能順暢通過。

## Migration Plan

1. **配置檔更新**：更新 `src/main/resources/application-mssql.yml` 與 `src/main/resources/application.yml` 引入佔位符。
2. **Java 類別改造**：在 `JwtProperties.java` 增加 `Environment` 依賴與 `@PostConstruct validateSecurityPosture()` 驗證邏輯。
3. **單元測試建立**：建立 `JwtPropertiesSecurityTest`，覆蓋預設載入、金鑰長度過短攔截、`prod` 環境弱金鑰拒絕啟動等測試場景。
4. **全套測試驗證**：執行 `mvn clean test` 驗證配置 Lint 與全單元測試無誤。
