# 前端離線架構與整合規範 (Frontend Offline Architecture)

本文件詳細規範前端**純離線架構 (Strict No-CDN)**、靜態資源目錄結構、核心前端技術棧（Bootstrap 5.3、Vue 3.0 JavaScript MVVM、Axios、SweetAlert2）之本地化整合指南與標準實作代碼。

---

## 1. 離線架構核心原則 (Strict No-CDN Policy)

1. **嚴禁任何外部 CDN 引用**：
   - 嚴格禁止在 HTML / Thymeleaf 模板中引入任何來自外網的鏈接（如 `cdn.jsdelivr.net`, `unpkg.com`, `cdnjs.cloudflare.com`, `fonts.googleapis.com` 等）。
2. **所有資源本地化 (100% Self-Hosted)**：
   - 包含 CSS 樣式檔、JavaScript 腳本、第三方套件庫、網頁字型（Web Fonts）及圖標皆必須放置於 `src/main/resources/static/` 目錄中。
3. **無網際網路環境支援 (Air-Gapped Ready)**：
   - 確保在完全斷網或純內網（Intranet / Air-gapped）環境下，系統之佈局排版、動態渲染、資料請求與彈窗互動均能 100% 正常運行。

---

## 2. 靜態資源目錄結構規範

```
src/main/resources/static/
├── css/
│   ├── swiss-theme.css             # 瑞士風格核心變數與基礎設計系統
│   └── app.css                     # 應用程式自訂與業務覆寫樣式
├── js/
│   ├── app.js                      # 全域應用初始化與通用的 Vue 插件/過濾器
│   ├── utils/
│   │   ├── http.js                 # Axios 統一封裝與 Request/Response 攔截
│   │   └── alert.js                # SweetAlert2 瑞士風格統一封裝
│   └── pages/                      # 各頁面獨立的 Vue 3 Controller 腳本
│       ├── home.js
│       └── user-manage.js
├── lib/                            # 本地第三方依賴函式庫 (嚴格鎖定版本)
│   ├── bootstrap-5.3.3/
│   │   ├── css/bootstrap.min.css
│   │   └── js/bootstrap.bundle.min.js
│   ├── vue-3.4.x/
│   │   └── vue.global.prod.js      # Vue 3 獨立運行時 (IIFE 格式)
│   ├── axios-1.7.x/
│   │   └── axios.min.js            # Axios 核心
│   └── sweetalert2-11.x/
│       ├── sweetalert2.min.css
│       └── sweetalert2.all.min.js
└── fonts/                          # 本地字型庫 (無襯線現代字體)
    └── inter/
```

---

## 3. 核心技術棧整合指南

| 技術棧 / 函式庫 | 推薦版本 | 核心職責與使用範疇 |
| :--- | :--- | :--- |
| **Bootstrap** | 5.3.x | 響應式網格 (12-column Grid)、RWD 斷點控制、排版輔助 Utilities (Flexbox, Spacing) |
| **Vue** | 3.x (Production) | MVVM 雙向數據綁定、響應式狀態管理、表單驗證、動態列表渲染、非同步操作視圖更新 |
| **Axios** | 1.7.x | 處理 Ajax 非同步 HTTP 請求、注入 Request Header、對齊後端 `ApiResponse` 結構 |
| **SweetAlert2** | 11.x | 操作結果 Toast 提示、刪除/重要動作 Confirmation 詢問框、錯誤告警視窗 |

---

## 4. Axios 統一封裝標準 (`/js/utils/http.js`)

封裝原則：
1. 自動附加 `X-Requested-With` 與基礎 `baseURL`。
2. 響應攔截器自動解構後端 `ApiResponse`：成功時直接返回 `res.data`，失敗時自動調用 `SwissAlert.error` 並返回 `Promise.reject`。
3. 統一捕獲 HTTP 400/401/403/404/500 等網絡或伺服器異常。

