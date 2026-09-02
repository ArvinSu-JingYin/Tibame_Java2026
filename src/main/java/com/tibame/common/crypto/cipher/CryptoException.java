package com.tibame.common.crypto.cipher;

/**
 * 密碼學與加解密專用例外
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
