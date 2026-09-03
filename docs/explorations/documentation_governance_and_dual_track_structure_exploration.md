# 日常流水帳系統 (Daily Ledger System) — 文件治理、分類矩陣與雙軌目錄架構探索報告

> **專案代號**：`daily-ledger-system`  
> **文件版本**：v1.0.0  
> **建立日期**：2026-09-03  
> **模式定位**：探索報告 (Exploration Report / `opsx-explore`)  
> **狀態**：探索完成與決策收斂 (`Exploration Completed & Decisions Aligned`)  
> **技術棧與生態**：Markdown / GitHub Flavored Markdown / Mermaid / OpenSpec  
> **導覽指引**：[← 返回專案文件總覽門戶 (docs/README.md)](../README.md)  

---

## 1. 探索背景與問題陳述 (Background & Problem Statement)

隨著「日常流水帳系統」專案的敏捷演進、多階段架構重構（包含模組化密碼學、抗量子密碼學調研、單元測試體系建立、IDE 代碼潔淨排查等），專案的 `docs/` 資料夾累積了豐富的技術文件。

然而，當前 `docs/` 底層所有 Markdown 文件皆處於同一扁平層級（Flat Hierarchy），導致以下管理與查閱痛點：
1. **認知負載劇增**：新加入或日常維運的開發者難以一眼分辨哪些是「已定案、需嚴格遵守的系統規格/操作手冊 (SOP)」，哪些是「頭腦風暴、選型評估與探索性的 Spike 筆記」。
2. **單一真實來源 (SSOT) 模糊**：早期所撰寫的整合型大型規劃報告與後期細緻拆分的模組規格書（如 `docs/daily_ledger_system/` 下的 01~09 文件）並存於同一層級，容易造成版本參照的疑惑。
3. **缺乏頂層導航中樞**：進入 `docs/` 資料夾時缺乏統一的門戶指引（Portal / Index），無法快速按功能主題（認證、流水帳、測試、維運）一鍵跳轉至對應產物。

本探索報告旨在系統性定義專案的文件分類哲學、確立「雙軌制（探索軌 vs 規範維運軌）」治理模型，並規劃平滑搬遷至新結構的實施藍圖。

---

## 2. 文件分類哲學與分類矩陣 (Taxonomy & Governance Philosophy)

為徹底解決文件混雜問題，專案確立將所有非代碼產物依據**時效性**、**權威性**與**目標受眾**劃分為兩大核心軌道：

```
+---------------------------------------------------------------------------------------------------------------+
|                                     文件屬性本質對比 (Document Taxonomies)                                    |
+--------------------+--------------------------------------------------+---------------------------------------+
| 特性維度           | 探索文件軌 (Exploration Track)                   | 正式規範軌 (Specification Track)      |
+--------------------+--------------------------------------------------+---------------------------------------+
| 核心定位           | 思考推演、技術選型對比、問題排查 (Spikes/RFCs)    | 權威事實 (Single Source of Truth)、SOP|
| 目錄歸屬           | docs/explorations/                               | docs/specifications/                  |
| 生命週期           | 階段性、記錄決策當下的脈絡與演進歷史             | 持續性、隨代碼庫演進嚴格同步與維護    |
| 內容風格           | 提出方案 A/B、權衡優缺點、探索性架構圖           | 定義驗收條件、架構規範、操作步驟契約  |
| 典型來源           | /opsx-explore、技術調研、IDE 診斷排查            | OpenSpec 落地交付物、系統手冊、規格書 |
| 維護要求           | 產出後通常凍結作為決策背景（非破壞性不修改）      | 代碼或業務邏輯變更時必須連帶同步更新  |
+--------------------+--------------------------------------------------+---------------------------------------+
```

---

## 3. 現存文檔盤點與歸屬映射 (Current Inventory & Taxonomy Mapping)

針對目前專案 `docs/` 資料夾下現存之檔案進行逐一屬性認定與映射歸類：

