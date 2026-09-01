# 瑞士國際主義風格視覺設計系統 (Swiss Design Style Guide)

本文件定義系統之**瑞士國際主義風格（International Typographic Style / Swiss Style）**視覺設計規範。系統內所有頁面、UI 元件、字體排版、配色與微互動皆須嚴格遵循本規範，確保介面呈現客觀、嚴謹、清晰且極具現代美感的專業視覺體驗。

---

## 1. 瑞士風格五大核心理念

1. **形式服從功能 (Form Follows Function)**：
   - 摒棄任何無功能的裝飾、過度漸層與模糊毛玻璃陰影。介面上的每一個元素均服務於資訊傳遞。
2. **字體排版即設計 (Typography as Primary Graphic)**：
   - 使用高度可讀的現代無襯線字體（Sans-serif）。透過字重對比（Bold vs Regular）、大小階梯、全大寫標記（Uppercase Tagging）與字距建立鮮明層次。
3. **嚴謹非對稱網格 (Asymmetrical Grid Structure)**：
   - 依據數學比例構建版面，區塊間擁有明確對齊軸線，展現穩定而生動的結構美。
4. **大膽負空間與留白 (Generous Negative Space)**：
   - 留白並非空白，而是核心構圖載體，賦予使用者呼吸感與專注力。
5. **俐落幾何線條 (Sharp Geometric Linework)**：
   - 堅持 **直角（0px 圓角）** 或極微倒角，以 1px / 2px 純色實心直線劃分資訊層級。

---

## 2. 色彩系統 (Color Palette)

瑞士風格以黑、白、灰高對比中性色為基礎，並輔以極具辨識度與聚焦力的 **經典瑞士紅 (Swiss Red)**：

```
+-----------------------------------------------------------------------------+
| SWISS DESIGN SYSTEM COLOR PALETTE                                           |
+-----------------------------------------------------------------------------+
|  Primary Swiss Red    : #DC2626 (Pantone 485C / 活力焦點色、警示與強調)      |
|  Absolute Black       : #111111 (高對比主標題、粗邊框、重點按鈕)            |
|  Deep Dark Charcoal   : #262626 (正文內文、次級標題)                        |
|  Slate Grey           : #737373 (輔助文字、元數據標籤、次要描述)             |
|  Concrete Neutral     : #E5E5E5 (隔線、表格水平分界、卡片細邊框)             |
|  Off-White Paper      : #F8F9FA (頁面基底背景色)                            |
|  Pure White           : #FFFFFF (卡片容器表面、彈窗背景)                    |
+-----------------------------------------------------------------------------+
```

---

## 3. 字體與排版階層 (Typography Hierarchy)

### 3.1 字體族系 (Font Stack)
優先載入本地無襯線現代字體：
```css
font-family: -apple-system, BlinkMacSystemFont, "Inter", "Helvetica Neue", Arial, "Noto Sans TC", sans-serif;
font-family-mono: "SFMono-Regular", Menlo, Monaco, Consolas, "Liberation Mono", monospace;
```

### 3.2 階層定義規範
- **分類代碼標籤 (Category Tag)**：全大寫、`0.75rem`、字重 700、寬字距（`letter-spacing: 0.1em`）、紅色或黑色。
- **主標題 (H1)**：`2.25rem` ~ `2.5rem`、字重 800、緊湊字距（`letter-spacing: -0.03em`）、純黑 `#111111`。
- **次標題 (H2 / H3)**：`1.25rem` ~ `1.5rem`、字重 700、純黑或深炭灰。
- **序列編號 (Numbered Indices)**：在主要區塊前加入海報風格的幾何編號（例如 `01 / OVERVIEW`, `SEC // 03`）。

---

## 4. UI 元件樣式規範

### 4.1 按鈕系統 (Buttons)
- **形狀**：堅持直角（`border-radius: 0`）。
- **互動**：懸停（Hover）時採用高反差反轉或實色填滿，過渡時間為 `0.15s`。

