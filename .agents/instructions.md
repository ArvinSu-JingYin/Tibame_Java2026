# 系統開發與架構規範說明書 (System Architecture & Development Instructions)

本文件定義本專案（Spring Boot MVC 網站與 Web API）之基礎架構、分層職責、前端離線架構整合規範以及瑞士風格（Swiss Style）視覺設計系統。所有參與本專案之開發人員與 AI Agent 均須嚴格遵守本規範。

---

## 目錄
1. [專案概觀與核心原則](#1-專案概觀與核心原則)
2. [後端分層架構規範 (Spring Boot)](#2-後端分層架構規範-spring-boot)
   - [2.1 架構分層與職責邊界](#21-架構分層與職責邊界)
   - [2.2 建議目錄結構 (Package Structure)](#22-建議目錄結構-package-structure)
   - [2.3 數據傳遞與模型規範 (Entity / DTO / VO)](#23-數據傳遞與模型規範-entity--dto--vo)
   - [2.4 統一 API 響應格式與異常處理](#24-統一-api-響應格式與異常處理)
   - [2.5 事務與數據校驗規範](#25-事務與數據校驗規範)
3. [前端離線架構與函式庫規範](#3-前端離線架構與函式庫規範)
   - [3.1 離線架構原則 (Strict No-CDN)](#31-離線架構原則-strict-no-cdn)
   - [3.2 靜態資源目錄結構](#32-靜態資源目錄結構)
   - [3.3 核心技術棧整合指南](#33-核心技術棧整合指南)
   - [3.4 Vue 3 MVVM 頁面集成規範](#34-vue-3-mvvm-頁面集成規範)
   - [3.5 Axios 統一封裝與攔截器規範](#35-axios-統一封裝與攔截器規範)
   - [3.6 SweetAlert2 封裝與互動規範](#36-sweetalert2-封裝與互動規範)
4. [瑞士國際主義風格 (Swiss Style) 視覺設計系統](#4-瑞士國際主義風格-swiss-style-視覺設計系統)
   - [4.1 瑞士風格核心理念](#41-瑞士風格核心理念)
   - [4.2 色彩系統 (Color Palette)](#42-色彩系統-color-palette)
   - [4.3 字體與排版系統 (Typography Hierarchy)](#43-字體與排版系統-typography-hierarchy)
   - [4.4 網格與空間佈局 (Grid & Whitespace)](#44-網格與空間佈局-grid--whitespace)
   - [4.5 UI 元件樣式規範](#45-ui-元件樣式規範)
5. [命名與開發規範 (Coding Conventions)](#5-命名與開發規範-coding-conventions)
6. [開發檢核清單 (Definition of Done)](#6-開發檢核清單-definition-of-done)

---

## 1. 專案概觀與核心原則

本專案採用 **前後端分離/混合分層** 之兼顧模式：
- **後端**：基於 Spring Boot 3.x / Java 17+，實現高內聚低耦合的分層架構。區分 MVC 視圖控制器（SSR 頁面路由）與 RESTful Web API 控制器（數據接口）。
- **前端**：基於 **純離線架構（No CDN）**，所有靜態資源與第三方套件（Bootstrap 5.3、Vue 3、Axios、SweetAlert2）皆本地化部署於 `resources/static`。
- **視覺**：徹底貫徹 **瑞士國際主義風格（Swiss Design Style）**，注重客觀清晰的排版、非對稱嚴謹網格、高品質留白與極簡幾何線條。

---

## 2. 後端分層架構規範 (Spring Boot)

後端嚴格採用四層架構：**Repository ➔ Service ➔ (MVC Controller / Web API Controller)**。各層之間依賴方向為單向向下，禁止跨層調用或逆向調用。

### 2.1 架構分層與職責邊界

```
                  ┌─────────────────────────────────────┐
                  │          Client Browser             │
                  └─────────┬─────────────────┬─────────┘
                            │ (Page Request)  │ (AJAX / REST Request)
                            ▼                 ▼
   ┌───────────────────────────────────┐  ┌───────────────────────────────────┐
   │          MVC Controller           │  │        Web API Controller         │
   │  (@Controller, Thymeleaf Views)   │  │   (@RestController, JSON REST)    │
   └─────────────────┬─────────────────┘  └─────────────────┬─────────────────┘
                     │                                      │
                     └──────────────────┬───────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │            Service Layer            │
                     │  (Business Logic, Transaction, DTO) │
                     └──────────────────┬──────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │          Repository Layer           │
                     │    (Spring Data JPA / MyBatis)      │
                     └──────────────────┬──────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │         Database (RDBMS)            │
                     └─────────────────────────────────────┘
```

#### (1) Repository 層 (數據訪問層)
- **職責**：專注於資料庫 CRUD、持久化查詢、分頁與條件過濾。
- **規範**：
  - 介面繼承 `JpaRepository` / `CrudRepository` 或使用 MyBatis Mapper。
  - 僅接收與返回 **Entity** 實體或特定的 Projection 介面。
  - 嚴禁包含業務邏輯（如金額計算、權限驗證等）。
  - 自定義查詢命名遵守 Spring Data 規範（如 `findByUsernameAndStatus`）。

#### (2) Service 層 (業務邏輯層)
- **職責**：封裝核心業務邏輯、交易控制（Transaction Management）、數據轉換（Entity ↔ DTO）、跨 Repository 協調。
- **規範**：
  - 一律採用 **介面 (Interface)** 與 **實現類 (Impl)** 分離（如 `UserService` 與 `UserServiceImpl`）。
  - 事務標註 `@Transactional` 應加在 Service 實作方法上，並明確聲明 `readOnly = true` 或 Rollback 規則。
  - 拋出具備語意之業務自定義異常（如 `BusinessException`, `ResourceNotFoundException`）。
  - 對外接收 DTO，對 Controller 返回 DTO 或業務結果 VO，不將底層 Entity 直接暴露給外層。

#### (3) MVC Controller 層 (頁面視圖控制器)
- **職責**：處理瀏覽器頁面請求、渲染 SSR 模板（Thymeleaf）、頁面導向與基礎 Model 裝載。
- **規範**：
  - 使用 `@Controller` 註解。
  - 方法返回為視圖路徑字串（如 `return "pages/user/index";`）。
  - 僅調用 Service 層獲取頁面初始所需的 ViewModel/Model 數據，禁止直接調用 Repository。
  - 保持輕量，複雜動態操作交由前端 Vue 3 調用 Web API 處理。

#### (4) Web API Controller 層 (RESTful 數據控制器)
- **職責**：處理 Ajax / 前端客戶端非同步請求，提供純 JSON 數據接口。
- **規範**：
  - 使用 `@RestController` 與 `@RequestMapping("/api/v1/...")`。
  - 遵循 RESTful 設計原則（GET: 獲取, POST: 建立, PUT: 全量更新, PATCH: 局部更新, DELETE: 刪除）。
  - 輸入參數使用 `@Valid` 或 `@Validated` 結合 DTO 進行校驗。
  - 返回統一的包裝物件 `ApiResponse<T>`。

---

### 2.2 建議目錄結構 (Package Structure)

```
src/main/java/com/company/project/
├── common/                         # 公共模組
│   ├── constant/                   # 常數定義 (ErrorCode, SystemConst)
│   ├── exception/                  # 自定義異常類 (BusinessException, ApiException)
│   ├── response/                   # 統一響應封裝 (ApiResponse, PageResult)
│   └── util/                       # 工具類 (DateUtil, SecurityUtil)
├── config/                         # 系統配置 (WebMvcConfig, SecurityConfig, SwaggerConfig)
├── controller/
│   ├── mvc/                        # [MVC Controller] 頁面跳轉與視圖路由
│   │   ├── HomeController.java
│   │   └── UserViewController.java
│   └── api/                        # [Web API Controller] RESTful API 控制器
│       ├── AuthApiController.java
│       └── UserApiController.java
├── model/
│   ├── dto/                        # 請求傳輸對象 (UserCreateRequestDto, QueryDto)
│   ├── entity/                     # 資料庫持久化實體 (User.java, Role.java)
│   └── vo/                         # 視圖/響應對象 (UserResponseVo, UserSummaryVo)
├── repository/                     # [Repository] 數據持久化介面
│   └── UserRepository.java
└── service/                        # [Service] 業務邏輯介面與實現
    ├── UserService.java
    └── impl/
        └── UserServiceImpl.java
```

---

### 2.3 數據傳遞與模型規範 (Entity / DTO / VO)

| 模型類型 | 所在目錄 | 核心作用 | 規範要求 |
| :--- | :--- | :--- | :--- |
| **Entity** | `model.entity` | 映射資料庫表格結構 | 僅於 Repository 與 Service 層流轉，嚴禁直接作為 Controller 請求/響應參數 |
| **DTO (Data Transfer Object)** | `model.dto` | 接收客戶端輸入或跨系統傳輸 | 包含 `@NotBlank`, `@NotNull`, `@Size` 等校驗註解，不可包含業務邏輯 |
| **VO (View Object)** | `model.vo` | API 輸出至前端之呈現對象 | 遮蔽敏感資訊（如密碼、鹽值），結構專為前端展示最佳化 |

---

### 2.4 統一 API 響應格式與異常處理

#### (1) 統一響應包裝類 (`ApiResponse<T>`)
```java
package com.company.project.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;           // 狀態碼 (200 為成功，其餘為業務錯誤碼)
    private boolean success;    // 是否成功
    private String message;     // 提示訊息
    private T data;             // 核心承載數據
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
```

#### (2) 全域異常攔截器 (`GlobalExceptionHandler`)
```java
package com.company.project.common.exception;

import com.company.project.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.company.project.controller.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception ex) {
        String defaultMsg = "參數驗證失敗";
        if (ex instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
            defaultMsg = manv.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResponse.error(400, defaultMsg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneralException(Exception ex) {
        // Log error with Logger
        return ApiResponse.error(500, "系統發生未預期錯誤，請聯絡管理員");
    }
}
```

---

### 2.5 事務與數據校驗規範

1. **事務邊界**：
   - 事務一律聲明在 `Service` 實現類的方法級別。
   - 唯讀查詢務必標註 `@Transactional(readOnly = true)`。
   - 涉及多表異動、金流計算或多步更新之方法必須標註 `@Transactional(rollbackFor = Exception.class)`。
2. **參數校驗**：
   - DTO 必須全面使用 `jakarta.validation.constraints.*`（例如 `@NotBlank(message="名稱不可為空")`）。
   - Controller 接口入口參數務必加上 `@Valid`。

---

## 3. 前端離線架構與函式庫規範

### 3.1 離線架構原則 (Strict No-CDN)

1. **嚴禁任何外部 CDN 引用**：禁止在 HTML/Thymeleaf 模板中引入 `https://cdn.jsdelivr.net/...`、`https://unpkg.com/...`、`https://cdnjs.cloudflare.com/...` 等外部資源。
2. **完全本地化 (Self-Hosted)**：所有 CSS、JavaScript、字體檔案、圖標皆必須放置於 `src/main/resources/static/` 目錄中。
3. **離線環境可執行**：在無網際網路連線（Intranet / Air-gapped）之生產環境下，頁面佈局、樣式與互動腳本須 100% 正常運作。

---

### 3.2 靜態資源目錄結構

```
src/main/resources/static/
├── css/
│   ├── swiss-theme.css             # 瑞士風格核心變數與基礎設計系統
│   └── app.css                     # 應用程式自訂與覆寫樣式
├── js/
│   ├── app.js                      # 全域應用初始化與通用的 Vue 插件
│   ├── utils/
│   │   ├── http.js                 # Axios 統一封裝與 Request/Response 攔截
│   │   └── alert.js                # SweetAlert2 瑞士風格統一封裝
│   └── pages/                      # 各頁面獨立的 Vue Controller 腳本
│       ├── home.js
│       └── user-manage.js
├── lib/                            # 本地第三方依賴函式庫 (嚴格鎖定版本)
│   ├── bootstrap-5.3.3/
│   │   ├── css/bootstrap.min.css
│   │   └── js/bootstrap.bundle.min.js
│   ├── vue-3.4.x/
│   │   └── vue.global.prod.js      # Vue 3 獨立運行時 (IIFE)
│   ├── axios-1.7.x/
│   │   └── axios.min.js            # Axios 核心
│   └── sweetalert2-11.x/
│       ├── sweetalert2.min.css
│       └── sweetalert2.all.min.js
└── fonts/                          # 本地字型庫 (無襯線現代字體)
    └── inter/
```

---

### 3.3 核心技術棧整合指南

| 技術棧 / 函式庫 | 版本要求 | 職責與用法 |
| :--- | :--- | :--- |
| **Bootstrap** | 5.3.x | 響應式網格 (Grid)、基礎佈局 (RWD)、斷點控制、通用 Utility Classes |
| **Vue** | 3.x (Production) | MVVM 視圖數據雙向綁定、表單狀態管理、列表動態渲染、客戶端組件化 |
| **Axios** | 1.x | 非同步 API 請求、統一 Request Header 注入、統一 Error 處理 |
| **SweetAlert2** | 11.x | 互動對話方塊、操作確認 (Confirm)、Toast 提示訊息（定制為瑞士幾何風格） |

---

### 3.4 Vue 3 MVVM 頁面集成規範

在 Thymeleaf 頁面中，將 Vue 3 實例掛載至特定的容器（如 `#app`），透過 Vue 3 `Composition API` (`Vue.createApp` 與 `setup()`) 進行狀態驅動開發。

#### Thymeleaf + Vue 3 模板標準架構 (`templates/pages/user/index.html`)：
```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>使用者管理 — 瑞士風格系統</title>
    <!-- 本地 CSS 引用 (禁止 CDN) -->
    <link rel="stylesheet" th:href="@{/lib/bootstrap-5.3.3/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/lib/sweetalert2-11.x/sweetalert2.min.css}">
    <link rel="stylesheet" th:href="@{/css/swiss-theme.css}">
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body class="swiss-body">

    <!-- 主內容掛載節點 -->
    <div id="app" v-cloak class="container-fluid swiss-grid py-4">
        <!-- 頂部標題區域 (瑞士排版) -->
        <header class="swiss-header mb-5 border-bottom border-dark border-2 pb-3">
            <span class="swiss-tag">SYS-ADMIN / 01</span>
            <h1 class="swiss-title fw-bold text-uppercase mt-2">{{ pageTitle }}</h1>
        </header>

        <!-- 數據表格區域 -->
        <main class="swiss-card p-4">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h3 class="h5 fw-bold mb-0">使用者名冊</h3>
                <button class="btn btn-swiss-primary" @click="openCreateModal">
                    + 新增使用者
                </button>
            </div>

            <div class="table-responsive">
                <table class="table table-swiss align-middle">
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">姓名</th>
                            <th scope="col">電子郵件</th>
                            <th scope="col">狀態</th>
                            <th scope="col" class="text-end">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="user in userList" :key="user.id">
                            <td class="font-monospace">{{ user.id }}</td>
                            <td class="fw-semibold">{{ user.username }}</td>
                            <td>{{ user.email }}</td>
                            <td>
                                <span class="badge" :class="user.active ? 'badge-swiss-active' : 'badge-swiss-inactive'">
                                    {{ user.active ? '啟用' : '停用' }}
                                </span>
                            </td>
                            <td class="text-end">
                                <button class="btn btn-sm btn-swiss-outline me-2" @click="editUser(user)">編輯</button>
                                <button class="btn btn-sm btn-swiss-danger" @click="deleteUser(user.id)">刪除</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <!-- 本地 JS 引用 (順序：庫 -> 工具 -> 頁面腳本) -->
    <script th:src="@{/lib/bootstrap-5.3.3/js/bootstrap.bundle.min.js}"></script>
    <script th:src="@{/lib/vue-3.4.x/vue.global.prod.js}"></script>
    <script th:src="@{/lib/axios-1.7.x/axios.min.js}"></script>
    <script th:src="@{/lib/sweetalert2-11.x/sweetalert2.all.min.js}"></script>
    
    <!-- 封裝工具 -->
    <script th:src="@{/js/utils/http.js}"></script>
    <script th:src="@{/js/utils/alert.js}"></script>
    
    <!-- 當前頁面 Vue 控制邏輯 -->
    <script th:src="@{/js/pages/user-manage.js}"></script>
</body>
</html>
```

---

### 3.5 Axios 統一封裝與攔截器規範 (`/js/utils/http.js`)

```javascript
/**
 * Axios 統一封裝模組 (離線架構標準)
 */
(function(window) {
    const instance = axios.create({
        baseURL: '/api/v1',
        timeout: 10000,
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    });

    // 請求攔截器
    instance.interceptors.request.use(
        (config) => {
            // 可在此自動注入 CSRF Token 或 JWT Header
            return config;
        },
        (error) => Promise.reject(error)
    );

    // 響應攔截器
    instance.interceptors.response.use(
        (response) => {
            const res = response.data;
            // 與後端 ApiResponse 結構對齊
            if (res && typeof res.success === 'boolean') {
                if (res.success) {
                    return res.data;
                } else {
                    // 業務邏輯失敗
                    SwissAlert.error(res.message || '操作失敗');
                    return Promise.reject(new Error(res.message || 'Error'));
                }
            }
            return res;
        },
        (error) => {
            const message = error.response?.data?.message || '網路連線異常，請稍後再試';
            SwissAlert.error(message);
            return Promise.reject(error);
        }
    );

    // 掛載至全域
    window.http = instance;
})(window);
```

---

### 3.6 SweetAlert2 封裝與互動規範 (`/js/utils/alert.js`)

為維持**瑞士風格**的幾何感與簡約感，將 SweetAlert2 預設圓角改為直角/微倒角，並統一通知入口：

```javascript
/**
 * 瑞士風格 SweetAlert2 封裝工具
 */
(function(window) {
    const SwissAlert = {
        // 成功提示 Toast
        toast(message) {
            Swal.fire({
                text: message,
                icon: 'success',
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true,
                customClass: {
                    popup: 'swiss-swal-toast'
                }
            });
        },

        // 錯誤訊息視窗
        error(message, title = 'ERROR') {
            return Swal.fire({
                title: `<span class="swiss-swal-title">${title}</span>`,
                text: message,
                icon: 'error',
                confirmButtonText: '確定',
                customClass: {
                    popup: 'swiss-swal-popup',
                    confirmButton: 'btn btn-swiss-primary px-4'
                },
                buttonsStyling: false
            });
        },

        // 確認對話框
        confirm(message, title = 'CONFIRMATION') {
            return Swal.fire({
                title: `<span class="swiss-swal-title">${title}</span>`,
                text: message,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: '執行',
                cancelButtonText: '取消',
                customClass: {
                    popup: 'swiss-swal-popup',
                    confirmButton: 'btn btn-swiss-danger px-4 me-2',
                    cancelButton: 'btn btn-swiss-outline px-4'
                },
                buttonsStyling: false
            }).then(result => result.isConfirmed);
        }
    };

    window.SwissAlert = SwissAlert;
})(window);
```

---

## 4. 瑞士國際主義風格 (Swiss Style) 視覺設計系統

瑞士風格（International Typographic Style / Swiss Style）源於 1950 年代，強調 **客觀、理性、清晰、高對比、非裝飾性**。

### 4.1 瑞士風格核心理念
1. **形式服從功能 (Form follows function)**：摒棄所有無意義的陰影、漸變與花俏裝飾。
2. **排版為核心 (Typographic Clarity)**：依靠無襯線字體大小對比、字重（Bold vs Regular）、字距與行距傳遞結構。
3. **極致非對稱網格 (Asymmetrical Grid)**：利用數學邏輯比例切分頁面區塊，維持穩固且生動的版面平衡。
4. **大膽留白 (Generous Negative Space)**：留白本身就是核心構圖元素，確保資訊呼吸感與可讀性。
5. **幾何俐落線條 (Sharp Borders & Lines)**：邊框以 1px 或 2px 純色直線為主，圓角介於 `0px` 至 `2px`。

---

### 4.2 色彩系統 (Color Palette)

瑞士風格以高純度的黑白灰中性色為基底，搭配具強烈視覺焦點的 **經典瑞士紅 (Swiss Red)**：

```
+-------------------------------------------------------------------------+
| SWISS DESIGN COLOR SPECIFICATION                                       |
+-------------------------------------------------------------------------+
|  Primary Swiss Red    : #DC2626 (Pantone 485C / Vibrant Accent)         |
|  Absolute Black       : #111111 (High-contrast typography)              |
|  Deep Charcoal        : #262626 (Subtitles & Strong Borders)            |
|  Slate Grey           : #737373 (Auxiliary text & Metadata)             |
|  Concrete Neutral     : #E5E5E5 (Divider lines & Grid borders)          |
|  Off-White Paper      : #F8F9FA (Background Canvas)                     |
|  Pure White           : #FFFFFF (Card Surface)                          |
+-------------------------------------------------------------------------+
```

#### CSS 設計變數 (`/css/swiss-theme.css`):
```css
:root {
    /* 瑞士風格核心色盤 */
    --swiss-red: #dc2626;
    --swiss-red-hover: #b91c1c;
    --swiss-black: #111111;
    --swiss-dark: #262626;
    --swiss-grey: #737373;
    --swiss-light-grey: #e5e5e5;
    --swiss-bg: #f8f9fa;
    --swiss-white: #ffffff;

    /* 字體與排版 */
    --swiss-font-family: -apple-system, BlinkMacSystemFont, "Inter", "Helvetica Neue", Arial, "Noto Sans TC", sans-serif;
    --swiss-font-mono: "SFMono-Regular", Menlo, Monaco, Consolas, "Liberation Mono", monospace;

    /* 邊框與圓角 (幾何俐落) */
    --swiss-border-width: 1px;
    --swiss-border-bold: 2px;
    --swiss-border-color: var(--swiss-light-grey);
    --swiss-radius: 0px; /* 堅持直角幾何美學 */
}
```

---

### 4.3 字體與排版系統 (Typography Hierarchy)

1. **字體選擇**：無襯線字體（Helvetica Neue, Arial, Inter, Noto Sans TC）。
2. **標題排版**：大寫英文字母標記（Uppercase Tags）、嚴格字重對比。
3. **序號標籤 (Numbered Indexing)**：關鍵區塊採用如同海報設計的編號（如 `01 / OVERVIEW`, `SYS-USER // 04`）。

```css
/* 瑞士排版樣式範例 */
.swiss-tag {
    display: inline-block;
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--swiss-red);
    margin-bottom: 0.25rem;
}

.swiss-title {
    font-family: var(--swiss-font-family);
    font-weight: 800;
    letter-spacing: -0.03em;
    color: var(--swiss-black);
}
```

---

### 4.4 網格與空間佈局 (Grid & Whitespace)

- 採用 Bootstrap 5 的 12 欄位網格系統，但外層加入清晰的幾何隔線。
- 使用欄位間距（Gutter）與大留白（`py-5`, `mb-5`）強化結構分明感。

---

### 4.5 UI 元件樣式規範

#### (1) 按鈕 (Buttons)
- 直角或極微小倒角（`border-radius: 0`）。
- 高對比純色，懸停時進行高反差反轉或實色填滿。

```css
.btn-swiss-primary {
    background-color: var(--swiss-red);
    color: var(--swiss-white);
    border: 1px solid var(--swiss-red);
    border-radius: var(--swiss-radius);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: 0.5rem 1.25rem;
    transition: all 0.15s ease-in-out;
}
.btn-swiss-primary:hover {
    background-color: var(--swiss-black);
    border-color: var(--swiss-black);
    color: var(--swiss-white);
}

.btn-swiss-outline {
    background-color: transparent;
    color: var(--swiss-black);
    border: 1px solid var(--swiss-black);
    border-radius: var(--swiss-radius);
    font-weight: 600;
    padding: 0.5rem 1.25rem;
}
.btn-swiss-outline:hover {
    background-color: var(--swiss-black);
    color: var(--swiss-white);
}
```

#### (2) 卡片與容器 (Cards & Containers)
- 純白背景、灰色細邊框（1px solid `#e5e5e5`），**不使用模糊陰影**（No soft drop-shadow）。

```css
.swiss-card {
    background-color: var(--swiss-white);
    border: 1px solid var(--swiss-light-grey);
    border-radius: var(--swiss-radius);
    box-shadow: none;
}
```

#### (3) 數據表格 (Data Tables)
- 頂部使用粗黑邊框（2px solid `#111111`），列間使用 1px 淺灰隔線。
- 表頭（`thead`）文字大寫、字距微寬。

```css
.table-swiss {
    border-top: 2px solid var(--swiss-black);
    margin-bottom: 0;
}
.table-swiss th {
    font-weight: 700;
    text-transform: uppercase;
    font-size: 0.8125rem;
    letter-spacing: 0.05em;
    color: var(--swiss-black);
    border-bottom: 1px solid var(--swiss-black);
    background-color: var(--swiss-bg);
    padding: 0.75rem 1rem;
}
.table-swiss td {
    padding: 0.875rem 1rem;
    border-bottom: 1px solid var(--swiss-light-grey);
}
```

---

## 5. 命名與開發規範 (Coding Conventions)

### 5.1 後端命名規範
- **套件命名**：全部小寫單數，如 `controller.api`, `model.dto`。
- **類別命名**：大駝峰式（UpperCamelCase），並帶有明確後綴：
  - Controller: `*Controller` (例如 `UserViewController`, `UserApiController`)
  - Service: 介面 `*Service`，實現類 `*ServiceImpl`
  - Repository: `*Repository`
  - 實體類: `User`, `Role` (對應數據表單數)
  - 傳輸對象: `*RequestDto`, `*ResponseVo`
- **方法命名**：小駝峰式（lowerCamelCase），動詞開頭（`getUserById`, `createOrder`）。

### 5.2 前端命名規範
- **檔案命名**：kebab-case（如 `user-manage.js`, `swiss-theme.css`）。
- **Vue 變數與方法**：
  - 狀態變數: 小駝峰式（`userList`, `isLoading`, `formData`）
  - 事件處理: `handle*` 或 `on*`（`handleSubmit`, `handleDelete`）
  - API 調用方法: `fetch*`, `save*`, `remove*`（`fetchUserList`, `saveUser`）

---

## 6. 開發檢核清單 (Definition of Done)

在提交任何新功能或代碼異動前，必須通過以下檢核：

- [ ] **架構分界**：MVC Controller 與 Web API Controller 職責清晰分離，無跨層直接依賴 Repository。
- [ ] **數據契約**：API 均封裝為標準 `ApiResponse<T>`，輸入參數皆有 DTO 校驗。
- [ ] **離線完整性**：全局無任何外部 CDN 連結，於斷網狀態下頁面樣式與互動腳本正常。
- [ ] **視覺風格**：遵循瑞士風格設計系統（直角/極簡邊框、無裝飾性陰影、經典瑞士紅色調、層次清晰的無襯線字體排版）。
- [ ] **互動回饋**：非同步操作完成均採用 SweetAlert2 進行統一風格的提示反饋。
