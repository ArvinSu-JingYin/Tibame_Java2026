# 3. 系統技術架構與設計 (Architecture & Technical Design)

> **專案代號**：`daily-ledger-system`  
> **所屬模組**：系統分層架構與類別設計  
> **相關基準**：[OpenSpec Specs](../../../openspec/specs)

---

## 1. 系統總體四層分層架構

系統嚴格遵循 Spring Boot 四層架構原則，層與層之間職責分離、依賴單向：

```mermaid
graph TD
    subgraph PresentationLayer ["1. 控制器層 (Controller Layer)"]
        MVC["Thymeleaf MVC Controller<br/>ViewController (頁面路由導向)"]
        API1["AuthApiController (/api/v1/auth)"]
        API2["CategoryApiController (/api/v1/categories)"]
        API3["RecordApiController (/api/v1/records)"]
    end

    subgraph SecurityLayer ["2. 安全攔截與上下文層 (Security Layer)"]
        Filter["JwtAuthenticationFilter (OncePerRequestFilter)"]
        Context["CurrentUserContext (ThreadLocal 封裝)"]
        Principal["UserPrincipal (當前使用者身分)"]
        Filter --> Context
        Context --> Principal
    end

    subgraph ServiceLayer ["3. 業務邏輯層 (Service Layer @Service)"]
        AuthSvc["AuthServiceImpl (@Transactional)"]
        CatSvc["CategoryServiceImpl (@Transactional)"]
        LedgerSvc["LedgerServiceImpl (@Transactional)"]
        ParserSvc["SmartParserService (方案 B 自然語言擴展介面)"]
    end

    subgraph RepositoryLayer ["4. 數據訪問層 (Repository Layer @Repository)"]
        UserRepo["UserRepository (Spring Data JPA)"]
        CatRepo["CategoryRepository (Spring Data JPA)"]
        RecordRepo["AccountRecordRepository (Spring Data JPA)"]
    end

    subgraph DatabaseLayer ["5. 資料庫層 (MS SQL Server)"]
        MSSQL[("tibame_account (localhost:1433)<br/>sys_user / sys_category / account_record")]
    end

    MVC --> ServiceLayer
    API1 --> Filter
    API2 --> Filter
    API3 --> Filter
    Filter --> ServiceLayer
    AuthSvc --> UserRepo
    CatSvc --> CatRepo
    LedgerSvc --> RecordRepo
    ParserSvc -.-> LedgerSvc
    UserRepo --> MSSQL
    CatRepo --> MSSQL
    RecordRepo --> MSSQL
```

---

## 2. 輸入擴展架構：方案 A (結構化) 與 方案 B (自然語言)

```mermaid
flowchart LR
    Input["使用者在中央 Bar 輸入"] --> ModeCheck{"輸入模式判定"}
    
    ModeCheck -->|當前方案 A: 結構化表單| StructForm["讀取 Type + Amount + CategoryId + Description + Date"]
    ModeCheck -->|預留方案 B: 自然語句| SmartParser["SmartParserService 正則與 NLP 解析器<br/>(例如: '午餐 120 飲食')"]
    
    StructForm --> DTO["RecordCreateRequestDto"]
    SmartParser --> DTO
    
    DTO --> Service["LedgerService.createRecord()"]
    Service --> SecurityCheck["校驗 UserContext.getCurrentUserId()"]
    SecurityCheck --> Save["AccountRecordRepository.save()"]
    Save --> UpdateView["觸發前端即時統計與流水列表刷新"]
```

---

## 3. 可插拔 Token 服務類別設計

系統定義 `TokenService` 介面，預設由 `JwtTokenServiceImpl` 實現無狀態 JWT 簽發與校驗；未來可無縫切換為 OAuth2 / SSO 實作。

```mermaid
classDiagram
    class TokenService {
        <<interface>>
        +generateToken(UserPrincipal principal) String
        +validateToken(String token) boolean
        +getUserIdFromToken(String token) Long
        +getUsernameFromToken(String token) String
    }
    class JwtTokenServiceImpl {
        -String secretKey
        -Long expirationMs
        +generateToken(UserPrincipal principal) String
        +validateToken(String token) boolean
        +getUserIdFromToken(String token) Long
        +getUsernameFromToken(String token) String
    }
    class OAuth2TokenServiceImpl {
        +generateToken(UserPrincipal principal) String
        +validateToken(String token) boolean
    }

    TokenService <|.. JwtTokenServiceImpl : 當前核心實作
    TokenService <|.. OAuth2TokenServiceImpl : 未來擴展 (SSO/OAuth2)
```
