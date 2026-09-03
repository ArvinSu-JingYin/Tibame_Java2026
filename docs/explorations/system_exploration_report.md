# 日常流水帳系統 (Daily Ledger System) — 系統架構與設計探索報告

> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

本文件為「日常流水帳系統」在探索階段（Explore Mode）之完整系統架構、互動設計、資料庫規格與技術落實規劃報告。

---

## 1. 執行摘要 (Executive Summary)

本專案旨在建立一套現代、極簡且高效的**個人日常流水帳管理系統**。系統結合 **Spring Boot 3.x 後端四層架構**、**純離線 (Strict No-CDN) 前端** 與 **瑞士國際主義風格 (Swiss Design Style)** 視覺系統。

- **核心特色**：
  - **類 Google 搜尋框記帳體驗**：首頁工作台以中央焦點列為核心，採用結構化快填（方案 A），並預留未來升級自然語言解析（方案 B）之擴展介面。
  - **雙層分類管理**：提供開箱即用的系統預設分類，並支援使用者個人化自訂分類 CRUD。
  - **安全與隔離**：基於 JWT 無狀態認證，並具備可插拔之安全架構；保證各使用者帳目數據 100% 獨立隔離。
  - **資料庫**：微軟 MS SQL Server（資料庫名稱：`tibame_account`，`localhost:1433`，帳號：`sa`，密碼：`1111`）。

---

## 2. 系統總體架構 (System Architecture)

系統後端採用嚴格四層架構（Controller ➔ Service ➔ Repository ➔ Database），前端採用純離線自託管架構。

```mermaid
graph TD
    subgraph Client ["前端客戶端 (No-CDN Offline)"]
        Browser["瀏覽器 (Browser)"]
        VueApp["Vue 3 Composition API"]
        BS["Bootstrap 5.3 (Grid/RWD)"]
        AxiosExt["Axios (HTTP 攔截器 + Token 注入)"]
        Swal["SweetAlert2 (瑞士幾何風格通知)"]
        Browser --> VueApp
        VueApp --> BS
        VueApp --> AxiosExt
        VueApp --> Swal
    end

    subgraph Backend ["後端服務 (Spring Boot 3.x)"]
        subgraph ControllerLayer ["控制器層"]
            MVC["MVC Controller (Thymeleaf SSR 視圖路由)"]
            API["Web API Controller (@RestController /api/v1/...)"]
        end

        subgraph InterceptorLayer ["安全與認證層"]
            JwtFilter["JwtAuthenticationFilter"]
            UserCtx["CurrentUserContext / ThreadLocal"]
        end

        subgraph ServiceLayer ["業務邏輯層 (@Service / @Transactional)"]
            AuthSvc["AuthService / TokenService (可插拔介面)"]
            CatSvc["CategoryService (分類管理)"]
            LedgerSvc["LedgerService (記帳 CRUD & 統計)"]
            ParserSvc["SmartParserService (方案 B 預留介面)"]
        end

        subgraph RepositoryLayer ["數據訪問層 (Spring Data JPA)"]
            UserRepo["UserRepository"]
            CatRepo["CategoryRepository"]
            RecordRepo["AccountRecordRepository"]
        end
    end

    subgraph Database ["資料庫層 (MS SQL Server)"]
        MSSQL[("tibame_account (localhost:1433)")]
    end

    AxiosExt -->|RESTful JSON| API
    Browser -->|GET 頁面請求| MVC
    API --> JwtFilter
    JwtFilter --> UserCtx
    API --> ServiceLayer
    MVC --> ServiceLayer
    AuthSvc --> UserRepo
    CatSvc --> CatRepo
    LedgerSvc --> RecordRepo
    ParserSvc -.-> LedgerSvc
    UserRepo --> MSSQL
    CatRepo --> MSSQL
    RecordRepo --> MSSQL
```

---

## 3. JWT 認證與可插拔擴展架構 (Authentication Flow)

系統採用 JWT 作為身分驗證憑證，前端透過 Axios 請求攔截器自動附帶 Token，後端透過統一 Filter 提取並驗證。

