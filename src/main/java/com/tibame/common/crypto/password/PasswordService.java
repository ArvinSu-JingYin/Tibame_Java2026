package com.tibame.common.crypto.password;

/**
 * 密碼管理服務標準介面
 * 提供密碼雜湊、比對、升級檢測與演算法識別能力
 */
public interface PasswordService {

    /**
     * 對明文密碼進行安全雜湊
     *
     * @param rawPassword 明文密碼
     * @return 雜湊後字串
     */
    String hash(String rawPassword);

    /**
     * 比對明文密碼與存儲的雜湊值是否相符
     *
     * @param rawPassword 明文密碼
     * @param storedHash  資料庫或快取中存儲的雜湊值
     * @return 若密碼正確回傳 true，否則回傳 false
     */
    boolean verify(String rawPassword, String storedHash);

    /**
     * 檢測存儲的雜湊是否需要升級（例如安全參數調整或過期演算法）
     *
     * @param storedHash 存儲的雜湊值
     * @return 若需升級回傳 true，否則回傳 false
     */
    boolean needsUpgrade(String storedHash);

    /**
     * 取得當前使用的預設演算法名稱
     *
     * @return 演算法名稱識別字串 (例如 "BCrypt")
     */
    String getAlgorithmName();
}
