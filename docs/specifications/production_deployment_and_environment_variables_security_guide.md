# 日常流水帳系統 — 正式伺服器環境變數安全注入與部署維運規格手冊 (Production Deployment & Environment Variables Security Guide)

> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **系統名稱**：日常流水帳系統 (Daily Ledger System)  
> **資安規範**：OWASP ASVS & 雲原生 12-Factor App III. Config 原則  
> **核心政策**：原始碼零機密原則 (Zero Secrets in VCS) & 弱密碼拒絕啟動原則 (Fail-Fast Defense)  

---

## 1. 概述與資安治理原則 (Overview & Security Governance)

為杜絕資料庫存取帳密與 JWT 簽署金鑰在版本控制系統（Git）中硬編碼外洩之風險，日常流水帳系統全面導入「環境變數動態覆蓋」與「啟動期資安防線 (Fail-Fast)」雙重防護機制：

1. **原始碼零機密 (Zero Secrets in VCS)**：
   - 原始碼與 YAML 設定檔（`application*.yml`）中僅保留本機開發與測試用的安全預設值，不包含任何正式環境之連線主機、帳號、密碼或生產私鑰。
   - 所有正式環境之機密憑證必須由底層雲端基礎設施或維運主機在行程啟動時動態注入。
2. **弱密碼啟動攔截 (Fail-Fast on Missing Secrets)**：
   - 當系統以 `--spring.profiles.active=prod` 或環境變數 `SPRING_PROFILES_ACTIVE=prod` 啟動時，`JwtProperties` 類別之 `@PostConstruct validateSecurityPosture()` 方法會主動校驗當前金鑰。
   - 若偵測到仍在使用開發預設金鑰（`DEFAULT_DEV_SECRET`），將立即拋出 `IllegalStateException` 中斷行程啟動，避免帶有預設金鑰的服務上線對外提供服務。
   - 若金鑰字節長度未達 HMAC-SHA256 規定之 32 位元組（256 位元），立即拋出 `IllegalArgumentException` 阻斷程序。

---

## 2. 核心敏感環境變數規範 (Sensitive Environment Variables)

| 環境變數名稱 | 適用 Profile | 正式環境必要性 | 規範與安全要求 | 範例值 |
| :--- | :--- | :--- | :--- | :--- |
| `DB_URL` | `prod`, `mssql` | 建議注入 | 正式 MS SQL Server JDBC 連線字串，必須啟用傳輸層加密 | `jdbc:sqlserver://db.internal:1433;databaseName=tibame_account;encrypt=true;trustServerCertificate=false;` |
| `DB_USERNAME` | `prod`, `mssql` | 建議注入 (預設 `sa`) | 正式資料庫連線帳號，建議依最小權限原則分配專用帳號 | `daily_ledger_app_user` |
| `DB_PASSWORD` | `prod`, `mssql` | **強制必填** | 高強度資料庫密碼，至少 16 字元以上含大小寫英數與符號 | `P@ssw0rd2026!#SecureCorp` |
| `JWT_SECRET` | 全環境 (以 `prod` 最嚴格) | **強制必填** (`prod`) | HMAC-SHA256 簽署金鑰，長度**至少 32 位元組 (256 位元)**，在 `prod` Profile 嚴禁使用預設值 | `SwissLedgerEnterpriseProdJwtSigningKey2026!#UltraSecureEntropyKey` |
| `JWT_EXPIRATION_MS`| 全環境 | 選填 (預設 86400000) | JWT 權杖有效存活時間（毫秒），預設 86400000 (24 小時) | `86400000` |
| `SPRING_PROFILES_ACTIVE`| 正式環境 | **強制必填** | 宣告啟用之 Spring Profile，正式環境請指定 `prod` 或 `mssql,prod` | `mssql,prod` |

---

## 3. 多平台環境變數注入標準作業程序 (SOP)

### 途徑 A：Linux 實體機 / 虛擬機 Systemd 部署 (`chmod 600` 環境檔)

此為傳統 Linux 伺服器最安全且標準之服務管制作法：

1. **建立機密環境變數檔案**：
   在 `/etc/tibame/` 下建立專用環境檔 `/etc/tibame/daily-ledger.env`：
   ```bash
   sudo mkdir -p /etc/tibame
   sudo nano /etc/tibame/daily-ledger.env
   ```
   檔案內容填寫如下：
   ```ini
   # /etc/tibame/daily-ledger.env
   SPRING_PROFILES_ACTIVE=mssql,prod
   DB_URL=jdbc:sqlserver://sql-cluster.internal.corp:1433;databaseName=tibame_account;encrypt=true;trustServerCertificate=false;
   DB_USERNAME=ledger_prod_svc
   DB_PASSWORD=StrongDatabasePassword2026!#
   JWT_SECRET=EnterpriseProductionQuantumSafeSecretKeyForDailyLedger2026!#SwissLedger
   JWT_EXPIRATION_MS=86400000
   ```

2. **配置私有安全權限 (`chmod 600`)**：
   確保僅有運行該程序的服務帳號（例如 `tibame` 或 `root`）具備讀取權限，徹底防範本機其他使用者窺探：
   ```bash
   sudo chown root:tibame /etc/tibame/daily-ledger.env
   sudo chmod 600 /etc/tibame/daily-ledger.env
   ls -la /etc/tibame/daily-ledger.env
   # 輸出驗證: -rw------- 1 root tibame ... /etc/tibame/daily-ledger.env
   ```