```mermaid
sequenceDiagram
    autonumber
    actor User as 使用者
    participant Vue as 前端 Vue 3 (Auth/Axios)
    participant AuthApi as AuthApiController
    participant AuthSvc as AuthService / TokenService
    participant MSSQL as MS SQL Server (sys_user)
    participant ApiController as LedgerApiController

    User->>Vue: 於登入頁輸入帳號密碼
    Vue->>AuthApi: POST /api/v1/auth/login
    AuthApi->>AuthSvc: authenticate(username, password)
    AuthSvc->>MSSQL: findByUsername(username)
    MSSQL-->>AuthSvc: 回傳使用者實體 (含 BCrypt 雜湊)
    AuthSvc->>AuthSvc: BCrypt 比對密碼
    AuthSvc->>AuthSvc: TokenService.generateToken(user)
    AuthSvc-->>AuthApi: 回傳 AuthResponseVo (JWT + 使用者資訊)
    AuthApi-->>Vue: 200 OK (ApiResponse 包裝)
    Vue->>Vue: auth.setToken(jwtToken) 存入本地

    Note over User, Vue: 登入成功，導向 /ledger 記帳工作台

    User->>Vue: 操作記帳 (新增/查詢)
    Vue->>ApiController: GET /api/v1/records (Header: Authorization Bearer JWT)
    ApiController->>ApiController: JwtFilter 驗證 Token 並提取 userId
    ApiController->>MSSQL: 查詢該 userId 專屬帳目
    MSSQL-->>ApiController: 帳目數據清單
    ApiController-->>Vue: 200 OK (ApiResponse<List<RecordVo>>)
```

### 可擴展性設計 (Extensibility Design)
```mermaid
classDiagram
    class TokenService {
        <<interface>>
        +generateToken(UserPrincipal principal) String
        +validateToken(String token) boolean
        +getUserIdFromToken(String token) Long
    }
    class JwtTokenServiceImpl {
        -String secretKey
        -Long expirationMs
        +generateToken() String
        +validateToken() boolean
    }
    class OAuth2TokenServiceImpl {
        +generateToken() String
    }
    class SessionTokenServiceImpl {
        +generateToken() String
    }

    TokenService <|.. JwtTokenServiceImpl : 當前實作
    TokenService <|.. OAuth2TokenServiceImpl : 未來擴展 (OAuth2 / SSO)
    TokenService <|.. SessionTokenServiceImpl : 未來擴展 (Session)
```

---

## 4. 類 Google 搜尋框記帳工作台與擴展設計 (Ledger UX)

### (1) 方案 A 介面與方案 B 擴展架構
- **方案 A (當前實施)**：中央搜尋列以直觀的「收支切換 + 金額 + 分類下拉 + 備註 + Enter 記帳」組成，精確無歧義。
- **方案 B (預留擴展)**：支援在同一列輸入自然語句（如 `午餐 120 飲食`），後端調用 `SmartParserService` 自動解析入庫。

```mermaid
flowchart TD
    StartInput["使用者在中央 Bar 輸入"] --> ModeCheck{"輸入模式判定"}
    
    ModeCheck -->|方案 A: 結構化表單| FormSubmit["讀取 Type / Amount / CategoryId / Description"]
    ModeCheck -->|方案 B: 自然語句 (預留)| SmartParse["送至 SmartParserService 解析正則/NLP"]
    
    SmartParse --> ValidateData["轉換為 RecordCreateRequestDto"]
    FormSubmit --> ValidateData
    
    ValidateData --> CheckAuth["校驗當前登入者 ID (UserContext)"]
    CheckAuth --> SaveDB["JPA 寫入 account_record 表"]
    SaveDB --> RefreshList["即時更新下方統計卡片與歷史流水清單"]
    RefreshList --> Toast["SweetAlert2 彈出極簡記帳成功 Toast"]
```

---

## 5. 資料庫實體關聯圖 (MS SQL Server ERD)

資料庫採用 `tibame_account`，並透過外鍵關聯落實資料完整性與使用者隔離。

```mermaid
erDiagram
    sys_user ||--o{ sys_category : "建立 (自訂分類)"
    sys_user ||--o{ account_record : "擁有 (資料隔離)"
    sys_category ||--o{ account_record : "分類關聯"

    sys_user {
        BIGINT id PK "使用者主鍵 (IDENTITY)"
        NVARCHAR username UK "登入帳號"
        VARCHAR password_hash "BCrypt 雜湊密碼"
        NVARCHAR email "電子郵件"
        NVARCHAR display_name "顯示暱稱"
        DATETIME2 created_at "建立時間"
    }

    sys_category {
        BIGINT id PK "分類主鍵 (IDENTITY)"
        BIGINT user_id FK "所屬使用者 (NULL 為系統共用預設)"
        VARCHAR type "EXPENSE (支出) / INCOME (收入)"
        NVARCHAR name "分類名稱 (如: 飲食, 交通, 薪資)"
        VARCHAR icon_code "圖標代碼"
        BIT is_system "1: 系統內建 (禁止刪除), 0: 自訂"
        INT sort_order "排序權重"
        DATETIME2 created_at "建立時間"
    }

    account_record {
        BIGINT id PK "流水帳主鍵 (IDENTITY)"
        BIGINT user_id FK "所屬使用者 (隔離查詢依據)"
        BIGINT category_id FK "分類外鍵"
        VARCHAR record_type "EXPENSE / INCOME"
        DECIMAL amount "金額 (12, 2)"
        NVARCHAR description "備註說明"
        DATE record_date "記帳日期"
        DATETIME2 created_at "建立時間"
        DATETIME2 updated_at "更新時間"
    }
```