```
+---------------------------------------------------------------------------------------------------------------+
|                                            現存文件分類與歸屬對照表                                            |
+-------------------------------------------------------------------+--------------------+----------------------+
| 檔案名稱                                                          | 屬性歸類           | 目標路徑             |
+-------------------------------------------------------------------+--------------------+----------------------+
| system_exploration_report.md                                      | 探索文件 (架構)    | docs/explorations/   |
| modular_crypto_and_pqc_design_exploration.md                      | 探索文件 (安全)    | docs/explorations/   |
| automated_testing_strategy_and_exploration.md                     | 探索文件 (測試)    | docs/explorations/   |
| code_cleanliness_and_ide_troubleshooting_exploration.md           | 探索文件 (代碼/IDE)| docs/explorations/   |
| daily_ledger_system_specification_and_planning_report.md          | 探索文件 (歷史聚合)| docs/explorations/   |
| documentation_governance_and_dual_track_structure_exploration.md | 探索文件 (文件治理)| docs/explorations/   |
+-------------------------------------------------------------------+--------------------+----------------------+
| daily_ledger_system/ (內含 01~09 完整拆分規格與 README)           | 正式規範 (系統核心)| docs/specifications/ |
| core_services_unit_testing_design.md                              | 正式規範 (測試設計)| docs/specifications/ |
| startup_script_specification_and_guide.md                         | 正式規範 (維運腳本)| docs/specifications/ |
+-------------------------------------------------------------------+--------------------+----------------------+
| [預計新建] README.md                                              | 頂層門戶導覽地圖   | docs/README.md       |
+-------------------------------------------------------------------+--------------------+----------------------+
```

### 個別檔案分析說明：
1. **`docs/daily_ledger_system/` (子目錄)**：
   - 內含 `01_executive_summary_and_proposal.md` 到 `09_unit_testing_guide_and_test_catalog.md` 以及模組 `README.md`。
   - 此系列文件經過嚴格審查與驗收，為系統核心能力（使用者認證、分類管理、流水帳、離線前端、維運腳本、操作手冊、單元測試）之權威規範，歸入 `docs/specifications/daily_ledger_system/`。
2. **`docs/daily_ledger_system_specification_and_planning_report.md`**：
   - 此為早期將 01～05 整合在同一檔案的全文大文件。依據架構決策，作為專案發展史之里程碑紀錄，歸入 `docs/explorations/`。
3. **`docs/core_services_unit_testing_design.md`**：
   - 記錄了針對 `CategoryService` 與 `LedgerService` 的核心業務隔離測試矩陣，具備正式設計規範性質，歸入 `docs/specifications/`。
4. **`docs/startup_script_specification_and_guide.md`**：
   - 定義 `start.ps1` 啟動腳本參數契約、智慧前置診斷與雙資料庫設定檔之正式技術規格，歸入 `docs/specifications/`。

---

## 4. 架構決策記錄 (Architectural Decision Records - ADR)

在本次探索中，針對核心議題凝聚並確認了以下關鍵決策：

### 決策 1：採用雙軌目錄制 (Dual-Track Hierarchy)
- **情境**：評估「按生命週期雙軌制（`explorations/` vs `specifications/`）」與「按功能領域模組化（`architecture/`、`testing/`、`operations/` 各自混合）」。
- **決策**：**採用雙軌制**。
- **理由**：
  - 開發者最迫切的需求是迅速區分「已生效的契約」與「發散的調研」。
  - 雙軌制結構扁平、語意直觀，避免多層巢狀目錄帶來的維護成本。

### 決策 2：早期整合版大文件歸入探索區
- **情境**：`daily_ledger_system_specification_and_planning_report.md` 包含早期 01～05 的聚合內容，與現行 `daily_ledger_system/` 有部分重疊。
- **決策**：**維持在探索區 (`docs/explorations/`) 封存備查**。
- **理由**：
  - 保留專案從草創提案到規格成熟的演進脈絡，不破壞歷史資料完整性。
  - 避免將其與最新權威規範混淆，確保正式規範區由 `daily_ledger_system/` 單一代表。

### 決策 3：建立頂層導航地圖 (`docs/README.md`)
- **情境**：重組後的目錄需要入口指引，否則使用者進入 `docs/` 面對兩個子目錄仍需逐一探索。
- **決策**：**在 `docs/` 根目錄建立頂層門戶 `README.md`**。
- **理由**：
  - 提供專案級全域索引，支援跨探索軌與規範軌的「按主題（認證、流水帳、測試、安全、維運）」矩陣導覽。

---

## 5. 目標目錄拓撲與重組藍圖 (Target Directory Topology)

實施重組後的完整目錄樹狀結構規劃如下：

