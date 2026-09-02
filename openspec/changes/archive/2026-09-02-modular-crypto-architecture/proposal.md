## Why

目前系統的密碼雜湊與安全性實作直接耦合 Spring Security 框架 (`PasswordEncoder` / `BCryptPasswordEncoder`)，且缺乏通用的雙向對稱加解密模組。隨著資安防護要求提升與後量子密碼學 (Post-Quantum Cryptography, PQC) 的演進，需要建立一套具備密碼學敏捷度 (Crypto Agility) 的模組化架構，將密碼驗證、原則檢驗、演算法自動平滑升級與量子安全對稱加密 (AES-256-GCM) 進行標準化封裝與解耦，以保障系統長期安全與可維護性。

## What Changes

- **新增密碼管理模組 (`PasswordService`)**：定義標準密碼雜湊、比對與升級檢測介面，預設提供 BCrypt 實作，支援多演算法策略切換。
- **新增密碼強度驗證模組 (`PasswordPolicyValidator`)**：提供可配置的密碼複雜度原則校驗機制，並支援透過 `application.yml` 進行自定義。
- **新增登入自動重雜湊升級機制 (Hash Auto-Upgrade)**：當用戶登入時，系統自動檢測現存 Hash 是否符合最新安全參數；若不符則自動重新計算並寫回資料庫，用戶無感知。
- **新增通用抗量子對稱加解密模組 (`CryptoService`)**：採用 AES-256-GCM (AEAD) 提供 256-bit 量子抗性加密，搭配隨機 IV 與自描述版本封裝格式 (`$v1$aes256gcm$...`)。
- **重構認證服務 (`AuthServiceImpl`)**：解除對 Spring Security 的直接綁定，全面改為依賴 `PasswordService` 與 `PasswordPolicyValidator`。
- **權杖服務重整 (`TokenService`)**：將既有 JWT 權杖實作歸納至統一的 `com.tibame.common.crypto` 命名空間。
- **保持 100% 向後相容**：現有資料庫中的使用者密碼雜湊與既有對外 REST API 簽約完全相容，不受任何破壞性影響。

## Capabilities

### New Capabilities
- `cryptography-and-security`: 提供量子安全對稱加解密 (AES-256-GCM AEAD) 服務與自描述密文封裝格式，保護系統內部與資料庫敏感資料。

### Modified Capabilities
- `user-authentication`: 密碼雜湊與驗證由 `PasswordService` 模組化提供，加入密碼強度檢驗與登入自動雜湊升級 (Auto-Upgrade) 能力，並維持現有帳號密碼完全相容。

## Impact

- **後端程式碼**：
  - 新增 `com.tibame.common.crypto` 套件（含 `password/`, `cipher/`, `token/` 子模組）。
  - 修改 `com.tibame.service.impl.AuthServiceImpl` 注入介面。
  - 清理 `com.tibame.config.AppConfig` 中分散的 Bean 定義。
- **資料庫**：現有 `users` 表的 `password_hash` 欄位維持不變，相容既有 BCrypt 格式。
- **外部 API / 前端**：API 介面與 HTTP 狀態碼保持 100% 一致。
