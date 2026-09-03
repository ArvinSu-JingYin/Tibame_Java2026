# 密碼與通用加密模組化暨抗量子密碼學 (PQC) 架構探索報告

> **專案代號**：`daily-ledger-system`  
> **探索主題**：密碼管理與加解密模組化設計 (Crypto Agility & Post-Quantum Cryptography Readiness)  
> **狀態**：探索完成與架構歸納 (`Exploration Completed`)  
> **建立日期**：2026-09-02  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題陳述 (Background & Problem Statement)

在目前系統的初始實作中，密碼雜湊與安全性元件主要分散於 `AppConfig.java`、`AuthServiceImpl.java` 與 `common/security` 中，存在以下架構限制與潛在風險：

```
+-----------------------------------------------------------------------------+
|                           [現有架構現狀與耦合點]                            |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [Controller Layer]                                                         |
|         |                                                                   |
|         v                                                                   |
|  [AuthServiceImpl] ----------------------------+                            |
|         |                                      |                            |
|         | (直接依賴 Spring Security API)        | (自定義 Token 抽象)         |
|         v                                      v                            |
|  [PasswordEncoder]                     [TokenService]                       |
|   (BCryptPasswordEncoder)               (JwtTokenServiceImpl - JJWT)        |
|   定義於 AppConfig.java                  定義於 common/security/             |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### 核心痛點分析：
1. **商業邏輯直接耦合第三方框架 API**：
   - 業務服務層（[`AuthServiceImpl`](file:///c:/Arvin/GitHub/Tibame_Java2026/Tibame_Java2026/src/main/java/com/tibame/service/impl/AuthServiceImpl.java)）直接注入並綁定 Spring Security 的 `PasswordEncoder`。若未來更換底層實作或自定義安全框架，商業邏輯層將被迫重構。
2. **缺乏演算法擴展性與平滑升級機制 (Hash Migration / Upgrade)**：
   - 系統寫死為單一 BCrypt 演算法。若未來系統安全等級提升（如升級至 Argon2id、調整 Work Factor / Cost）時，缺乏演算法前綴識別與登入自動重雜湊升級機制。
3. **缺乏通用的雙向加解密抽象模組 (Cipher / Crypto Module)**：
   - 目前系統僅具備單向雜湊與 JWT 簽章，缺少針對敏感資料（如個資、金流備註、第三方金鑰、資料庫欄位保護）的**雙向對稱加解密 (Symmetric Encryption)** 機制。
4. **密碼原則與強度驗證機制缺失 (Password Policy & Strength Validation)**：
   - 註冊與修改密碼僅依賴基本的 DTO `@NotBlank` 驗證，缺少模組化且可配置的密碼複雜度校驗器（長度、字符組合、防弱密碼字典等）。

---

## 2. 後量子密碼學 (PQC) 威脅評估與防禦設計

隨著量子運算技術的發展，傳統密碼學演算法面臨嚴峻的數學理論威脅。本架構在設計之初即導入**密碼學敏捷度 (Crypto Agility)**，確保具備量子抗性（Quantum-Resistant Readiness）：

```
+-------------------------------------------------------------------------------+
|                       量子運算對密碼學的威脅與防禦對策                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [密碼學領域]         [量子演算法威脅]          [抗量子 (PQC) 防禦設計]        |
|                                                                               |
|  1. 對稱加密          Grover 演算法             * 採用 AES-256-GCM            |
|     (AES / Cipher)    (有效金鑰長度減半)          (256-bit 經 Grover 弱化後   |
|                       AES-128 等同僅剩 64-bit     仍具 128-bit 充足量子安全)  |
|                       不可再使用！              * 密文採用「自描述版本標頭」  |
|                                                                               |
|  2. 密碼雜湊          BHT / Grover 演算法       * 現階段 BCrypt / SHA-256     |
|     (Password Hash)   (碰撞與原像攻擊加速)      * 架構支援 Argon2id 擴充      |
|                                                 * 支援登入時「自動無感升級」  |
|                                                                               |
|  3. 非對稱/金鑰交換   Shor 演算法               * 預留 PQC 混成架構           |
|     (RSA / ECC)       (在多項式時間內被完全破解)  (Hybrid Classical-PQC:      |
|                                                  如 ML-KEM/Kyber 擴充點)     |
+-------------------------------------------------------------------------------+
```

---

## 3. 模組化架構設計藍圖 (Crypto Agility Architecture)

整體架構將安全與密碼學功能拆分為三大高內聚模組：**密碼管理模組**、**通用加解密模組**與**權杖憑證模組**。

```
+-------------------------------------------------------------------------------------------+
|                          [Crypto Agility 模組化分層架構]                                  |
+-------------------------------------------------------------------------------------------+
|                                                                                           |
|  [商業業務層 (Business Layer)]                                                             |
|   - AuthServiceImpl (用戶註冊/登入/身份管理)                                               |
|   - LedgerService / Future Service (敏感資料加密保護)                                     |
|         |                                              |                                  |
|         v                                              v                                  |
|  +------------------------------+             +------------------------------+            |
|  |     PasswordService (介面)    |             |      CryptoService (介面)     |            |
|  +------------------------------+             +------------------------------+            |
|  | * hash(rawPassword)          |             | * encrypt(plainText)         |            |
|  | * verify(rawPassword, hash)  |             | * decrypt(cipherText)        |            |
|  | * needsUpgrade(hash)         |             | * getAlgorithmName()         |            |
|  +------------------------------+             +------------------------------+            |
|         |                                              |                                  |
|         v (策略工廠 / 委派)                             v (自描述版本路由 / Dispatcher)    |
|  +------------------------------+             +------------------------------+            |
|  |   BCryptPasswordServiceImpl  |             |    AesGcmCryptoServiceImpl   |            |
|  |   (支援 Cost Factor 與升級)  |             |    (AES-256-GCM + 隨機 IV)   |            |
|  +------------------------------+             +------------------------------+            |
|    |                                            |                                         |
|    +-- [擴充點: Argon2id]                        +-- [擴充點: PQC Hybrid / Kyber]          |
|                                                                                           |
+-------------------------------------------------------------------------------------------+
```

---

## 4. 核心子模組詳細規劃

### 4.1 密碼管理模組 (`com.tibame.common.crypto.password`)

1. **`PasswordService` 介面**：
   - `String hash(String rawPassword)`：將明文密碼進行安全雜湊。
   - `boolean verify(String rawPassword, String storedHash)`：驗證明文密碼與存儲的雜湊值是否匹配。
   - `boolean needsUpgrade(String storedHash)`：檢查存儲的雜湊是否已落後於目前系統配置的安全參數（如 Cost Factor 變更或過期演算法）。

2. **`PasswordPolicyValidator` 介面與設定**：
   - 負責密碼複雜度校驗（最小長度、英數字與特殊符號要求）。
   - 透過 `PasswordPolicyProperties` 映射 `application.yml` 配置，便於營運與資安合規調整。

3. **登入無感平滑升級流程 (Password Verification & Auto-Upgrade Flow)**：
   ```
   +---------------+              +-----------------+              +--------------------+
   |  LoginRequest |              | AuthServiceImpl |              |  PasswordService   |
   +---------------+              +-----------------+              +--------------------+
           |                               |                                  |
           |--- 1. login(username, pwd) -->|                                  |
           |                               |--- 2. verify(pwd, storedHash) -->|
           |                               |<-- 3. boolean result ------------|
           |                               |                                  |
           |                               |--- 4. needsUpgrade(storedHash) ->|
           |                               |<-- 5. boolean upgradeNeeded -----|
           |                               |                                  |
           |                               | [If upgradeNeeded == true]       |
           |                               |--- 6. hash(pwd) ---------------->|
           |                               |<-- 7. new upgraded hash ---------|
           |                               |                                  |
           |                               | (更新資料庫 user.passwordHash)    |
           |<-- 8. Login Success (JWT) ----|                                  |
   ```

---

### 4.2 通用加解密模組 (`com.tibame.common.crypto.cipher`)

1. **量子安全對稱加密標準**：
   - **演算法**：`AES/GCM/NoPadding`（256-bit Key）。
   - **隨機性保證**：每次加密使用 `SecureRandom` 動態生成 **12-byte IV / Nonce**。
   - **防竄改 (AEAD)**：包含 **128-bit Authentication Tag**，防止密文被竄改或重放。

2. **自描述密文封裝格式 (Self-Describing Ciphertext Envelope)**：
   - 密文標準輸出格式：
     ```
     $v1$aes256gcm$<Base64-12Byte-IV>$<Base64-CipherTextAndTag>
     ```
   - **優勢**：
     - 未來引進後量子對稱演算法（如 `$v2$chacha20poly1305$...$` 或 `$v3$pqc-hybrid$...$`）時，解密器能依據前綴自動路由至對應演算法，新舊資料庫密文和平共存。

---

### 4.3 權杖與安全整合模組 (`com.tibame.common.crypto.token`)

- 將現有 `TokenService` 與 `JwtTokenServiceImpl` 歸納至統一的 `common.crypto.token` 命名空間。
- 統一管理金鑰長度（要求 256-bit 以上 HMAC-SHA 金鑰），並與加密配置解耦。

---

## 5. 推薦套件結構 (Target Package Structure)

```
com.tibame.common.crypto/
├── password/
│   ├── PasswordService.java                  # 密碼雜湊與驗證介面
│   ├── PasswordPolicyValidator.java          # 密碼複雜度檢驗介面
│   ├── PasswordPolicyProperties.java         # 密碼原則配置類
│   └── impl/
│       ├── BCryptPasswordServiceImpl.java    # 預設 BCrypt 實作 (支援升級判定)
│       └── DefaultPasswordPolicyValidator.java# 預設密碼強度檢驗實作
│
├── cipher/
│   ├── CryptoService.java                    # 雙向通用加解密介面
│   ├── CryptoException.java                  # 專用加密例外
│   ├── CryptoProperties.java                 # 金鑰與演算法設定
│   └── impl/
│       └── AesGcmCryptoServiceImpl.java      # AES-256-GCM 抗量子對稱加密實作
│
└── token/
    ├── TokenService.java                     # 權杖服務介面 (保留並重新組織)
    └── impl/
        └── JwtTokenServiceImpl.java          # JJWT 權杖實作
