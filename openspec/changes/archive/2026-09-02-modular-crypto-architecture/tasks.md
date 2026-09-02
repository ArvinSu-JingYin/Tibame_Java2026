## 1. 建立密碼與原則子模組 (Password Submodule)

- [x] 1.1 建立 `PasswordService` 介面與 `BCryptPasswordServiceImpl` 實作（支援 hash, verify, needsUpgrade），並撰寫單元測試驗證雜湊運算與升級判定
- [x] 1.2 建立 `PasswordPolicyValidator` 介面、`PasswordPolicyProperties` 配置類與 `DefaultPasswordPolicyValidator` 實作，並撰寫單元測試驗證長度與複雜度檢驗規則

## 2. 建立通用抗量子對稱加解密子模組 (Crypto Cipher Submodule)

- [x] 2.1 建立 `CryptoService` 介面、`CryptoException` 例外類與 `CryptoProperties` 配置類（支援 256-bit 金鑰注入與驗證）
- [x] 2.2 實作 `AesGcmCryptoServiceImpl`（AES-256-GCM AEAD、12-byte 隨機 IV、自描述信封格式 `$v1$aes256gcm$...`），並撰寫單元測試驗證加解密、自描述解析與竄改拒絕行為

## 3. 權杖重整與業務服務層解耦 (AuthService Refactoring)

- [x] 3.1 將 `TokenService` 與 `JwtTokenServiceImpl` 歸納至 `com.tibame.common.crypto.token` 命名空間並確保 Spring Bean 依賴正常
- [x] 3.2 重構 `AuthServiceImpl`，改為注入 `PasswordService` 與 `PasswordPolicyValidator`，並在註冊時加入密碼原則檢驗、在登入成功時加入無感 Auto-Upgrade
- [x] 3.3 重構 `AppConfig.java` 移除重複之 `BCryptPasswordEncoder` 定義，由模組統一自動配置

## 4. 全量整合測試與驗收 (Verification & Testing)

- [x] 4.1 執行全量 Maven 測試 (`mvn test`)，驗證所有認證 API 端點、既有 BCrypt 密碼向後相容性與加解密模組之正確性
