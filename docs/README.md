# 日常流水帳系統 (Daily Ledger System) — 文件總覽與治理門戶

> **系統名稱**：日常流水帳系統 (Daily Ledger System)  
> **文件架構**：雙軌目錄體系 (Dual-Track Documentation Architecture)  
> **治理標準**：OpenSpec 驅動與瑞士風格 (Swiss Style) 資訊架構  
> **維護狀態**：正式啟用 (Active & Governed)  

---

## 1. 歡迎與導覽宣言 (Welcome & Philosophy)

本目錄為「日常流水帳系統」專案的唯一技術文件門戶中心（Documentation Portal）。本專案恪守瑞士風格極簡與功能主義設計原則，將系統的所有技術知識、架構決策、系統規格與操作手冊依據**時效性**、**權威性**與**生命週期**劃分為清晰的「雙軌目錄架構」，以提供單一真實來源（Single Source of Truth, SSOT），並大幅降低工程師與維運人員的查閱與維護成本。

---

## 2. 雙軌分類治理模型 (Dual-Track Governance Model)

專案文件依本質嚴格分流為兩大獨立軌道：

```
+---------------------------------------------------------------------------------------------------------------+
|                                      文件屬性與治理維度對比 (Document Taxonomies)                              |
+--------------------+--------------------------------------------------+---------------------------------------+
| 特性維度           | 探索文件軌 (Exploration Track)                   | 正式規範軌 (Specification Track)      |
+--------------------+--------------------------------------------------+---------------------------------------+
| 核心定位           | 思考推演、技術選型對比、問題排查 (Spikes / RFCs)  | 權威事實 (Single Source of Truth)、SOP|
| 目錄歸屬           | docs/explorations/                               | docs/specifications/                  |
| 生命週期           | 階段性、記錄決策當下的脈絡與演進歷史             | 持續性、隨代碼庫演進嚴格同步與維護    |
| 內容風格           | 提出方案 A/B、權衡優缺點、探索性架構圖           | 定義驗收條件、架構規範、操作步驟契約  |
| 典型來源           | /opsx-explore、技術調研、IDE 診斷排查            | OpenSpec 落地交付物、系統手冊、規格書 |
| 維護要求           | 產出後通常凍結作為決策背景（非破壞性不修改）      | 代碼或業務邏輯變更時必須連帶同步更新  |
+--------------------+--------------------------------------------------+---------------------------------------+
```

---

## 3. 目錄拓撲樹狀圖 (Directory Topology)

```
docs/
│
├── README.md                                               <-- 本文件：頂層導航中樞、檢索地圖與治理標準
│
├── explorations/                                           <-- [探索文件軌] 思考演進、技術調研、歷史大文件
│   ├── system_exploration_report.md                        (系統架構與瑞士風格視覺選型探索)
│   ├── modular_crypto_and_pqc_design_exploration.md       (密碼管理與抗量子密碼學架構探索)
│   ├── automated_testing_strategy_and_exploration.md       (自動化測試金字塔選型與規劃探索)
│   ├── code_cleanliness_and_ide_troubleshooting_exploration.md (IDE 診斷與代碼潔淨方案探索)
│   ├── daily_ledger_system_specification_and_planning_report.md (早期全方位實施整合版探索報告)
│   ├── documentation_governance_and_dual_track_structure_exploration.md (文件治理與架構探索報告)
│   └── e2e_testing_design_and_playwright_exploration.md    (端到端測試體系設計與 Playwright Java 選型探索)
│
└── specifications/                                         <-- [正式規範軌] 權威設計契約、維運指南與 SOP 手冊
    ├── daily_ledger_system/                                (核心流水帳業務系統 01~09 拆分規格庫)
    │   ├── README.md                                       (系統主規格導航目錄)
    │   ├── 01_executive_summary_and_proposal.md            (專案提案與執行摘要)
    │   ├── 02_functional_specifications.md                 (功能規格契約與時序圖)
    │   ├── 03_system_architecture_and_design.md            (後端四層架構與類別設計)
    │   ├── 04_database_design_and_ddl.md                   (MS SQL 資料庫 ERD 與 DDL)
    │   ├── 05_tasks_and_implementation_plan.md             (任務分解甘特圖與驗收清單)
    │   ├── 06_quality_assurance_and_dod.md                 (交付定義檢核矩陣 DoD)
    │   ├── 07_startup_script_and_devops_guide.md           (啟動腳本與維運指南)
    │   ├── 08_system_operation_and_user_manual.md          (終端使用者操作手冊 SOP)
    │   └── 09_unit_testing_guide_and_test_catalog.md       (單元測試操作手冊與 54 案例清單)
    │
    ├── core_services_unit_testing_design.md                (核心業務服務單元測試設計規格)
    ├── engineering_standards_and_code_cleanliness.md       (專案通用工程標準、代碼潔淨與 IDE 排除指南)
    └── startup_script_specification_and_guide.md           (啟動腳本 start.ps1 規格與維運手冊)
```

---

## 4. 跨領域主題檢索矩陣 (Cross-Domain Topic Matrix)

為方便跨軌道快速查閱，以下按核心領域提供直接導覽對照：

