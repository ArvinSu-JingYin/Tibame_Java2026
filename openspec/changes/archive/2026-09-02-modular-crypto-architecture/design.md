## Context

系統目前在商業邏輯層直接耦合 Spring Security 的 `PasswordEncoder` 與 BCrypt 實作，且缺乏通用的雙向對稱加解密模組。隨著資安防護演進與量子密碼威脅（Grover 演算法），系統需要一套具備密碼學敏捷度 (Crypto Agility) 的模組化架構，將密碼驗證、原則檢驗與對稱加密抽離為獨立子模組。詳細動機請參閱 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- **密碼學解耦**：定義標準 `PasswordService` 介面，讓業務層（`AuthServiceImpl`）擺脫對第三方框架 API 的直接依賴。
- **密碼原則檢驗**：建立模組化 `PasswordPolicyValidator`，提供可配置的長度與複雜度校驗。
- **登入無感升級 (Auto-Upgrade)**：支援登入時自動檢測舊版雜湊或過低 Cost Factor，並於背景無感重雜湊升級。
- **抗量子對稱加密**：建立 `CryptoService`，採用 AES-256-GCM (AEAD) 與動態 12-byte IV，輸出自描述信封格式 `$v1$aes256gcm$...`。
- **完全向後相容**：現有使用者資料庫中的 `passwordHash` 保持 100% 相容，無須重設密碼。

**Non-Goals:**
- 本階段不引入重量級外部金鑰管理系統（如 HashiCorp Vault / AWS KMS），以標準 JCA 與 Spring Boot 配置管理為主。
- 不變更外部 REST API 請求/回應 JSON 結構。

## Decisions

### 1. 密碼模組介面化與策略封裝
- **決策**：定義 `com.tibame.common.crypto.password.PasswordService`，由 `BCryptPasswordServiceImpl` 提供預設實作。
- **理由**：解除業務層與 Spring Security 的直接綁定。未來若切換至 Argon2id 或 PBKDF2，僅需替換或新增實作類別。
- **備選方案**：維持直接依賴 Spring Security。缺點是架構僵化，難以實現多演算法策略管理與自定義信封解析。

### 2. 抗量子對稱加密選型 (AES-256-GCM AEAD)
- **決策**：通用加解密模組採用 256-bit 金鑰長度的 `AES/GCM/NoPadding`，每次加密動態產生 12-byte 隨機 IV 與 128-bit 驗證標籤 (Tag)。
- **理由**：Grover 量子演算法會使對稱金鑰有效安全長度減半（AES-128 降至 64-bit 不再安全），AES-256 在量子威脅下仍保有 128-bit 的充分安全強度；GCM 模式提供 AEAD 完整性校驗，可防止密文竄改。
- **備選方案**：AES-CBC + HMAC。缺點是需分別管理兩把金鑰且運算開銷較大。

### 3. 自描述密文信封格式 (Self-Describing Envelope)
- **決策**：加密輸出格式定義為 `$v1$aes256gcm$<Base64-12B-IV>$<Base64-CipherTextAndTag>`。
- **理由**：未來若引入後量子演算法（如 `$v2$kyber...$`），解密路由器只需解析前綴即可自動分發，確保新舊密文於資料庫中無縫共存。
- **備選方案**：直接將 IV 與密文 raw bytes 拼接。缺點是缺乏版本與演算法標識，未來演算法升級時無法識別歷史資料。

### 4. 登入自動雜湊升級流程
- **決策**：在 `AuthServiceImpl.login()` 驗證成功後呼叫 `passwordService.needsUpgrade(user.getPasswordHash())`，若回傳 true 則重新計算並更新 DB。
- **理由**：當系統提升 Cost Factor 或更換雜湊演算法時，使用者登入時即平滑遷移，無須強制全體重設密碼。

## Risks / Trade-offs

- **[風險] BCrypt Cost Factor 過高導致伺服器 CPU 負載過重**  
  → **緩解措施**：預設採用標準 Cost 10（約 80~100ms/次），並可透過設定檔彈性微調。
- **[風險] AES-256 加密金鑰長度不足**  
  → **緩解措施**：在 `CryptoProperties` 與 `AesGcmCryptoServiceImpl` 初始化時強制校驗金鑰位元數（必須為 256-bit / 32 bytes），不足時拋出初始化例外。
- **[風險] 密文篡改或無效 IV**  
  → **緩解措施**：利用 GCM Authentication Tag 進行完整性驗證，解密失敗一律拋出標準 `CryptoException`，不洩漏任何金鑰或底層堆疊資訊。

## Migration Plan

1. 建立 `com.tibame.common.crypto` 基礎設施（密碼、加密、權杖子模組）。
2. 在 `AuthServiceImpl` 中替換注入元件並加入密碼強度檢驗與 Auto-Upgrade。
3. 移除 `AppConfig.java` 內重複的 Bean 定義。
4. 執行全量單元測試與 Spring Boot 整合測試，驗證登入、註冊、加解密之相容性。