```
docs/
│
├── README.md                                               <-- [新建] 頂層導航中樞、檢索地圖與治理標準
│
├── explorations/                                           <-- [探索文件區] 思考、技術選型、問題排查與歷史聚合
│   ├── system_exploration_report.md                        (系統架構與瑞士風格視覺選型探索)
│   ├── modular_crypto_and_pqc_design_exploration.md       (密碼管理與抗量子密碼學架構探索)
│   ├── automated_testing_strategy_and_exploration.md       (自動化測試金字塔選型與規劃探索)
│   ├── code_cleanliness_and_ide_troubleshooting_exploration.md (IDE 診斷與代碼潔淨方案探索)
│   ├── daily_ledger_system_specification_and_planning_report.md (早期全方位實施整合版探索報告)
│   └── documentation_governance_and_dual_track_structure_exploration.md (本文件：文件治理與架構探索)
│
└── specifications/                                         <-- [正式規範區] 權威設計契約、維運指南與手冊
    ├── daily_ledger_system/                                (核心流水帳業務系統 01~09 拆分規格庫)
    │   ├── 01_executive_summary_and_proposal.md            (專案提案與執行摘要)
    │   ├── 02_functional_specifications.md                 (功能規格契約與時序圖)
    │   ├── 03_system_architecture_and_design.md            (後端四層架構與類別設計)
    │   ├── 04_database_design_and_ddl.md                   (MS SQL 資料庫 ERD 與 DDL)
    │   ├── 05_tasks_and_implementation_plan.md             (任務分解甘特圖與驗收清單)
    │   ├── 06_quality_assurance_and_dod.md                 (交付定義檢核矩陣 DoD)
    │   ├── 07_startup_script_and_devops_guide.md           (啟動腳本與維運指南)
    │   ├── 08_system_operation_and_user_manual.md          (終端使用者操作手冊 SOP)
    │   ├── 09_unit_testing_guide_and_test_catalog.md       (單元測試操作手冊與 54 案例清單)
    │   └── README.md                                       (系統主規格導航目錄)
    │
    ├── core_services_unit_testing_design.md                (核心業務服務單元測試設計規格)
    └── startup_script_specification_and_guide.md           (啟動腳本 start.ps1 規格與維運手冊)
```

---

## 6. 頂層導覽地圖 (`docs/README.md`) 設計規範

頂層 `docs/README.md` 將依據瑞士極簡設計精神與清晰的資訊架構（Information Architecture）組織，主要包含四大模組：

1. **歡迎與導覽宣言 (Header & Philosophy)**：
   - 簡述專案目標、核心技術棧與文檔導覽原則。
2. **雙軌分類治理說明 (Governance Model)**：
   - 快速指引開發者何時查看 `explorations/`，何時遵循 `specifications/`。
3. **跨維度主題檢索矩陣 (Topic Matrix Table)**：
   - 橫跨業務領域建立直接跳轉連結，例如：
     - **身分認證與安全**：探索報告 ➔ `modular_crypto...`；正式規格 ➔ `02_functional...`、`03_system...`。
     - **記帳核心業務**：探索報告 ➔ `system_exploration...`；正式規格 ➔ `02_functional...`、`04_database...`、`08_system_operation...`。
     - **測試與品質體系**：探索報告 ➔ `automated_testing...`；正式規格 ➔ `09_unit_testing...`、`core_services_unit...`。
     - **腳本與環境維運**：探索報告 ➔ `code_cleanliness...`；正式規格 ➔ `startup_script...`、`07_startup_script...`。
4. **貢獻指南與命名規範 (Contribution Guide)**：
   - 新增文件時的命名準則與標準放置目錄。

---

## 7. 文件生命週期與流轉治理流程 (Lifecycle & Workflow)

專案未來的文件沉澱機制緊密結合 **OpenSpec 變更生命週期**，形成從「發散思考」到「規範固化」的標準工作流：

```
+-----------------------------------------------------------------------------------+
|                        文件生命週期流轉模型 (Document Lifecycle Flow)              |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   [ 新功能需求 / 架構調整 / 技術疑難 ]                                            |
|              |                                                                    |
|              v                                                                    |
|     /opsx-explore (探索階段)                                                      |
|              |                                                                    |
|              +---> 產出: docs/explorations/*_exploration.md                       |
|                          (完整記錄背景思考、候選架構對比、未定風險)               |
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
|        - 系統功能與資料庫規格書 (02_*, 04_*)                                       |
|        - 操作與維運指南 (07_*, 08_*, startup_script_*)                            |
|        - 單元測試指引與清單 (09_*, core_services_*)                               |
|                   |                                                               |
|                   v                                                               |
|   同步更新: docs/README.md (主題索引與鏈結維護)                                   |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 8. 結論與後續實施建議 (Conclusion & Next Steps)

本探索報告已完整梳理 `docs/` 的文件分類模型，並獲得採用「雙軌制」的架構共識。

### 建議後續實施步驟：
1. **建立正式變更提案**：透過 `/opsx-propose` 建立變更（例如：`reorganize-docs-dual-track`）。
2. **目錄建立與檔案搬遷**：
   - 建立 `docs/explorations/` 與 `docs/specifications/`。
   - 依據對照表移動對應檔案。
3. **建立頂層 `docs/README.md`**：完成門戶導航與主題矩陣索引。
4. **驗證相對鏈結**：確保所有 Markdown 交叉參照與 IDE 預覽無死鏈結。