```

---

## 6. 相容性評估與遷移策略 (Compatibility & Migration Strategy)

| 評估維度 | 實作策略 | 相容性保證 |
| :--- | :--- | :--- |
| **現有資料庫資料** | 現存 User 的 `passwordHash` (BCrypt `$2a$` 格式) | **100% 向後相容**，既有密碼雜湊不需重設即可直接驗證。 |
| **業務層呼叫** | `AuthServiceImpl` 改為注入 `PasswordService` | **100% API 簽約相容**，Controller 與前端介面零感知。 |
| **配置解耦** | 廢棄 `AppConfig.java` 內的臨時 Bean，由模組提供自動配置 | 架構更清晰，降低 Spring Security 框架綁定。 |
| **測試覆蓋** | 撰寫單元測試覆蓋 Hash 升級、AES-GCM 加解密與極端密文驗證 | 提供自動化回歸測試防護網。 |

---

## 7. 後續落地建議流程

當團隊準備進行實際程式碼重構時，建議遵循標準 OpenSpec 流程：
1. **建立提案**：`/opsx-propose modular-crypto-architecture`
2. **實施變更**：`/opsx-apply` 執行套件重構、介面實作與單元測試。
3. **驗證與存檔**：確認所有測試通過後，進行 `/opsx-archive`。
