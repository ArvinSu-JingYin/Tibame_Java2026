---
name: spring-boot-skills
description: "Spring Boot MVC 與 Web API 開發規範，涵蓋後端四層架構 (Repository/Service/MVC/Web API) 與純離線 (No-CDN) 前端架構 (Bootstrap 5.3, Vue 3 MVVM, Axios, SweetAlert2) 及瑞士風格 (Swiss Style) 視覺設計系統。"
---

# Spring Boot 系統架構與前端離線瑞士風格開發規範 (Spring Boot Skills)

本技能模組規範了 Spring Boot 專案之後端分層標準、RESTful Web API 與 MVC 視圖規範、純離線前端整合架構（No-CDN），以及具備客觀、理性、幾何俐落美感的**瑞士國際主義風格（Swiss Design Style）**設計系統。

---

## 1. 核心規範架構總覽

```
.agents/skills/spring-boot-skills/
├── SKILL.md                                 # 本技能核心入口與快速參考手冊
└── references/                              # 細部模組規範文件庫
    ├── backend-architecture.md              # 後端四層架構、事務邊界、模型 (DTO/VO) 與統一 API 響應
    ├── frontend-offline-architecture.md     # 前端純離線整合 (Bootstrap 5.3, Vue 3, Axios, SweetAlert2)
    ├── swiss-style-guide.md                 # 瑞士國際主義風格視覺設計、色彩、排版與 UI 元件庫
    └── coding-standards-and-dod.md          # 命名慣例、安全性規範與開發檢核清單 (Definition of Done)
```

---

## 2. 規範參考手冊導覽