3. **撰寫 Systemd 服務單元檔 (`/etc/systemd/system/daily-ledger.service`)**：
   ```ini
   [Unit]
   Description=Daily Ledger System Spring Boot Application
   After=network.target sqlserver.service

   [Service]
   Type=simple
   User=tibame
   Group=tibame
   WorkingDirectory=/opt/tibame/app
   EnvironmentFile=/etc/tibame/daily-ledger.env
   ExecStart=/usr/bin/java -Xms1024m -Xmx2048m -jar /opt/tibame/app/daily-ledger-system.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10
   StandardOutput=journal
   StandardError=journal

   [Install]
   WantedBy=multi-user.target
   ```

4. **啟動與狀態檢查**：
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable daily-ledger.service
   sudo systemctl start daily-ledger.service
   sudo systemctl status daily-ledger.service
   ```

---

### 途徑 B：容器化環境 (Docker & Docker Compose)

1. **建立正式環境專用 `.env.prod` 檔案**：
   ```bash
   cat << 'EOF' > .env.prod
   SPRING_PROFILES_ACTIVE=mssql,prod
   DB_URL=jdbc:sqlserver://sqlserver:1433;databaseName=tibame_account;encrypt=false;trustServerCertificate=true;
   DB_USERNAME=sa
   DB_PASSWORD=VeryStrongProdPassword2026!#
   JWT_SECRET=CustomProductionSuperSecureSecretKeyWithSufficientEntropy2026!#SwissLedger
   JWT_EXPIRATION_MS=86400000
   EOF
   chmod 600 .env.prod
   ```
   > [!IMPORTANT]
   > 必須確認 `.env.prod` 與所有包含機密的 `.env*` 檔案均已被列入 `.gitignore`，嚴禁提交至 Git 倉庫。

2. **編寫 `docker-compose.prod.yml`**：
   ```yaml
   version: '3.8'

   services:
     daily-ledger:
       image: daily-ledger-system:latest
       container_name: daily-ledger-app
       ports:
         - "8080:8080"
       env_file:
         - .env.prod
       restart: always
       healthcheck:
         test: ["CMD-SHELL", "curl -f http://localhost:8080/ || exit 1"]
         interval: 30s
         timeout: 5s
         retries: 3
   ```

3. **啟動容器**：
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

---

### 途徑 C：雲原生環境 (Kubernetes Secrets)

在 Kubernetes 叢集中，機密參數應透過 `Secret` 資源妥善封裝並透過加密保存於 etcd：

1. **建立 Kubernetes Secret 物件**：
   ```bash
   kubectl create secret generic daily-ledger-prod-secrets \
     --from-literal=DB_URL='jdbc:sqlserver://mssql-cluster-service:1433;databaseName=tibame_account;encrypt=true;trustServerCertificate=false;' \
     --from-literal=DB_USERNAME='ledger_app_prod' \
     --from-literal=DB_PASSWORD='UltraSecurePassword2026!#' \
     --from-literal=JWT_SECRET='K8sProdHighEntropyJwtSecretKeyWithMoreThan32BytesLength2026!#' \
     --from-literal=JWT_EXPIRATION_MS='86400000' \
     -n production
   ```

2. **於 Deployment 中掛載機密**：
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: daily-ledger-deployment
     namespace: production
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: daily-ledger
     template:
       metadata:
         labels:
           app: daily-ledger
       spec:
         containers:
         - name: daily-ledger-container
           image: your-registry.corp/daily-ledger-system:0.0.1-SNAPSHOT
           ports:
           - containerPort: 8080
           env:
           - name: SPRING_PROFILES_ACTIVE
             value: "mssql,prod"
           envFrom:
           - secretRef:
               name: daily-ledger-prod-secrets
           resources:
             limits:
               cpu: "2"
               memory: "2Gi"
             requests:
               cpu: "500m"
               memory: "1Gi"
   ```

---

## 4. 驗證與排查指引 (Verification & Troubleshooting)

### 啟動日誌驗證

當伺服器正確注入環境變數且啟動時，可在日誌中觀察到以下健全狀態：
- **正確載入 profile**：`The following 2 profiles are active: "mssql", "prod"`
- **資料庫正常連線**：HikariCP 連線池成功初始化連線至正式 SQL Server。
- **JWT 驗證通過**：未出現任何 `IllegalStateException` 或 `IllegalArgumentException`。

### 弱密鑰啟動攔截 (Fail-Fast) 診斷訊息

若維運人員疏忽未覆蓋 `JWT_SECRET`，Spring Boot 將中斷啟動並在日誌終端輸出：
```
java.lang.IllegalStateException: 生產環境資安防護阻斷：檢測到啟用 prod Profile 但仍使用預設開發 JWT 金鑰。請設定環境變數 JWT_SECRET 以提供高強度生產密鑰！
	at com.tibame.config.JwtProperties.validateSecurityPosture(JwtProperties.java:65)
```
此時請檢查 Systemd `EnvironmentFile`、Docker `.env.prod` 或 K8s Secret 配置，確認 `JWT_SECRET` 變數名稱與數值正確無誤。
