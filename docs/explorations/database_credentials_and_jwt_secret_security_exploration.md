# 資料庫連線憑證與 JWT 簽署金鑰安全管理策略探索報告 (Database Credentials & JWT Secret Security Strategy Exploration)

> **文件版本**：v1.0.0  
> **建立日期**：2026-09-04  
> **模式定位**：探索報告 (Exploration Report / opsx-explore)  
> **技術棧**：Spring Boot 3.3.13 / Spring Environment / Microsoft SQL Server / JJWT / Linux Systemd / Docker / K8s  
> **目標範疇**：資料庫密碼去明文化、JWT 簽署金鑰納管、正式伺服器資安合規、環境變數動態覆蓋架構  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  
> **關聯文件**：[YAML 設定檔規範與 JWT 強型別配置探索報告 (yaml_configuration_and_ide_diagnostics_exploration.md)](yaml_configuration_and_ide_diagnostics_exploration.md) / [抗量子密碼學與密碼管理探索報告 (modular_crypto_and_pqc_design_exploration.md)](modular_crypto_and_pqc_design_exploration.md)

---

## 1. 探索背景與安全挑戰剖析 (Background & Security Challenges)

在現有代碼庫中，資料庫連線參數與核心安全憑證分散於多個 Spring Boot YAML 配置檔中。特別是連線至微軟 SQL Server 的專屬設定檔以及全域應用的 JWT 簽署配置，存在顯著的明文硬編碼風險。

### 1.1 現狀暴露點分析 (Current Exposure Points)

#### 1. MS SQL 連線設定檔：`src/main/resources/application-mssql.yml`
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=tibame_account;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true;
    username: sa
    password: '1111'
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```
- **風險**：管理員帳號 (`sa`) 與預設弱密碼 (`'1111'`) 以明文形式直接存於檔案中，並隨版本控制系統（Git）追蹤。

#### 2. 全域設定檔與類別：`src/main/resources/application.yml` 與 `JwtProperties.java`
```yaml
jwt:
  secret: SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026
  expiration-ms: 86400000
```
- **風險**：JWT 簽署金鑰（HMAC-SHA256 Secret）直接寫死於設定檔與 Java 類別預設欄位中。若攻擊者取得此金鑰，即可在任何地點偽造合法使用者的 JWT 權杖，完全繞過後端認證機制。

### 1.2 正式伺服器部署之核心資安要求 (Production Security Compliance)

在部署至正式生產環境（Production）時，系統必須遵循現代資訊安全與雲原生標準（如 12-Factor App、OWASP ASVS、ISO 27001）：

```
+-------------------------------------------------------------------------------+
|                            正式環境資安合規兩大基石                            |
+-------------------------------------------------------------------------------+
|                                                                               |
|  1. 原始碼零機密原則 (Zero Secrets in VCS):                                    |
|     * 嚴格禁止任何正式環境的資料庫密碼、服務憑證與加密金鑰進入 Git 歷史紀錄。   |
|     * 代碼庫僅能留存「變數佔位符（Placeholders）」或「安全預設範本」。         |
|                                                                               |
|  2. 弱密碼拒絕啟動原則 (Fail-Fast on Missing Secrets):                        |
|     * 正式環境若未正確配置外部機密（如維運人員漏設環境變數），系統必須拒絕啟動， |
|       立即拋出嚴重異常中斷程序，絕不能隱性降級回本地弱密碼 ('1111')。          |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. 四大安全技術架構對比與決策矩陣 (Architecture Options & Decision Matrix)

針對「將密碼轉化為較安全做法」，本探索全面評估了四種業界主流架構途徑：

```
[低安全 / 零門檻]
  Level 0: 明文硬編碼在 YAML (現狀)
      |
      v
  Level 1: 方案 C: 外部本地獨立配置隔離 (.gitignore + local profile)
      |
      v
  Level 2: 方案 A: 系統環境變數佔位符注入 (Spring Relaxed Binding / 12-Factor) [選定方案]
      |
      v
  Level 3: 方案 B: 組態屬性對稱加密 (Jasypt ENC 標籤)
      |
      v
  Level 4: 方案 D: 集中式秘密管理系統 (HashiCorp Vault / Cloud Secret Manager)
[最高安全 / 架構維運成本最高]
```

### 2.1 四大架構詳細評估

