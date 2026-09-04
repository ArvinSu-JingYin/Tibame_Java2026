## Why

現行設定檔中存在資料庫連線參數（`application-mssql.yml` 中的管理員帳號與預設密碼）與全域 JWT 簽署金鑰（`application.yml` 與 `JwtProperties.java`）明文硬編碼的資安風險，隨 Git 版本控制系統追蹤存在金鑰外洩與權杖偽造威脅。

為符合現代雲原生 12-Factor App 與正式生產環境資安合規規範（OWASP ASVS），本變更旨在落實「原始碼零機密原則 (Zero Secrets in VCS)」與「弱密碼拒絕啟動原則 (Fail-Fast on Missing Secrets)」，支援透過系統環境變數動態覆蓋機密憑證，並在生產環境啟動期實施防呆阻斷，同時為本機開發保留零設定阻力的預設值。

## What Changes

- **MS SQL 資料庫憑證環境變數化**：將 `src/main/resources/application-mssql.yml` 中的連線 URL、使用者名稱與密碼改為環境變數佔位符語法（`${DB_URL:...}`, `${DB_USERNAME:sa}`, `${DB_PASSWORD:1111}`），預設值維持現有本機開發設定。
- **JWT 簽署金鑰環境變數化**：將 `src/main/resources/application.yml` 中的 `jwt.secret` 與 `jwt.expiration-ms` 改為環境變數佔位符語法（`${JWT_SECRET:...}`, `${JWT_EXPIRATION_MS:86400000}`）。
- **啟動期安全驗證防線 (Fail-Fast Defense)**：於 `JwtProperties.java` 新增 `@PostConstruct` 安全性校驗方法：
  - 若啟用 `prod` profile 且偵測到仍在使用預設開發金鑰，立即拋出 `IllegalStateException` 中斷程序啟動。
  - 嚴格校驗金鑰字節長度，若未滿 32 位元組（256 位元）則立即拋出 `IllegalArgumentException` 中斷啟動。
- **自動化測試防護與回歸驗證**：新增針對 `JwtProperties` 安全驗證與環境變數解析的單元測試，並確保既有 `YamlConfigurationLintTest` 與全案測試維持 100% 綠燈。
- **正式環境部署安全實務指引**：在專案文檔中規範 Systemd、Docker Compose、Kubernetes 與 GitHub Actions 的安全環境變數注入 SOP（包括 `chmod 600` 私有環境檔機制）。

## Capabilities

### New Capabilities
None

### Modified Capabilities
- `cryptography-and-security`: 新增敏感憑證與 JWT 金鑰之環境變數動態覆蓋、生產環境弱密碼啟動攔截 (Fail-Fast) 及 HMAC-SHA256 密鑰最小長度校驗規格。

## Impact

- **受影響設定檔**：`src/main/resources/application-mssql.yml`、`src/main/resources/application.yml`。
- **受影響 Java 類別**：`com.tibame.config.JwtProperties`。
- **受影響測試**：新增 `JwtPropertiesSecurityTest`；既有測試與 `YamlConfigurationLintTest` 全數通過。
- **向下相容性**：完全相容。未設置環境變數的本機開發者與 CI 測試流程將透明回退至安全預設值，無須手動配置即可無痛運行。