| 模組規範文件 | 核心範疇與指引 | 檔案連結 |
| :--- | :--- | :--- |
| **後端分層架構規範** | 定義 `Repository`、`Service`、`MVC Controller`、`Web API Controller` 職責邊界；規定 `Entity` ↔ `DTO` ↔ `VO` 模型流轉；提供標準 `ApiResponse<T>` 與 `GlobalExceptionHandler` 實作。 | [references/backend-architecture.md](file:///c:/Arvin/COURSE/TibMe%E7%B7%AF%E8%82%B2/JAVA%20%E9%87%91%E8%9E%8D%E5%BE%AE%E6%9C%8D%E5%8B%99/Project3/.agents/skills/spring-boot-skills/references/backend-architecture.md) |
| **前端離線架構規範** | 嚴格執行 **No-CDN** 原則（所有資源置於 `resources/static/`）；整合 Bootstrap 5.3、Vue 3.0 JavaScript (Composition API) MVVM 架構；提供 `http.js` (Axios) 與 `alert.js` (SweetAlert2) 封裝模組。 | [references/frontend-offline-architecture.md](file:///c:/Arvin/COURSE/TibMe%E7%B7%AF%E8%82%B2/JAVA%20%E9%87%91%E8%9E%8D%E5%BE%AE%E6%9C%8D%E5%8B%99/Project3/.agents/skills/spring-boot-skills/references/frontend-offline-architecture.md) |
| **瑞士風格視覺指南** | 定義「形式服從功能」的設計哲學；以黑白灰中性色搭配經典瑞士紅 (`#DC2626`)；無襯線字體高對比排版、大膽留白、嚴謹非對稱網格、0px 直角幾何邊框、無模糊陰影。 | [references/swiss-style-guide.md](file:///c:/Arvin/COURSE/TibMe%E7%B7%AF%E8%82%B2/JAVA%20%E9%87%91%E8%9E%8D%E5%BE%AE%E6%9C%8D%E5%8B%99/Project3/.agents/skills/spring-boot-skills/references/swiss-style-guide.md) |
| **命名標準與 DoD 清單** | 規範後端 Class/Method 與前端 JS/CSS 命名法則；涵蓋 Jakarta Bean Validation 與 CSRF/XSS 防護；提供功能交付前的完整檢核清單 (Definition of Done)。 | [references/coding-standards-and-dod.md](file:///c:/Arvin/COURSE/TibMe%E7%B7%AF%E8%82%B2/JAVA%20%E9%87%91%E8%9E%8D%E5%BE%AE%E6%9C%8D%E5%8B%99/Project3/.agents/skills/spring-boot-skills/references/coding-standards-and-dod.md) |

---

## 3. 開發工作流程標準 (Standard Workflows)

### 3.1 後端 Web API 開發標準工作流
1. **建立 Entity**：於 `model/entity/` 建立資料表對應實體，僅限 Repository/Service 使用。
2. **建立 Repository**：於 `repository/` 繼承 `JpaRepository`，僅提供純粹 CRUD 與持久化查詢。
3. **建立 DTO 與 VO**：
   - 請求參數於 `model/dto/` 建立，全面配置 `jakarta.validation` 註解（如 `@NotBlank`, `@Size`）。
   - 回應數據於 `model/vo/` 建立，排除敏感資訊（密碼、Token）。
4. **實現 Service 邏輯**：
   - 介面置於 `service/`，實作置於 `service/impl/`。
   - 查詢標註 `@Transactional(readOnly = true)`，異動標註 `@Transactional(rollbackFor = Exception.class)`。
   - 業務校驗不符時拋出 `BusinessException`。
5. **建立 Web API Controller**：
   - 於 `controller/api/` 使用 `@RestController` 與 `@RequestMapping("/api/v1/...")`。
   - 方法使用 `@Valid` 驗證請求，一律返回 `ApiResponse<T>`。

---

### 3.2 視圖與前端 Vue 3 頁面開發標準工作流
1. **建立 MVC Controller**：
   - 於 `controller/mvc/` 使用 `@Controller`，返回視圖路徑（如 `return "pages/user/index";`）。
2. **編寫 Thymeleaf 模板**：
   - 於 `templates/` 下建立 HTML，嚴格引用本地靜態資源（`/lib/bootstrap-5.3.3/`, `/lib/vue-3.4.x/`, `/lib/axios-1.7.x/`, `/lib/sweetalert2-11.x/`, `/css/swiss-theme.css`）。
   - 頁面掛載 `<div id="app" v-cloak>`。
3. **編寫 Vue 3 頁面邏輯**：
   - 於 `static/js/pages/` 建立獨立 JS 檔，使用 `Vue.createApp({ setup() { ... } }).mount('#app')`。
   - 透過 `window.http` 呼叫後端 Web API，透過 `window.SwissAlert` 處理彈窗與提示。
4. **應用瑞士風格樣式**：
   - 容器採用 `.swiss-card`，按鈕採用 `.btn-swiss-primary` / `.btn-swiss-outline` / `.btn-swiss-danger`。
   - 標題使用 `.swiss-tag` 與 `.swiss-title`，表格採用 `.table-swiss`。

---

## 4. 核心速查代碼片段 (Cheatsheet)

### 4.1 後端 Controller 標準結構
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponseVo> getUserById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponseVo> createUser(@Valid @RequestBody UserCreateRequestDto dto) {
        return ApiResponse.ok("建立成功", userService.createUser(dto));
    }
}
```

### 4.2 前端 Vue 3 + Axios + SweetAlert2 標準結構
```javascript
(function() {
    'use strict';
    const { createApp, ref, onMounted } = Vue;

    createApp({
        setup() {
            const dataList = ref([]);
            const loading = ref(false);

            const loadData = async () => {
                loading.value = true;
                try {
                    dataList.value = await window.http.get('/users');
                } finally {
                    loading.value = false;
                }
            };

            const removeItem = async (id) => {
                const ok = await window.SwissAlert.confirm('確定要刪除此筆資料嗎？', 'DELETE');
                if (ok) {
                    await window.http.delete(`/users/${id}`);
                    window.SwissAlert.toast('已成功刪除');
                    await loadData();
                }
            };

            onMounted(loadData);
            return { dataList, loading, removeItem };
        }
    }).mount('#app');
})();
```

### 4.3 瑞士風格 CSS 核心類別速查
- **主色/邊框**：紅色 `--swiss-red: #dc2626`、黑色 `--swiss-black: #111111`、灰底 `--swiss-bg: #f8f9fa`。
- **直角卡片**：`.swiss-card` (1px solid `#e5e5e5`, 直角, 無模糊投影)。
- **按鈕**：`.btn-swiss-primary` (紅底白字/直角/黑 Hover)、`.btn-swiss-outline` (黑線框/直角/黑 Hover)、`.btn-swiss-danger` (紅字線框/紅 Hover)。
- **表格**：`.table-swiss` (2px 實心黑頂邊框, 大寫灰色表頭)。
- **標籤**：`.swiss-tag` (全大寫, 粗體, 紅色標記代碼)。

---

## 5. 相關規範與文件指引

深入學習或查閱特定模組之完整代碼與規範，請參閱 [references/](file:///c:/Arvin/COURSE/TibMe%E7%B7%AF%E8%82%B2/JAVA%20%E9%87%91%E8%9E%8D%E5%BE%AE%E6%9C%8D%E5%8B%99/Project3/.agents/skills/spring-boot-skills/references) 目錄下的各專業文檔。
