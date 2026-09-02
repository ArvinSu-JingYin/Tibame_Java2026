package com.tibame.common.crypto.cipher;

/**
 * 通用對稱雙向加解密服務標準介面
 */
public interface CryptoService {

    /**
     * 對明文資料進行對稱加密，回傳自描述版本信封格式字串
     *
     * @param plainText 明文字串
     * @return 自描述密文字串 (例如 "$v1$aes256gcm$...")
     * @throws CryptoException 加密失敗時拋出
     */
    String encrypt(String plainText);

    /**
     * 解析自描述密文信封並還原為明文
     *
     * @param cipherEnvelope 自描述密文字串
     * @return 原始明文字串
     * @throws CryptoException 解密失敗、格式錯誤或密文遭竄改時拋出
     */
    String decrypt(String cipherEnvelope);

    /**
     * 取得加解密演算法名稱
     *
     * @return 演算法名稱識別字串 (例如 "AES-256-GCM")
     */
    String getAlgorithmName();
}
