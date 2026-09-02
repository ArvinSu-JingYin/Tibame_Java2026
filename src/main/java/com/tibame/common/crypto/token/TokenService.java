package com.tibame.common.crypto.token;

/**
 * 權杖管理服務標準介面
 */
public interface TokenService {

    /**
     * 為指定使用者產生簽章權杖 (JWT)
     *
     * @param userId   使用者 ID
     * @param username 使用者帳號
     * @return 簽名後的權杖字串
     */
    String generateToken(Long userId, String username);

    /**
     * 驗證權杖之簽章與有效性
     *
     * @param token 權杖字串
     * @return 若有效且未過期回傳 true，否則回傳 false
     */
    boolean validateToken(String token);

    /**
     * 從權杖中提取使用者 ID
     *
     * @param token 權杖字串
     * @return 使用者 ID，無效時回傳 null
     */
    Long getUserIdFromToken(String token);

    /**
     * 從權杖中提取使用者帳號名稱
     *
     * @param token 權杖字串
     * @return 使用者帳號名稱，無效時回傳 null
     */
    String getUsernameFromToken(String token);
}