| 業務/技術領域 | 探索軌研究報告 (Explorations) | 正式規範與指南 (Specifications) | 核心重點摘要 |
| :--- | :--- | :--- | :--- |
| **身分認證與密碼安全** | [modular_crypto_and_pqc_design_exploration.md](explorations/modular_crypto_and_pqc_design_exploration.md) | [02_functional_specifications.md](specifications/daily_ledger_system/02_functional_specifications.md)<br>[03_system_architecture_and_design.md](specifications/daily_ledger_system/03_system_architecture_and_design.md) | BCrypt / Argon2id / ML-KEM 抗量子加密、JWT/Session 認證時序 |
| **記帳與分類核心業務** | [system_exploration_report.md](explorations/system_exploration_report.md)<br>[daily_ledger_system_specification_and_planning_report.md](explorations/daily_ledger_system_specification_and_planning_report.md) | [02_functional_specifications.md](specifications/daily_ledger_system/02_functional_specifications.md)<br>[04_database_design_and_ddl.md](specifications/daily_ledger_system/04_database_design_and_ddl.md)<br>[08_system_operation_and_user_manual.md](specifications/daily_ledger_system/08_system_operation_and_user_manual.md) | 分類雙層樹狀管理、收支流水帳 CRUD、統計圖表、MS SQL 實體設計 |
| **測試體系與品質保證** | [automated_testing_strategy_and_exploration.md](explorations/automated_testing_strategy_and_exploration.md)<br>[e2e_testing_design_and_playwright_exploration.md](explorations/e2e_testing_design_and_playwright_exploration.md) | [core_services_unit_testing_design.md](specifications/core_services_unit_testing_design.md)<br>[09_unit_testing_guide_and_test_catalog.md](specifications/daily_ledger_system/09_unit_testing_guide_and_test_catalog.md)<br>[06_quality_assurance_and_dod.md](specifications/daily_ledger_system/06_quality_assurance_and_dod.md) | 單元測試金字塔、Playwright Java 真機 E2E、H2 隔離架構、DoD 檢核 |
| **工程標準與代碼潔淨** | [code_cleanliness_and_ide_troubleshooting_exploration.md](explorations/code_cleanliness_and_ide_troubleshooting_exploration.md) | [engineering_standards_and_code_cleanliness.md](specifications/engineering_standards_and_code_cleanliness.md) | IDE 存檔自動化、正則常數快取、JpaRepository 標註、Zero-Warning 交付矩陣 |
| **腳本工具與環境維運** | - | [startup_script_specification_and_guide.md](specifications/startup_script_specification_and_guide.md)<br>[07_startup_script_and_devops_guide.md](specifications/daily_ledger_system/07_startup_script_and_devops_guide.md) | `start.ps1` 參數契約、智慧環境前置偵測、雙資料庫切換、非同步瀏覽器啟動 |
| **文件治理與架構體系** | [documentation_governance_and_dual_track_structure_exploration.md](explorations/documentation_governance_and_dual_track_structure_exploration.md) | [README.md](README.md) (本門戶)<br>[specifications/daily_ledger_system/README.md](specifications/daily_ledger_system/README.md) | 雙軌架構、SSOT 單一真實來源、OpenSpec 變更生命週期銜接 |

---

## 5. 文件生命週期與流轉治理流程 (Lifecycle & Workflow)

所有非代碼文件的產出均與專案之敏捷工作流及 OpenSpec 緊密結合：

```
+-----------------------------------------------------------------------------------+
|                        文件生命週期流轉模型 (Document Lifecycle Flow)              |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   [ 新需求 / 架構調整 / 技術疑難 ]                                                |
|              |                                                                    |
|              v                                                                    |
|     /opsx-explore (探索階段)                                                      |
|              |                                                                    |
|              +---> 產出: docs/explorations/*_exploration.md                       |
|                          (完整記錄背景思考、候選架構對比、權衡與未定風險)         |
|                                      |                                            |
|                                      v                                            |
|                        是否確定落地為正式功能/架構?                                |
|                                      |                                            |
|                   +------------------+------------------+                         |
|                   | 是                                  | 否 (存檔備查)           |
|                   v                                     v                         |
|       /opsx-propose & /opsx-apply           [ 永久保留在 explorations 作為決策歷史 ]
|       (OpenSpec Change 開發與測試)                                                |
|                   |                                                               |
|                   v                                                               |
|       實作完成 & 歸檔同步 (/opsx-archive)                                         |
|                   |                                                               |
|                   v                                                               |
|   產出/更新至: docs/specifications/                                               |
|        - 系統功能與資料庫規格書                                                   |
|        - 操作手冊與維運指南 (SOP)                                                  |
|        - 單元測試指引與測試清單                                                   |
|                   |                                                               |
|                   v                                                               |
|   同步更新: docs/README.md (主題索引與鏈結維護)                                   |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 6. 貢獻指南與命名規範 (Contribution Guidelines)

為維護專案文件庫的整潔與權威性，任何文檔的新增與修改請嚴格遵守以下準則：

1. **嚴禁散落於根目錄**：
   - `docs/` 根目錄**僅允許存在 `README.md`**，其餘所有文件必須明確放置於 `explorations/` 或 `specifications/`。
2. **命名規範 (Naming Conventions)**：
   - **探索文件**：必須以 `_exploration.md` 或 `_report.md` 結尾，如 `xxx_design_exploration.md`。
   - **正式規範文件**：必須以 `_specification.md`、`_design.md` 或 `_guide.md` 結尾；若屬於子系統完整規格，則放置於該系統專屬子目錄中並以編號序列（如 `01_xxx.md`）組織。
3. **相對鏈結維護**：
   - 文件內如有引用其他文件，必須使用正確的相對路徑（例如跨軌參照使用 `../specifications/...` 或 `../explorations/...`）。
4. **即時同步門戶**：
   - 凡新增規格或重要探索報告，必須在第一時間同步更新本 `docs/README.md` 之主題檢索矩陣。