```javascript
/**
 * Axios 統一封裝模組 (離線架構標準)
 * 掛載至 window.http
 */
(function(window) {
    'use strict';

    if (typeof axios === 'undefined') {
        console.error('未找到 Axios 函式庫，請確認 /lib/axios-1.7.x/axios.min.js 已正確載入');
        return;
    }

    const http = axios.create({
        baseURL: '/api/v1',
        timeout: 15000,
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    });

    // 請求攔截器
    http.interceptors.request.use(
        (config) => {
            // 可在此自動注入 CSRF Token (若 Spring Security 啟用)
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            if (csrfToken && csrfHeader) {
                config.headers[csrfHeader] = csrfToken;
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    // 響應攔截器
    http.interceptors.response.use(
        (response) => {
            const res = response.data;

            // 後端標準 ApiResponse 結構校驗
            if (res && typeof res.success === 'boolean') {
                if (res.success) {
                    return res.data; // 直接提供業務數據給呼叫端
                } else {
                    // 業務邏輯自定義錯誤處理
                    const errorMsg = res.message || '操作未成功完成';
                    if (window.SwissAlert) {
                        window.SwissAlert.error(errorMsg);
                    } else {
                        alert(errorMsg);
                    }
                    return Promise.reject(new Error(errorMsg));
                }
            }
            return res;
        },
        (error) => {
            let errorMsg = '網路連線異常，請稍後再試';
            if (error.response) {
                const status = error.response.status;
                const data = error.response.data;

                if (data && data.message) {
                    errorMsg = data.message;
                } else if (status === 400) {
                    errorMsg = '請求參數錯誤 (400)';
                } else if (status === 401) {
                    errorMsg = '未授權或登入已逾期，請重新登入 (401)';
                } else if (status === 403) {
                    errorMsg = '您無權執行此操作 (403)';
                } else if (status === 404) {
                    errorMsg = '請求的資源不存在 (404)';
                } else if (status >= 500) {
                    errorMsg = '伺服器內部錯誤，請聯絡系統管理員 (500)';
                }
            } else if (error.code === 'ECONNABORTED') {
                errorMsg = '請求超時，請檢查網絡連線';
            }

            if (window.SwissAlert) {
                window.SwissAlert.error(errorMsg);
            } else {
                alert(errorMsg);
            }
            return Promise.reject(error);
        }
    );

    window.http = http;
})(window);
```

---

## 5. SweetAlert2 瑞士風格封裝 (`/js/utils/alert.js`)

封裝原則：
1. 將 SweetAlert2 的外觀改造為**瑞士風格**（直角邊框、高對比色、大膽標題排版）。
2. 提供三個簡潔標準方法：`toast(message)`、`error(message, title)`、`confirm(message, title)`。

