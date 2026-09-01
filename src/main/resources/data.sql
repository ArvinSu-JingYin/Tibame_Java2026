-- ==========================================================
-- Daily Ledger System - System Default Categories Seed Data
-- ==========================================================

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'飲食聚餐' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'飲食聚餐', 'utensils', 1, 10);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'交通出行' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'交通出行', 'car', 1, 20);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'日常用品' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'日常用品', 'bag', 1, 30);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'居住水電' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'居住水電', 'house', 1, 40);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'休閒娛樂' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'休閒娛樂', 'controller', 1, 50);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'醫療保健' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'醫療保健', 'heart-pulse', 1, 60);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'其他支出' AND type = 'EXPENSE')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'EXPENSE', N'其他支出', 'tags', 1, 99);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'薪資所得' AND type = 'INCOME')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'INCOME', N'薪資所得', 'cash-stack', 1, 10);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'兼職副業' AND type = 'INCOME')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'INCOME', N'兼職副業', 'briefcase', 1, 20);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'投資理財' AND type = 'INCOME')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'INCOME', N'投資理財', 'graph-up-arrow', 1, 30);

IF NOT EXISTS (SELECT 1 FROM sys_category WHERE is_system = 1 AND name = N'其他收入' AND type = 'INCOME')
    INSERT INTO sys_category (user_id, type, name, icon_code, is_system, sort_order) VALUES (NULL, 'INCOME', N'其他收入', 'wallet2', 1, 99);