```
+---------------------------------------------------------------------------------------------------------------+
|                                          四大敏感資訊安全方案對比矩陣                                         |
+--------------------+----------------------+--------------------+---------------------+------------------------+
| 評估維度           | 方案 A：環境變數注入 | 方案 B：Jasypt 加密 | 方案 C：外部私有檔案| 方案 D：Vault 秘密系統 |
+--------------------+----------------------+--------------------+---------------------+------------------------+
| **核心機制**       | ${DB_PASSWORD:1111}  | ENC(密文) + 主金鑰 | .gitignore 排除檔案 | 啟動連線 Vault 獲取    |
| **安全性等級**     | ★★★★☆                | ★★★★☆              | ★★★☆☆               | ★★★★★                  |
| **程式碼更動量**   | 極小（僅調整 YAML）  | 中（需引入相依套件）| 小（調整 YAML 匯入）| 極大（需微服務架構）   |
| **額外依賴**       | 無 (Spring 原生支援) | jasypt-starter     | 無                  | spring-cloud-vault     |
| **本機開發阻力**   | 零（具備相容預設值） | 需解密金鑰或明文檔 | 需手動複製範本檔    | 需啟動本地 Vault 服務  |
| **CI/CD 友善度**   | 最佳（GitHub Secrets)| 良好（需傳遞 Master)| 需在 CI 動態產檔    | 需設定 CI 認證角色     |
| **生產環境合規**   | 完全符合 (12-Factor) | 符合                | 不建議正式環境使用  | 頂級金融企業標準       |
+--------------------+----------------------+--------------------+---------------------+------------------------+
```

### 2.2 決策結論：選定「方案 A（環境變數動態覆蓋）」

- **選擇理由**：
  1. **零外部依賴**：完全利用 Spring Boot 原生 `Environment` 與屬性佔位符解析能力，不增加 Maven 相依負擔。
  2. **生產環境標準**：現代容器化（Docker / Kubernetes）與 Linux Systemd 原生支援環境變數注入，維運門檻低、安全強度高。
  3. **雙態相容**：本地開發保留開發用預設值（Zero-Friction Dev），生產環境透過環境變數覆蓋並具備 Fail-Fast 安全檢驗。

---

## 3. 選定方案（方案 A）落地架構與實施設計 (Implementation Blueprint)

### 3.1 雙態流向架構圖 (Dual-State Execution Architecture)

系統透過環境變數與預設值的優先級解析，同時滿足本地開發與生產隔離：

