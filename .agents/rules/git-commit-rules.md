# Git Commit Message 規範 (Spring Boot & OpenSpec)

本專案統一採用基於 **Conventional Commits 1.0.0** 之規範，結合 **Spring 架構層級 (Scope)** 與 **繁體中文** 書寫，並強制/建議附帶 **OpenSpec 變更關聯 (Opsx)**。

---

## 1. 訊息格式結構

```text
<type>(<scope>): <繁體中文簡述>

[選填/複雜變更必填: 詳細變更說明]
- 條列具體變更細節與邏輯
- 說明為什麼要進行此調整

Opsx: <change-name>
```

---

## 2. Type 類型定義

| Type | 說明 | 常用繁體中文動詞 |
| :--- | :--- | :--- |
| `feat` | 新增功能 (Feature) | 新增、實作、支援 |
| `fix` | 錯誤修復 (Bug fix) | 修復、修正、解決 |
| `refactor` | 代碼重構 (無功能變動) | 重構、抽取、簡化 |
| `perf` | 效能優化 (Performance) | 優化、提升、加速 |
| `test` | 測試相關 | 新增、補充、調整 |
| `style` | 格式調整 (不影響邏輯) | 格式化、調整排版、修正縮排 |
| `docs` | 文檔變更 | 撰寫、更新、補齊 |
| `chore` | 建構/輔助工具變更 | 升級、配置、調整 |
| `revert` | 還原提交 | 還原、回退 |

---

## 3. Scope (Spring 架構分層)

所有 Scope 請統一使用**小寫字母**：

- `controller`: Web API / MVC 控制層、路由與請求接收 (`*Controller.java`)
- `service`: 業務邏輯層、交易控制 (`*Service.java`, `*ServiceImpl.java`)
- `repository`: 資料庫存取層、Spring Data JPA / MyBatis (`*Repository.java`, `*Dao.java`)
- `entity`: 資料庫實體模型 (`*Entity.java`, `@Entity`)
- `dto`: 資料傳輸物件、Request / Response VO、驗證規則 (`*DTO.java`, `*Req.java`, `*Resp.java`)
- `config`: Spring 配置類、Bean 宣告、屬性設定 (`*Config.java`, `application.yml`)
- `security`: 認證與授權、JWT、Filter (`SecurityConfig.java`, `*Filter.java`)
- `exception`: 全域例外處理、錯誤代碼映射 (`GlobalExceptionHandler.java`)
- `view`: 前端視圖、Thymeleaf、Vue 3 模板與靜態資源 (`*.html`, `*.js`, `*.css`)
- `common`: 工具類別、常數定義、共用組件 (`*Utils.java`, `*Constants.java`)
- `build`: Maven 依賴、打包設定、CI/CD (`pom.xml`, `.gitignore`)
- `specs`: OpenSpec 規格與變更檔案 (`openspec/`)

---

## 4. Subject (簡述) 書寫規則

1. **語系**：一律使用**繁體中文**。
2. **動詞開頭**：以動作動詞開始（例如：`新增`、`修復`、`優化`、`重構`、`調整`）。
3. **長度限制**：首行不超過 **72 字元**。
4. **結尾標點**：結尾**不加**句號（`.` 或 `。`）。

---

## 5. Body & Footer 規則

- **Body (選填/複雜變更)**：以條列式 `- ` 說明重要變更點與邏輯。
- **Footer (OpenSpec 關聯)**：有對應 OpenSpec change 時，必須加入 `Opsx: <change-name>`（例如：`Opsx: daily-ledger-system`）。

---

## 6. 生成範例 (Examples)

### 範例 A：新增 Controller
```text
feat(controller): 新增帳本明細分頁查詢與匯出 API

- 實作 GET /api/v1/ledgers 支援分頁與多條件過濾
- 實作 POST /api/v1/ledgers/export 支援 Excel 異步匯出

Opsx: daily-ledger-system
```

### 範例 B：修復 Service 交易邏輯
```text
fix(service): 修正餘額扣除計算錯誤與交易回滾失效問題

- 在 LedgerService.deduct() 加入 @Transactional 確保原子性
- 修正扣款金額大於餘額時未拋出 InsufficientBalanceException 的問題

Opsx: daily-ledger-system
```

### 範例 C：優化 JPA 查詢
```text
perf(repository): 優化帳目關聯查詢避免 JPA N+1 問題

- 在 LedgerRepository 查詢中使用 @EntityGraph 預先載入 Category

Opsx: daily-ledger-system
```
