-- ==========================================================
-- Daily Ledger System - MS SQL Server DDL Schema
-- Database: tibame_account
-- ==========================================================

-- 1. 使用者表 (sys_user)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'sys_user')
BEGIN
    CREATE TABLE sys_user (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password_hash VARCHAR(100) NOT NULL,
        email NVARCHAR(100) NOT NULL,
        display_name NVARCHAR(50) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );
END;

-- 2. 分類表 (sys_category)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'sys_category')
BEGIN
    CREATE TABLE sys_category (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        type VARCHAR(10) NOT NULL, -- EXPENSE / INCOME
        name NVARCHAR(50) NOT NULL,
        icon_code VARCHAR(30) NULL,
        is_system BIT NOT NULL DEFAULT 0,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_category_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    );
END;

-- 3. 流水帳記錄表 (account_record)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'account_record')
BEGIN
    CREATE TABLE account_record (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        category_id BIGINT NOT NULL,
        record_type VARCHAR(10) NOT NULL, -- EXPENSE / INCOME
        amount DECIMAL(12, 2) NOT NULL,
        description NVARCHAR(200) NULL,
        record_date DATE NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        CONSTRAINT FK_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
        CONSTRAINT FK_record_category FOREIGN KEY (category_id) REFERENCES sys_category(id)
    );
END;