```javascript
/**
 * 瑞士風格 SweetAlert2 封裝工具
 * 掛載至 window.SwissAlert
 */
(function(window) {
    'use strict';

    if (typeof Swal === 'undefined') {
        console.error('未找到 SweetAlert2 函式庫，請確認 /lib/sweetalert2-11.x/ 已正確載入');
        return;
    }

    const SwissAlert = {
        /**
         * 成功通知 Toast
         * @param {string} message 提示訊息
         */
        toast(message) {
            return Swal.fire({
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

        /**
         * 錯誤提示視窗
         * @param {string} message 錯誤訊息
         * @param {string} title 標題代碼 (預設 ERROR)
         */
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

        /**
         * 操作確認對話方塊
         * @param {string} message 確認提示訊息
         * @param {string} title 標題 (預設 CONFIRMATION)
         * @returns {Promise<boolean>} 用戶是否確認
         */
        confirm(message, title = 'CONFIRMATION') {
            return Swal.fire({
                title: `<span class="swiss-swal-title">${title}</span>`,
                text: message,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: '確認執行',
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

## 6. Vue 3 MVVM 頁面集成規範

### 6.1 HTML / Thymeleaf 模板標準結構 (`templates/pages/user/index.html`)

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>使用者管理 — 瑞士風格系統</title>

    <!-- [STRICT OFFLINE] 本地 CSS 依賴引入 -->
    <link rel="stylesheet" th:href="@{/lib/bootstrap-5.3.3/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/lib/sweetalert2-11.x/sweetalert2.min.css}">
    <link rel="stylesheet" th:href="@{/css/swiss-theme.css}">
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body class="swiss-body">

    <!-- Vue 3 主掛載節點 -->
    <div id="app" v-cloak class="container-fluid swiss-grid py-4">
        
        <!-- 頁首區域 (瑞士排版) -->
        <header class="swiss-header mb-5 border-bottom border-dark border-2 pb-3">
            <span class="swiss-tag">SYS-ADMIN // 01</span>
            <h1 class="swiss-title fw-bold text-uppercase mt-2">{{ pageTitle }}</h1>
        </header>

        <!-- 數據展示與操作區 -->
        <main class="swiss-card p-4">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3 class="h5 fw-bold mb-1">系統用戶清單</h3>
                    <p class="text-muted small mb-0">共 {{ userList.length }} 筆記錄</p>
                </div>
                <button class="btn btn-swiss-primary" @click="openCreateModal">
                    + 新增用戶
                </button>
            </div>

            <!-- 載入中狀態 -->
            <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-dark" role="status">
                    <span class="visually-hidden">載入中...</span>
                </div>
            </div>

            <!-- 數據表格 -->
            <div v-else class="table-responsive">
                <table class="table table-swiss align-middle">
                    <thead>
                        <tr>
                            <th scope="col">編號</th>
                            <th scope="col">帳號</th>
                            <th scope="col">電子郵件</th>
                            <th scope="col">狀態</th>
                            <th scope="col" class="text-end">操作選項</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="user in userList" :key="user.id">
                            <td class="font-monospace">{{ user.id }}</td>
                            <td class="fw-bold">{{ user.username }}</td>
                            <td>{{ user.email }}</td>
                            <td>
                                <span class="badge" :class="user.active ? 'badge-swiss-active' : 'badge-swiss-inactive'">
                                    {{ user.active ? '啟用中' : '已停用' }}
                                </span>
                            </td>
                            <td class="text-end">
                                <button class="btn btn-sm btn-swiss-outline me-2" @click="editUser(user)">編輯</button>
                                <button class="btn btn-sm btn-swiss-danger" @click="deleteUser(user.id)">刪除</button>
                            </td>
                        </tr>
                        <tr v-if="userList.length === 0">
                            <td colspan="5" class="text-center py-4 text-muted">目前暫無數據</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <!-- [STRICT OFFLINE] 本地 JS 依賴 (依順序載入) -->
    <script th:src="@{/lib/bootstrap-5.3.3/js/bootstrap.bundle.min.js}"></script>
    <script th:src="@{/lib/vue-3.4.x/vue.global.prod.js}"></script>
    <script th:src="@{/lib/axios-1.7.x/axios.min.js}"></script>
    <script th:src="@{/lib/sweetalert2-11.x/sweetalert2.all.min.js}"></script>

    <!-- 工具層 -->
    <script th:src="@{/js/utils/alert.js}"></script>
    <script th:src="@{/js/utils/http.js}"></script>

    <!-- 當前頁面 Vue 實例 -->
    <script th:src="@{/js/pages/user-manage.js}"></script>
</body>
</html>
```

---

### 6.2 頁面 Vue 3 腳本實作規範 (`/js/pages/user-manage.js`)

```javascript
/**
 * 使用者管理頁面 Vue 3 Composition API 控制腳本
 */
(function() {
    'use strict';

    const { createApp, ref, onMounted } = Vue;

    createApp({
        setup() {
            // 響應式狀態
            const pageTitle = ref('使用者管理');
            const loading = ref(false);
            const userList = ref([]);

            // 取得使用者清單
            const fetchUsers = async () => {
                loading.value = true;
                try {
                    const data = await window.http.get('/users');
                    userList.value = data.content || data || [];
                } catch (err) {
                    console.error('查詢失敗:', err);
                } finally {
                    loading.value = false;
                }
            };

            // 刪除使用者
            const deleteUser = async (id) => {
                const confirmed = await window.SwissAlert.confirm(
                    `您確定要刪除編號 [${id}] 的使用者嗎？此操作無法還原。`,
                    'DELETE USER'
                );

                if (confirmed) {
                    try {
                        await window.http.delete(`/users/${id}`);
                        window.SwissAlert.toast('使用者已成功刪除');
                        await fetchUsers();
                    } catch (err) {
                        console.error('刪除失敗:', err);
                    }
                }
            };

            const openCreateModal = () => {
                // 開啟新增視窗或表單
            };

            const editUser = (user) => {
                // 開啟編輯視窗
            };

            onMounted(() => {
                fetchUsers();
            });

            return {
                pageTitle,
                loading,
                userList,
                fetchUsers,
                deleteUser,
                openCreateModal,
                editUser
            };
        }
    }).mount('#app');
})();
```