```
+-------------------------------------------------------------------------------+
|                           本機開發環境 (Local Development)                    |
+-------------------------------------------------------------------------------+
|                                                                               |
|   開發者電腦 (未設置 DB_PASSWORD / JWT_SECRET 環境變數)                       |
|        |                                                                      |
|        v                                                                      |
|   Spring Boot 啟動解析 YAML:                                                  |
|   * spring.datasource.password = ${DB_PASSWORD:1111}  ---> 解析為 '1111'      |
|   * jwt.secret = ${JWT_SECRET:SwissLedger...}         ---> 解析為開發用金鑰   |
|        |                                                                      |
|        +--> 開發者零設定直接開箱運行 (Zero Setup Overhead)                    |
|                                                                               |
+-------------------------------------------------------------------------------+

+-------------------------------------------------------------------------------+
|                           正式生產環境 (Production Environment)               |
+-------------------------------------------------------------------------------+
|                                                                               |
|   實體機 Systemd (chmod 600 env 檔) 或 Kubernetes Secret 物件                 |
|   [DB_PASSWORD=StrongProdP@ss2026!]  [JWT_SECRET=RandomProdSecure256BitKey..] |
|        |                                                                      |
|        v (容器/進程啟動時注入 Process Environment)                            |
|   Spring Boot 啟動解析 YAML:                                                  |
|   * spring.datasource.password ---> 被環境變數 StrongProdP@ss2026! 覆蓋       |
|   * jwt.secret                 ---> 被環境變數 RandomProdSecure256BitKey.. 覆蓋|
|        |                                                                      |
|        +--> 記憶體中完全使用高強度正式機密，Git 原始碼庫零外洩風險            |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

### 3.2 具體配置改造規範 (Configuration Specifications)

#### 1. MS SQL 連線設定：`src/main/resources/application-mssql.yml`
改為具備自文檔化（Self-documenting）的屬性佔位符：
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=tibame_account;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true;}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:1111}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

#### 2. 全域 JWT 設定：`src/main/resources/application.yml`
```yaml
jwt:
  secret: ${JWT_SECRET:SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

#### 3. 啟動期安全驗證防線（Fail-Fast Validation in `JwtProperties.java`）
為防止生產環境誤用開發用弱金鑰，在 `JwtProperties` 類別內增設防禦機制：
```java
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private static final String DEFAULT_DEV_SECRET = "SwissLedgerSecureJwtKeyForDailyAccountBookSystem2026!#SwissLedger2026";

    private String secret = DEFAULT_DEV_SECRET;
    private long expirationMs = 86400000L;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void validateSecurityPosture() {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProduction && DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                "【重大資安警告】正式生產環境 (prod) 嚴禁使用預設開發 JWT 金鑰！請透過環境變數 JWT_SECRET 注入高強度密鑰。"
            );
        }
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                "JWT 密鑰長度不足！HMAC-SHA256 密鑰長度必須至少為 256 位元 (32 位元組)。"
            );
        }
    }
}
```

---

## 4. 正式伺服器部署操作指南 (Production Deployment Guide)

在正式伺服器環境中，機密資訊不得直接暴露於命令列參數（以防止 `ps aux` 查閱時外洩）。應採用以下標準實踐：

### 4.1 傳統 Linux 伺服器部署 (Systemd Service)

1. **建立專屬環境設定檔**：
   ```bash
   sudo mkdir -p /etc/tibame
   sudo nano /etc/tibame/daily-ledger.env
   ```

2. **寫入正式機密（不包含引號）**：
   ```ini
   # /etc/tibame/daily-ledger.env
   DB_URL=jdbc:sqlserver://prod-db-server.internal:1433;databaseName=tibame_ledger;encrypt=true;trustServerCertificate=false;
   DB_USERNAME=ledger_prod_app
   DB_PASSWORD=YourSuperStrongProductionSqlPassword2026!
   JWT_SECRET=ProdCustomSecureKeyShouldBeLongAndUnpredictableRandomString2026!#SwissLedger
   JWT_EXPIRATION_MS=86400000
   ```

3. **鎖定檔案權限（極關鍵資安步驟）**：
   ```bash
   # 僅允許運行應用的特定系統帳號（例如 spring）與 root 讀取
   sudo chown spring:spring /etc/tibame/daily-ledger.env
   sudo chmod 600 /etc/tibame/daily-ledger.env
   ```

4. **在 Systemd Unit 中引用**：
   在 `/etc/systemd/system/daily-ledger.service` 內配置：
   ```ini
   [Unit]
   Description=Tibame Daily Ledger System
   After=network.target

   [Service]
   Type=simple
   User=spring
   Group=spring
   EnvironmentFile=/etc/tibame/daily-ledger.env
   ExecStart=/usr/bin/java -jar /opt/tibame/daily-ledger-system.jar --spring.profiles.active=mssql,prod
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```

---

### 4.2 容器化部署 (Docker Compose & Kubernetes)

#### Docker Compose
在生產伺服器的 `.env` 檔案中定義（確保 `.env` 不被 Git 追蹤）：
```yaml
# docker-compose.yml
services:
  daily-ledger-app:
    image: tibame/daily-ledger-system:latest
    environment:
      - DB_URL=${PROD_DB_URL}
      - DB_USERNAME=${PROD_DB_USERNAME}
      - DB_PASSWORD=${PROD_DB_PASSWORD}
      - JWT_SECRET=${PROD_JWT_SECRET}
    ports:
      - "8080:8080"
```

#### Kubernetes (K8s Secret)
```bash
# 透過 kubectl 建立密鑰物件
kubectl create secret generic daily-ledger-secrets \
  --from-literal=db-password='YourSuperStrongProductionSqlPassword2026!' \
  --from-literal=jwt-secret='ProdCustomSecureKeyShouldBeLongAndUnpredictableRandomString2026!#SwissLedger'
```
在 Deployment YAML 中透過 `envFrom` 或 `valueFrom.secretKeyRef` 映射至環境變數。

---

### 4.3 GitHub Actions CI/CD Pipeline 整合

在 GitHub Actions 管線中，可在 Repository Settings -> Secrets and variables -> Actions 中建立：
- `MSSQL_SA_PASSWORD`
- `JWT_SIGNING_SECRET`

在 `.github/workflows/ci-pr.yml` 中執行測試時以環境變數形式傳遞：
```yaml
- name: Run Integration Tests
  env:
    DB_PASSWORD: ${{ secrets.MSSQL_SA_PASSWORD }}
    JWT_SECRET: ${{ secrets.JWT_SIGNING_SECRET }}
  run: ./mvnw test -Dtest=AccountBookIntegrationTest
```

---

## 5. 總結與後續實施建議 (Next Steps)

本探索報告確立了本專案在敏感配置上的核心治理方向：
1. **去硬編碼化**：將 `application-mssql.yml` 的密碼與 `application.yml` 的 JWT 金鑰全數改為環境變數佔位符。
2. **零摩擦過渡**：為本地開發環境保留合法預設值，確保現有單元測試、E2E 測試與開發者工作流程不受破壞。
3. **加強防護**：提供生產環境部署 SOP（Systemd / chmod 600 / K8s Secrets）並可在 `JwtProperties` 建立 Fail-Fast 啟動防呆。

> **下一步行動**：  
> 若欲將本探索設計正式落實至代碼庫，可透過 OpenSpec 工作流程建立正式變更提案（例如：`openspec new change secure-sensitive-properties-with-env-vars`），依照提案、規格、任務的三階流程推動實作。