### 資料庫 DDL 腳本 (MS SQL Server)
```sql
-- 1. 使用者表
CREATE TABLE sys_user (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    email NVARCHAR(100) NOT NULL,
    display_name NVARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- 2. 分類表 (支援預設與個人自訂)
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

-- 3. 流水帳表記錄
CREATE TABLE account_record (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    record_type VARCHAR(10) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description NVARCHAR(200) NULL,
    record_date DATE NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NULL,
    CONSTRAINT FK_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT FK_record_category FOREIGN KEY (category_id) REFERENCES sys_category(id)
);

-- 4. 系統預設分類種子資料 (Seed Data)
INSERT INTO sys_category (user_id, type, name, is_system, sort_order) VALUES
(NULL, 'EXPENSE', N'飲食聚餐', 1, 10),
(NULL, 'EXPENSE', N'交通出行', 1, 20),
(NULL, 'EXPENSE', N'日常用品', 1, 30),
(NULL, 'EXPENSE', N'居住水電', 1, 40),
(NULL, 'EXPENSE', N'休閒娛樂', 1, 50),
(NULL, 'EXPENSE', N'醫療保健', 1, 60),
(NULL, 'EXPENSE', N'其他支出', 1, 99),
(NULL, 'INCOME',  N'薪資所得', 1, 10),
(NULL, 'INCOME',  N'兼職副業', 1, 20),
(NULL, 'INCOME',  N'投資理財', 1, 30),
(NULL, 'INCOME',  N'其他收入', 1, 99);
```

---

## 6. RESTful Web API 規格定義

所有 API 均以 `/api/v1` 為前綴，並統一封裝於 `ApiResponse<T>` 格式中。

| 模組 | HTTP 方法 | 端點 (Endpoint) | 說明 | 認證要求 |
| :--- | :--- | :--- | :--- | :--- |
| **認證** | `POST` | `/api/v1/auth/register` | 使用者註冊 (帳號、密碼、信箱) | 公開 |
| **認證** | `POST` | `/api/v1/auth/login` | 使用者登入 (回傳 JWT Token) | 公開 |
| **認證** | `GET` | `/api/v1/auth/me` | 獲取當前登入者基本資訊 | Bearer JWT |
| **分類** | `GET` | `/api/v1/categories` | 查詢可用的分類清單 (系統預設 + 當前使用者自訂) | Bearer JWT |
| **分類** | `POST` | `/api/v1/categories` | 新增使用者自訂分類 | Bearer JWT |
| **分類** | `PUT` | `/api/v1/categories/{id}` | 修改自訂分類名稱/圖標 | Bearer JWT |
| **分類** | `DELETE`| `/api/v1/categories/{id}` | 刪除自訂分類 (含關聯檢查) | Bearer JWT |
| **記帳** | `GET` | `/api/v1/records` | 分頁/條件查詢流水帳 (關鍵字、日期區間、分類) | Bearer JWT |
| **記帳** | `GET` | `/api/v1/records/summary` | 獲取本月統計 (總收入、總支出、結餘) | Bearer JWT |
| **記帳** | `POST` | `/api/v1/records` | 新增一筆流水帳 (中央 Bar 提交) | Bearer JWT |
| **記帳** | `PUT` | `/api/v1/records/{id}` | 修改流水帳記錄 | Bearer JWT |
| **記帳** | `DELETE`| `/api/v1/records/{id}` | 刪除流水帳記錄 | Bearer JWT |

---

## 7. 視覺與前端規範落實 (Swiss Style & Offline)

1. **色彩盤**：
   - 核心點綴：經典瑞士紅 `#DC2626`
   - 主色調：純黑 `#111111`、深炭灰 `#262626`、背景畫布 `#F8F9FA`、卡片底色 `#FFFFFF`。
2. **排版與網格**：
   - 無襯線字體層次（Inter / Helvetica / Arial）、編號標籤（如 `SYS-LEDGER // 01`）。
   - 幾何俐落直線（`1px` / `2px` 實線邊框）、直角或微倒角（`border-radius: 0px`）、無模糊發散陰影。
3. **離線依賴 (Strict No-CDN)**：
   - 本地目錄 `src/main/resources/static/lib/` 託管 Bootstrap 5.3.3、Vue 3.4.x、Axios 1.7.x、SweetAlert2 11.x。