```css
/* 主行動按鈕 (Swiss Primary) */
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

/* 次要線框按鈕 (Swiss Outline) */
.btn-swiss-outline {
    background-color: transparent;
    color: var(--swiss-black);
    border: 1px solid var(--swiss-black);
    border-radius: var(--swiss-radius);
    font-weight: 600;
    padding: 0.5rem 1.25rem;
    transition: all 0.15s ease-in-out;
}
.btn-swiss-outline:hover {
    background-color: var(--swiss-black);
    color: var(--swiss-white);
}

/* 危險/刪除按鈕 (Swiss Danger) */
.btn-swiss-danger {
    background-color: #ffffff;
    color: var(--swiss-red);
    border: 1px solid var(--swiss-red);
    border-radius: var(--swiss-radius);
    font-weight: 600;
    padding: 0.5rem 1.25rem;
    transition: all 0.15s ease-in-out;
}
.btn-swiss-danger:hover {
    background-color: var(--swiss-red);
    color: var(--swiss-white);
}
```

---

### 4.2 卡片容器 (Cards & Containers)
- 純白背景、1px 淺灰色實線邊框，**嚴禁使用模糊投影（No box-shadow）**。

```css
.swiss-card {
    background-color: var(--swiss-white);
    border: 1px solid var(--swiss-light-grey);
    border-radius: var(--swiss-radius);
    box-shadow: none;
}
```

---

### 4.3 數據表格 (Data Tables)
- 頂部使用 2px 實心黑色粗邊框。
- 表頭（`thead`）文字大寫、字距微寬、淺灰背景。
- 列（Row）懸停時呈現極淺的灰色回饋。

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
    font-size: 0.9375rem;
}
.table-swiss tbody tr:hover {
    background-color: #f1f3f5;
}
```

---

### 4.4 標籤徽章 (Badges)
- 直角方形、粗體、高對比文字。

```css
.badge-swiss-active {
    background-color: var(--swiss-black);
    color: var(--swiss-white);
    border-radius: 0;
    font-weight: 600;
    font-size: 0.75rem;
    padding: 0.35rem 0.65rem;
}
.badge-swiss-inactive {
    background-color: var(--swiss-light-grey);
    color: var(--swiss-dark);
    border-radius: 0;
    font-weight: 600;
    font-size: 0.75rem;
    padding: 0.35rem 0.65rem;
}
```

---

### 4.5 表單控制元件 (Form Inputs)
- 直角邊框、細黑灰線。
- 聚焦時（Focus）呈現乾淨俐落的純黑外框，不使用發光效果。

```css
.form-control-swiss {
    border: 1px solid var(--swiss-dark);
    border-radius: var(--swiss-radius);
    padding: 0.625rem 0.875rem;
    font-size: 0.9375rem;
    color: var(--swiss-black);
}
.form-control-swiss:focus {
    border-color: var(--swiss-red);
    box-shadow: 0 0 0 1px var(--swiss-red);
    outline: none;
}
```

---

## 5. 完整 CSS 樣式庫範本 (`/css/swiss-theme.css`)

```css
/**
 * 瑞士國際主義風格核心樣式庫 (Swiss Style Design System)
 */
:root {
    /* 核心色彩 */
    --swiss-red: #dc2626;
    --swiss-red-hover: #b91c1c;
    --swiss-black: #111111;
    --swiss-dark: #262626;
    --swiss-grey: #737373;
    --swiss-light-grey: #e5e5e5;
    --swiss-bg: #f8f9fa;
    --swiss-white: #ffffff;

    /* 字體與字重 */
    --swiss-font-family: -apple-system, BlinkMacSystemFont, "Inter", "Helvetica Neue", Arial, "Noto Sans TC", sans-serif;
    --swiss-font-mono: "SFMono-Regular", Menlo, Monaco, Consolas, "Liberation Mono", monospace;

    /* 邊框與直角規範 */
    --swiss-border-width: 1px;
    --swiss-border-bold: 2px;
    --swiss-border-color: var(--swiss-light-grey);
    --swiss-radius: 0px;
}

/* 全域基礎排版 */
body.swiss-body {
    background-color: var(--swiss-bg);
    color: var(--swiss-dark);
    font-family: var(--swiss-font-family);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
}

[v-cloak] {
    display: none !important;
}

/* 標題與標籤 */
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

/* SweetAlert2 瑞士風格自定義彈窗 */
.swiss-swal-popup {
    border-radius: 0 !important;
    border: 2px solid var(--swiss-black) !important;
    box-shadow: none !important;
    font-family: var(--swiss-font-family) !important;
}

.swiss-swal-title {
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    font-size: 1.25rem;
    color: var(--swiss-black);
}

.swiss-swal-toast {
    border-radius: 0 !important;
    border: 1px solid var(--swiss-black) !important;
    background-color: var(--swiss-black) !important;
    color: var(--swiss-white) !important;
}
```
