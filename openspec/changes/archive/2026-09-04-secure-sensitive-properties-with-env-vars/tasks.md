## 1. 配置檔敏感參數環境變數化

- [x] 1.1 重構 `src/main/resources/application-mssql.yml`，將 `url`、`username`、`password` 改為環境變數佔位符（`${DB_URL:...}`, `${DB_USERNAME:sa}`, `${DB_PASSWORD:1111}`）並保留開發預設值，透過語法檢查確認配置無誤
- [x] 1.2 重構 `src/main/resources/application.yml`，將 `jwt.secret` 與 `jwt.expiration-ms` 改為環境變數佔位符（`${JWT_SECRET:...}`, `${JWT_EXPIRATION_MS:86400000}`）並保留開發預設值，透過語法檢查確認配置無誤

## 2. 啟動期安全驗證防線實作

- [x] 2.1 改造 `com.tibame.config.JwtProperties`，注入 Spring `Environment` 並實作 `@PostConstruct public void validateSecurityPosture()`，於 `prod` profile 啟用且使用預設金鑰時拋出 `IllegalStateException`，金鑰長度未滿 32 位元組時拋出 `IllegalArgumentException`，透過代碼編譯確認無誤

## 3. 自動化測試防護與回歸驗證

- [x] 3.1 建立 `src/test/java/com/tibame/config/JwtPropertiesSecurityTest.java` 單元測試，涵蓋預設金鑰合格性、`prod` 環境弱金鑰阻斷、短金鑰拒絕及自訂金鑰正常載入等案例，執行 `mvn test -Dtest=JwtPropertiesSecurityTest` 確認 100% 通過
- [x] 3.2 執行 `mvn test -Dtest=YamlConfigurationLintTest`，驗證 YAML 佔位符改動完全符合現有鍵名跳脫規範
- [x] 3.3 執行全套測試 `mvn test`，確認全專案單元測試與 IDE Problems 面板維持零錯誤與零警告 (Zero-Warning DoD)

## 4. 正式環境部署資安指引與文件同步

- [x] 4.1 更新專案文件總覽與部署說明，記錄正式伺服器環境變數注入 SOP（包含 Linux Systemd `chmod 600` 環境檔、Docker Compose 與 Kubernetes Secrets 配置規範），並更新 `docs/README.md` 索引
